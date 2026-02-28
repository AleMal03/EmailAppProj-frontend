package mailproject.mailclient.model;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.*;
import mailproject.mailclient.model.adapters.BooleanPropertyAdapter;
import mailproject.mailclient.model.adapters.LocalDateTimeAdapter;
import mailproject.mailclient.model.beans.Email;
import mailproject.mailclient.model.servercommunication.ServerRequest;
import mailproject.mailclient.model.servercommunication.ServerResponse;

import java.io.*;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class DataModel {
	private final String SERVER_IP = "127.0.0.1";
	private final int SERVER_PORT = 5555;
	private final SimpleStringProperty currentUser;     // Indirizzo email dell'utente loggato
	private final SimpleListProperty<Email> inbox;      // Inbox: lista email in arrivo
	private final SimpleObjectProperty<Email> selectedEmail;    // Email attualmente selezionata
	private final ExecutorService requestExec;          // Esecutore per richieste al server come verifica dell'email o sync inbox
	private final ScheduledExecutorService connectionExec;       // Esecutore per verifica connessione periodica
	private final SimpleStringProperty genericError;    // Contiene il messaggio di errore da visualizzare nella GUI
	private ScheduledFuture<?> pollingTask;       // Polling task per la checkConnection periodica
	private final BooleanProperty isConnectionOnline;

	public DataModel() {
		currentUser = new SimpleStringProperty(null);
		inbox = new SimpleListProperty<>(javafx.collections.FXCollections.observableArrayList());
		selectedEmail = new SimpleObjectProperty<>(null);
		genericError = new SimpleStringProperty("");
		isConnectionOnline = new SimpleBooleanProperty(false);

		// Init executors
		requestExec = Executors.newFixedThreadPool(2, r->{// Un thread per le operazioni in background e uno per le richieste al server
			Thread t = new Thread(r);
			t.setDaemon(true);  // Setto i thread della pool come daemons
			return t;
		});
		connectionExec = Executors.newScheduledThreadPool(1, r->{
			Thread t = new Thread(r);
			t.setDaemon(true);  // Setto i thread della pool come daemons
			return t;
		});


		// Listener current user per sincronizzazione inbox al login e per la verifica periodica della connessione
		currentUser.addListener((_, _, newValue) -> {
			if(newValue != null) {
				// Tento la sincronizzazione ogni 5 secondi
				pollingTask = connectionExec.scheduleAtFixedRate(
						() -> syncInbox(newValue, (errorMsg) -> genericError.set(errorMsg)),
						0,
						5,
						TimeUnit.SECONDS);
			}
			else{
				if(pollingTask != null)
					pollingTask.cancel(true);   // Cancello il task per fermarlo
			}
		});
	}

	/**
	 * Esegue il login nella mailbox.
	 *
	 * @param emailAddress Indirizzo email dell'utente che effettua il login.
	 * @param onSuccess Callback da chiamare in caso di successo (risposta dal server pervenuta).
	 * @param onError Callback da chiamare in caso di errore nei parametri o nella connessione al server.
	 */
	public void createSession(String emailAddress, Consumer<Boolean> onSuccess, Consumer<String> onError){
		if(currentUser.get() != null){
			onError.accept("Utente già loggato");
			return;
		}

		final String email = emailAddress != null ? emailAddress.strip() : "";

		if(!isEmailValid(email)){
			onError.accept("Indirizzo email inserito non valido");
			return;
		}

		// Lavoro di Socket gestito dalla ThreadPool
		requestExec.execute(() -> {
			try{
				if(!emailExists(email)){
					Platform.runLater(() -> onError.accept("L'indirizzo email inserito non esiste"));
				}
				else{
					// Dopo che il Thread ha finito, rimando l'aggiornamento della GUI al MainThread
					Platform.runLater(() -> {
							currentUser.set(email.toLowerCase());
							onSuccess.accept(true);
					});
				}
			}
			catch(Exception e){
				Platform.runLater(() -> onError.accept("Comunicazione col server fallita"));
				e.printStackTrace();
			}
		});
	}

	/**
	 * Esegue il logout dalla mailbox.
	 */
	public void invalidateSession(){
		currentUser.set(null);
	}

	/**
	 * Verifica la correttezza sintattica dell'indirizzo email.
	 *
	 * @param email Indirizzo email da verificare.
	 * @return true se l'indirizzo email è valido, false altrimenti.
	 */
	private boolean isEmailValid(String email){
		String emailRegex = "^[\\w.-]+@[\\w-]+\\.[\\w-]{2,4}$";     // Regex per verifica sintattica dell'email

		if(email==null || email.isEmpty() || !email.matches(emailRegex))
			return false;

		return true;
	}

	/**
	 * Verifica esistenza dell'indirizzo email comunicando col server.
	 *
	 * @param email Indirizzo email da cercare.
	 * @return true se l'email esiste nel server, false altrimenti.
	 */
	private boolean emailExists(String email){
		ServerRequest req = new ServerRequest(currentUser.get(), "VER_EML", email);
		ServerResponse res = null;

		// Serializzo la richiesta in JSON
		Gson gson = new Gson();
		String reqJson = gson.toJson(req);

		try(Socket socket = new Socket(SERVER_IP, SERVER_PORT);
		    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		    Scanner in = new Scanner(socket.getInputStream());){

			out.println(reqJson);
			res = gson.fromJson(in.nextLine(),  ServerResponse.class);
		} catch (ConnectException e) {
			throw new RuntimeException("Server offline: connessione rifiutata.");
		}
		catch (Exception e){
			throw new RuntimeException("Errore nella ricezione del messaggio Json " + e);
		}

		if(res != null){
			if(res.isSuccess()){
				return (Boolean) res.getData();
			}
			else{
				throw new RuntimeException("Elaborazione del server fallita");
			}
		}

		throw new RuntimeException("Comunicazione col server fallita.");
	}

	private void syncInbox(String emailAddr, Consumer<String> onError){
		String user = emailAddr.split("@")[0];  // Estraggo user dall'indirizzo email
		Gson gson = new GsonBuilder()
				.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
				.registerTypeHierarchyAdapter(BooleanProperty.class, new BooleanPropertyAdapter())
				.create();

		File localFile = new File("data/" + user + ".json");
		Type tipoLista = new TypeToken<List<Email>>() {}.getType();  // Definisce e "cattura" il tipo esatto della Lista per non perdere informazioni

		// Richiesta al server asincrona con thread
		requestExec.execute(() -> {
			// 1. CARICAMENTO CACHE LOCALE
			final List<Email> localEmails = new ArrayList<>();
			if (localFile.exists()) {
				try (FileReader reader = new FileReader(localFile)) {
					List<Email> cacheEmails = gson.fromJson(reader, tipoLista);
					if (cacheEmails != null) localEmails.addAll(cacheEmails);
				}
				catch (Exception e){
					Platform.runLater(() -> onError.accept("Errore lettura cache locale inbox: " + e.getMessage()));
					e.printStackTrace();

				}
			}

			// 2. RICHIESTA NUOVE EMAILS AL SERVER
			final List<Email> newEmails =  new ArrayList<>();
			try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
			     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			     Scanner in = new Scanner(new InputStreamReader(socket.getInputStream()));) {

				// Invio richiesta
				int lastId = 0;
				for(Email email : localEmails){
					if(email.getId() > lastId){
						lastId =  email.getId();
					}
				}
				ServerRequest req = new ServerRequest(emailAddr, "SYN_INBX", lastId);
				out.println(gson.toJson(req, ServerRequest.class));

				// Ricezione risposta
				ServerResponse res = gson.fromJson(in.nextLine(), ServerResponse.class);


				// Aggiungo le email arrivate alle nuove email
				if(res!=null && res.isSuccess()){
					Platform.runLater(() -> {
						genericError.set("");
						isConnectionOnline.set(true);
					});  // Reset error msg

					System.out.println(res.toString());   // Debug
					newEmails.addAll(gson.fromJson(gson.toJson(res.getData()), tipoLista));   // Parsing doppio per non perdere dati
					for (Email email : newEmails) {
						email.setLetta(false);
					}
				}
			}
			catch(ConnectException e){
				Platform.runLater(() -> {
					onError.accept("Connessione col server persa. Tentativo di riconessione... ");
					isConnectionOnline.set(false);
				});
				e.printStackTrace();

				return;
			}
			catch (Exception e) {
				Platform.runLater(() -> onError.accept("Errore sincronizzazione inbox da server: " + e.getMessage()));
				e.printStackTrace();

				return;
			}

			// 3. MERGE E SORT DELLA INBOX (solo se ci sono nuove email, o è il primo sync al login)
			if (!newEmails.isEmpty() || (inbox.get().isEmpty() && !localEmails.isEmpty())) {
				Platform.runLater(() -> {
					// Resetto inbox
					inbox.get().clear();

					// Aggiungo tutte le email raccolte
					inbox.get().addAll(localEmails);
					inbox.get().addAll(newEmails);

					// Eseguo ordinamento
					inbox.get().sort((e1, e2) -> e2.getDataSpedizione().compareTo(e1.getDataSpedizione()));
				});
			}

			// 4. SALVATAGGIO NUOVA INBOX IN CACHE LOCALE (SE CI SONO STATE AGGIUNTE)
			if(!newEmails.isEmpty()){
				Gson gsonPretty = new GsonBuilder()
						.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
						.registerTypeHierarchyAdapter(BooleanProperty.class, new BooleanPropertyAdapter())
						.setPrettyPrinting()
						.create();
				try (FileWriter writer = new FileWriter(localFile);) {
					List<Email> emailsToWrite = new ArrayList<>(localEmails);
					emailsToWrite.addAll(newEmails);
					emailsToWrite.sort((e1, e2) -> e2.getDataSpedizione().compareTo(e1.getDataSpedizione()));
					gsonPretty.toJson(emailsToWrite, writer);
				} catch (Exception e) {
					Platform.runLater(() -> onError.accept("Errore scrittura inbox in cache locale: " + e.getMessage()));
					e.printStackTrace();
				}
			}

			System.out.println(inboxProperty().get());   // Debug

		});
	}

	/*
	* Metodi di properties e campi privati
	*/
	public String getCurrentUser(){return currentUser.get();}

	public SimpleStringProperty currentUserProperty(){return currentUser;}

	public SimpleListProperty<Email> inboxProperty(){return inbox;}

	public void setSelectedEmail(Email selectedEmail){
		this.selectedEmail.set(selectedEmail);
	}

	public Email getSelectedEmail(){return selectedEmail.get();}

	public SimpleObjectProperty<Email> selectedEmailProperty(){return selectedEmail;}

	public String getGenericError() {
		return genericError.get();
	}

	public SimpleStringProperty genericErrorProperty() {
		return genericError;
	}

	public boolean isIsConnectionOnline() {
		return isConnectionOnline.get();
	}

	public BooleanProperty isConnectionOnlineProperty() {
		return isConnectionOnline;
	}
}
