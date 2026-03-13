package mailproject.mailclient.model;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.*;
import mailproject.mailclient.model.adapters.BooleanPropertyAdapter;
import mailproject.mailclient.model.adapters.LocalDateTimeAdapter;
import mailproject.mailclient.model.beans.Email;
import mailproject.mailclient.model.beans.Notification;
import mailproject.mailclient.model.servercommunication.ServerRequest;
import mailproject.mailclient.model.servercommunication.ServerResponse;

import javax.lang.model.type.NullType;
import javafx.scene.media.AudioClip;
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
	private boolean justLogged; // Indica se l'utente si è appena loggato (flag per la visualizzazione delle notifiche)
	private final SimpleStringProperty currentUser;     // Indirizzo email dell'utente loggato
	private final SimpleListProperty<Email> inbox;      // Inbox: lista email in arrivo
	private final SimpleObjectProperty<Email> selectedEmail;    // Email attualmente selezionata
	private final ExecutorService requestExec;                  // Esecutore per richieste al server come verifica dell'email o sync inbox
	private final ScheduledExecutorService connectionExec;      // Esecutore per verifica connessione periodica
	private final SimpleObjectProperty<Notification> notificaUtente;    // Contiene il messaggio di errore da visualizzare nella GUI
	private ScheduledFuture<?> pollingTask;             // Polling task per la checkConnection periodica
	private final BooleanProperty isConnectionOnline;   // Stato connessione col server
	private final BooleanProperty isLoading;            // Stato di caricamento

	public DataModel() {
		currentUser = new SimpleStringProperty(null);
		inbox = new SimpleListProperty<>(javafx.collections.FXCollections.observableArrayList());
		selectedEmail = new SimpleObjectProperty<>(null);
		notificaUtente = new SimpleObjectProperty<>(null);
		isConnectionOnline = new SimpleBooleanProperty(false);
		isLoading = new SimpleBooleanProperty(false);
		justLogged = false;

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
						() -> syncInbox(newValue),
						0,
						5,
						TimeUnit.SECONDS);
			}
			else{
				if(pollingTask != null)
					pollingTask.cancel(true);   // Cancello il task per fermarlo
			}
		});


		// Listener status connessione per notifica personalizzata
		isConnectionOnline.addListener((_, oldValue, newValue) -> {
			if(oldValue == false && newValue == true){  // Se mi sono appena riconnesso al server, invio la notifica relativa
				if(!justLogged)
					notificaUtente.set(new Notification(Notification.NotificationType.INFO, "Connessione al server ristabilita."));
				else
					justLogged = false;
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
							justLogged = true;
							currentUser.set(email.toLowerCase());
							onSuccess.accept(true);
					});
				}
			}
			catch(Exception e){
				Platform.runLater(() -> onError.accept(e.getMessage()));
			}
		});
	}

	/**
	 * Esegue il logout dalla mailbox e il reset di tutte le variabili di stato ad essa associate.
	 */
	public void invalidateSession(){
		currentUser.set(null);
		isConnectionOnline.set(false);
		justLogged = false;
		inbox.get().clear();
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

	public List<String> areEmailValid(List<String> emails){
		List<String> invalidEmails = new ArrayList<>();

		for(String address : emails){
			if(!isEmailValid(address)){
				invalidEmails.add(address);
			}
		}

		return invalidEmails;
	}

	/**
	 * Verifica esistenza dell'indirizzo email comunicando col server.
	 *
	 * @param email Indirizzo email da cercare.
	 * @return true se l'email esiste nel server, false altrimenti.
	 */
	private boolean emailExists(String email){
		ServerRequest req = new ServerRequest(currentUser.get(), "EML_XST", email);
		ServerResponse res;

		// Serializzo la richiesta in JSON
		Gson gson = new Gson();
		String reqJson = gson.toJson(req);

		try(Socket socket = new Socket(SERVER_IP, SERVER_PORT);
		    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		    Scanner in = new Scanner(socket.getInputStream())){

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

	/**
	 * Sincronizza la inbox chiedendo i nuovi messaggi dal server.
	 *
	 * @param emailAddr Indirizzo email del currentUser.
	 */
	private void syncInbox(String emailAddr){
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
					Platform.runLater(() -> notificaUtente.set(new Notification(Notification.NotificationType.ERROR,
							"Errore lettura cache locale inbox: " + e.getMessage())));
				}
			}

			// 2. RICHIESTA NUOVE EMAILS AL SERVER
			final List<Email> newEmails =  new ArrayList<>();
			try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
			     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			     Scanner in = new Scanner(new InputStreamReader(socket.getInputStream()))) {

				// Invio richiesta
				long lastId = 0;
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
					Platform.runLater(() -> isConnectionOnline.set(true));

					newEmails.addAll(gson.fromJson(gson.toJson(res.getData()), tipoLista));   // Parsing doppio per non perdere dati
					for (Email email : newEmails) {
						email.setLetta(false);
					}
				}
			}
			catch(ConnectException e){
				Platform.runLater(() -> {
					notificaUtente.set(new Notification(Notification.NotificationType.ERROR,
							"Connessione col server persa. Tentativo di riconessione... "));
					isConnectionOnline.set(false);
				});

			}
			catch (Exception e) {
				Platform.runLater(() -> notificaUtente.set(new Notification(Notification.NotificationType.ERROR,
						"Errore sincronizzazione inbox da server: " + e.getMessage())));
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

					if(!newEmails.isEmpty()){
						notificaUtente.set(new Notification(Notification.NotificationType.INFO,
								"Hai dei nuovi messaggi in entrata!"));
						// Notifica audio
						try{
							String audioPath = getClass().getResource("/audio/incomingEmail.wav").toExternalForm();
							AudioClip notificaAudio = new AudioClip(audioPath);
							notificaAudio.play();
						}
						catch(Exception e){
							System.err.println("Impossibile riprodurre l'audio: " + e.getMessage());
						}
					}
				});
			}

			// 4. SALVATAGGIO NUOVA INBOX IN CACHE LOCALE (SE CI SONO STATE AGGIUNTE)
			if(!newEmails.isEmpty()){
				try{
					List<Email> emailsToWrite = new ArrayList<>(localEmails);
					emailsToWrite.addAll(newEmails);
					emailsToWrite.sort((e1, e2) -> e2.getDataSpedizione().compareTo(e1.getDataSpedizione()));
					updateInboxFile(localFile,  emailsToWrite);
				} catch (Exception e) {
					Platform.runLater(() -> notificaUtente.set(new Notification(Notification.NotificationType.ERROR,
							"Errore scrittura inbox in cache locale: " + e.getMessage())));
				}
			}
		});
	}

	/**
	 * Richiede la cancellazione di un'email dalla casella postale lato server per poi eliminarla anche localmente.
	 *
	 * @param toDelete Email da eliminare.
	 */
	public void deleteEmail(Email toDelete){
		long idToDelete = toDelete.getId();
		String user = currentUser.get().split("@")[0];  // Estraggo user dall'indirizzo email
		Gson gson = new Gson();
		ServerRequest req = new ServerRequest(currentUser.get(), "DLT_EML", idToDelete);

		isLoading.set(true);    // Inizio caricamento
		requestExec.execute(() -> {
			try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
			     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			     Scanner in = new Scanner(new InputStreamReader(socket.getInputStream()))) {

				out.println(gson.toJson(req, ServerRequest.class));     // Invio richiesta
				ServerResponse res = gson.fromJson(in.nextLine(), ServerResponse.class);

				if(res!=null && res.isSuccess()){
					boolean deleted = (Boolean) res.getData();
					if(deleted){    // Se lato server la cancellazione è avvenuta con successo, posso procedere lato client
						File localFile = new File("data/" + user + ".json");
						Platform.runLater(() -> {
							inbox.get().remove(toDelete);
							updateInboxFile(localFile, List.copyOf(inbox.get()));
						});
					}

					Platform.runLater(() -> notificaUtente.set(new Notification(Notification.NotificationType.SUCCESS, "Email eliminata.")));
				}
				else{
					Platform.runLater(() -> notificaUtente.set(new Notification(Notification.NotificationType.ERROR, "Errore di comunicazione col server: l'email non è stata cancellata.")));
				}
			} catch (Exception e) {
				Platform.runLater(() -> notificaUtente.set(new Notification(Notification.NotificationType.ERROR,"Errore cancellazione email: " + e.getMessage())));
			}
			finally {
				Platform.runLater(() -> isLoading.set(false));  // Fine caricamento
			}
		});
	}

	/**
	 * Sovrascrive la inbox sul file locale.
	 *
	 * @param localFile File locale della inbox.
	 * @param emailsToWrite Lista delle email da salvare su file.
	 * @throws RuntimeException Se la scrittura su file json non va a buon fine.
	 */
	private synchronized void updateInboxFile(File localFile, List<Email> emailsToWrite) throws RuntimeException{
		Gson gson = new GsonBuilder()
				.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
				.registerTypeHierarchyAdapter(BooleanProperty.class, new BooleanPropertyAdapter())
				.setPrettyPrinting()
				.create();

		try (FileWriter writer = new FileWriter(localFile)) {
			gson.toJson(emailsToWrite, writer);
		}
		catch(Exception e){
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * Marca l'email come letta e ne gestisce il salvataggio su file.
	 *
	 * @param email Email da marcare come letta.
	 */
	public void markEmailAsRead(Email email){
		// Procedo solo se l'email non era già stata letta (per evitare riscritture inutili)
		if (email != null && !email.isLetta()) {
			email.setLetta(true);

			String user = currentUser.get().split("@")[0];
			File localFile = new File("data/" + user + ".json");

			// Operazione di I/O asincrona
			requestExec.execute(() -> {
				try {
					updateInboxFile(localFile, List.copyOf(inbox.get()));
				} catch (Exception e) {
					Platform.runLater(() -> notificaUtente.set(new Notification(Notification.NotificationType.ERROR, "Errore salvataggio stato lettura in cache.")));
				}
			});
		}
	}

	/**
	 * Invia un'email al server perché venga distribuita ai destinatari.
	 *
	 * @param toSend Email da inviare.
	 * @param onDestInesistenti Funzione di callback in caso di destinatari inesistenti.
	 * @param onSuccess Funzione di callback in caso di operazione riuscita.
	 */
	public void sendEmail(Email toSend,  Consumer<List<String>> onDestInesistenti, Consumer<NullType> onSuccess){
		Gson gson = new GsonBuilder()
				.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())   // Adapter superflui perché i campi saranno null
				.registerTypeHierarchyAdapter(BooleanProperty.class, new BooleanPropertyAdapter())  // Adapter superflui perché i campi saranno null
				.create();

		// Mi assicuro che ci siano dei destinatari
		if(toSend.getDestinatari().isEmpty()){
			notificaUtente.set(new Notification(Notification.NotificationType.INFO, "Campo destinatari vuoto."));
			return;
		}

		isLoading.set(true);    // Inizio caricamento
		requestExec.execute(() -> {
			List<String> destinatariInesistenti = new ArrayList<>();

			// Prima verifico che i destinatari esistano
			try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
			     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			     Scanner in = new Scanner(new InputStreamReader(socket.getInputStream()))) {

				ServerRequest req = new ServerRequest(currentUser.get(), "VER_EML", toSend.getDestinatari());
				out.println(gson.toJson(req, ServerRequest.class));
				ServerResponse res = gson.fromJson(in.nextLine(), ServerResponse.class);

				if (res != null && res.isSuccess()) {
					Type tipoLista = new TypeToken<List<String>>() {}.getType();
					destinatariInesistenti.addAll(gson.fromJson(gson.toJson(res.getData()), tipoLista));

				} else {
					Platform.runLater(() -> {
						notificaUtente.set(new Notification(Notification.NotificationType.ERROR,
								"Operazione non riuscita: " + (res != null ? res.getMessage() : "errore del server sconosciuto")));
						isLoading.set(false);   // Fine caricamento
					});
					return;
				}
			} catch (Exception e) {
				Platform.runLater(() -> {
					notificaUtente.set(new Notification(Notification.NotificationType.ERROR, "Errore invio email: " + e.getMessage()));
					isLoading.set(false);   // Fine caricamento
				});
				return;
			}

			// Se alcuni destinatari non esistono, lo segnalo all'utente
			if(!destinatariInesistenti.isEmpty()){
				Platform.runLater(() -> {
					isLoading.set(false);
					notificaUtente.set(new Notification(Notification.NotificationType.ERROR, "Email non inviata."));
					onDestInesistenti.accept(destinatariInesistenti);
				});  // Fine caricamento (se la transazione è terminata)
				return;
			}

			// Se i destinatari esistono, invio la richiesta di send
			try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
			     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			     Scanner in = new Scanner(new InputStreamReader(socket.getInputStream()))) {

				ServerRequest req = new ServerRequest(currentUser.get(), "SND_EML", toSend);
				out.println(gson.toJson(req, ServerRequest.class));
				ServerResponse res = gson.fromJson(in.nextLine(), ServerResponse.class);

				if(res!=null && res.isSuccess()){
					boolean sent = (Boolean) res.getData();
					if(sent){
						Platform.runLater(() -> {
							notificaUtente.set(new Notification(Notification.NotificationType.SUCCESS, "Email inviata."));
							onSuccess.accept(null);
						});
					}
					else{
						Platform.runLater(() -> notificaUtente.set(new Notification(Notification.NotificationType.ERROR, "Errore invio email: " + res.getMessage())));
					}
				}
				else {
					Platform.runLater(() -> notificaUtente.set(new Notification(Notification.NotificationType.ERROR,
							"Operazione non riuscita: " + (res != null ? res.getMessage() : "errore del server sconosciuto"))));
				}
			}
			catch (Exception e) {
				Platform.runLater(() -> notificaUtente.set(new Notification(Notification.NotificationType.ERROR,"Errore invio email: " + e.getMessage())));
			}
			finally {
				Platform.runLater(() -> isLoading.set(false));  // Fine caricamento
			}
		});
	}

	/*
	* Metodi di properties e campi privati
	*/
	public String getCurrentUser(){return currentUser.get();}

	public SimpleStringProperty currentUserProperty(){return currentUser;}

	public SimpleListProperty<Email> inboxProperty(){return inbox;}

	public Email getSelectedEmail(){return selectedEmail.get();}

	public SimpleObjectProperty<Email> selectedEmailProperty(){return selectedEmail;}

	public SimpleObjectProperty<Notification> notificaUtenteProperty() {
		return notificaUtente;
	}

	public BooleanProperty isConnectionOnlineProperty() {
		return isConnectionOnline;
	}

	public BooleanProperty isLoadingProperty() {
		return isLoading;
	}
}
