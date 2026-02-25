package mailproject.mailclient.model;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import mailproject.mailclient.model.adapters.BooleanPropertyAdapter;
import mailproject.mailclient.model.adapters.LocalDateTimeAdapter;
import mailproject.mailclient.model.beans.Email;
import mailproject.mailclient.model.servercommunication.ServerRequest;
import mailproject.mailclient.model.servercommunication.ServerResponse;

import java.io.*;
import java.lang.reflect.Type;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class DataModel {
	private final SimpleStringProperty currentUser;     // Indirizzo email dell'utente loggato
	private final SimpleListProperty<Email> inbox;      // Inbox: lista email in arrivo
	private final SimpleObjectProperty<Email> selectedEmail;    // Email attualmente selezionata
	private final ExecutorService exec;    // Esecutore per richieste al server come verifica dell'email o sync inbox
	private final SimpleStringProperty genericError;    // Contiene il messaggio di errore da visualizzare nella GUI

	public DataModel() {
		currentUser = new SimpleStringProperty(null);
		inbox = new SimpleListProperty<>(javafx.collections.FXCollections.observableArrayList());
		selectedEmail = new SimpleObjectProperty<>(null);
		exec = Executors.newFixedThreadPool(2, r->{
			Thread t = new Thread(r);
			t.setDaemon(true);  // Setto i thread della pool come daemons
			return t;
		}); // Un thread per le operazioni in background e uno per le richieste al server
		genericError = new SimpleStringProperty("");

		// Listener current user per sincronizzazione inbox al login
		currentUser.addListener((_, _, newValue) -> {
			if(newValue != null)
				syncInbox(newValue, (errorMsg)-> genericError.set(errorMsg));
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
		exec.execute(() -> {
			try{
				if(!emailExists(email)){
					Platform.runLater(() -> onError.accept("L'indirizzo email inserito non esiste"));
				}
				else{
					// Dopo che il Thread ha finito, rimando l'aggiornamento della GUI al MainThread
					Platform.runLater(() -> {
							currentUser.set(email);
							onSuccess.accept(true);
					});
				}
			}
			catch(Exception e){
				Platform.runLater(() -> onError.accept("Comunicazione col server fallita"));
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
		final String serverIp = "127.0.0.1";
		final int serverPort = 5000;
		ServerRequest req = new ServerRequest(currentUser.get(), "VER_EML", email);
		ServerResponse res = null;

		// Serializzo la richiesta in JSON
		Gson gson = new Gson();
		String reqJson = gson.toJson(req);

		try(Socket socket = new Socket(serverIp, serverPort);
		    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		    Scanner in = new Scanner(socket.getInputStream());){

			out.println(reqJson);
			res = gson.fromJson(in.nextLine(),  ServerResponse.class);
		}
		catch (IOException | NoSuchElementException e){
			System.err.println(e.getMessage());
		}
		catch (JsonSyntaxException e){
			throw new RuntimeException("Errore nella ricezione del messaggio Json " + e);
		}

		if(res != null){
			return res.isSuccess();
		}

		throw new RuntimeException("Comunicazione col server fallita");
	}

	private void syncInbox(String emailAddr, Consumer<String> onError){
		String user = emailAddr.split("@")[0];  // Estraggo user dall'indirizzo email
		Gson gson = new GsonBuilder()
				.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
				.registerTypeAdapter(BooleanProperty.class, new BooleanPropertyAdapter())
				.create();


		File localFile = new File("data/" + user + ".json");
		Type tipoLista = new TypeToken<List<Email>>() {}.getType();  // Definisce e "cattura" il tipo esatto della Lista per non perdere informazioni

		// Richiesta al server asincrona con thread
		exec.execute(() -> {
			// 1. CARICAMENTO CACHE LOCALE
			final List<Email> localEmails = new ArrayList<>();
			if (localFile.exists()) {
				try (FileReader reader = new FileReader(localFile)) {
					List<Email> cacheEmails = gson.fromJson(reader, tipoLista);
					if (cacheEmails != null) localEmails.addAll(cacheEmails);
				}
				catch (Exception e){
					Platform.runLater(() -> onError.accept("Errore lettura cache locale inbox: " + e.getMessage()));
				}
			}

			// 2. RICHIESTA NUOVE EMAILS AL SERVER
			final List<Email> newEmails =  new ArrayList<>();
			try (Socket socket = new Socket("127.0.0.1", 5000);
			     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			     Scanner in = new Scanner(new InputStreamReader(socket.getInputStream()));) {

				// Invio richiesta
				ServerRequest req = new ServerRequest(emailAddr, "SYN_INBX", null);
				out.println(gson.toJson(req, ServerRequest.class));

				// Ricezione risposta
				ServerResponse res = gson.fromJson(in.nextLine(), ServerResponse.class);

				// Aggiungo le email arrivate alle nuove email
				if(res!=null && res.isSuccess()){
					newEmails.addAll(gson.fromJson(gson.toJson(res.getData()), tipoLista));   // Parsing doppio per non perdere dati
					for (Email email : newEmails) {
						email.setLetta(false);
					}
				}
			} catch (Exception e) {
				Platform.runLater(() -> onError.accept("Errore sincronizzazione inbox da server: " + e.getMessage()));
			}

			// 3. MERGE E SORT DELLA INBOX
			if (!localEmails.isEmpty() || !newEmails.isEmpty()) {
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
				try (FileWriter writer = new FileWriter(localFile);) {
					List<Email> emailsToWrite = new ArrayList<>(inbox.get());
					gson.toJson(emailsToWrite, writer);
				} catch (Exception e) {
					Platform.runLater(() -> onError.accept("Errore scrittura inbox in cache locale: " + e.getMessage()));
				}
			}
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
}
