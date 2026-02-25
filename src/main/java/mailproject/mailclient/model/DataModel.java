package mailproject.mailclient.model;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import mailproject.mailclient.model.adapters.BooleanPropertyAdapter;
import mailproject.mailclient.model.adapters.LocalDateTimeAdapter;
import mailproject.mailclient.model.beans.Email;
import mailproject.mailclient.model.beans.EmailVerifier;

import java.io.FileWriter;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class DataModel {
	private final SimpleStringProperty currentUser;     // Indirizzo email dell'utente loggato
	private final SimpleListProperty<Email> inbox;      // Inbox: lista email in arrivo
	private final SimpleObjectProperty<Email> selectedEmail;    // Email attualmente selezionata
	private final ExecutorService exec;    // Esecutore singolo per richieste al server come verifica dell'email

	public DataModel() {
		currentUser = new SimpleStringProperty(null);
		inbox = new SimpleListProperty<>(javafx.collections.FXCollections.observableArrayList());
		selectedEmail = new SimpleObjectProperty<>(null);
		exec = Executors.newSingleThreadExecutor();

		// Listener current user per sincronizzazione inbox al login
		currentUser.addListener((_, _, newValue) -> {
			if(newValue != null)
				syncInbox(newValue);
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
		EmailVerifier verifier = new EmailVerifier(email, currentUser.get());
		return verifier.call();
	}

	public String getCurrentUser(){return currentUser.get();}

	public SimpleStringProperty currentUserProperty(){return currentUser;}

	public SimpleListProperty<Email> inboxProperty(){return inbox;}

	public void setSelectedEmail(Email selectedEmail){
		this.selectedEmail.set(selectedEmail);
	}

	public Email getSelectedEmail(){return selectedEmail.get();}

	public SimpleObjectProperty<Email> selectedEmailProperty(){return selectedEmail;}

	private void syncInbox(String emailAddr){
		String user = emailAddr.split("@")[0];  // Estraggo user dall'indirizzo email
		Gson gson = new GsonBuilder()
				.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
				.registerTypeAdapter(BooleanProperty.class, new BooleanPropertyAdapter())
				.create();

		try(FileWriter writer = new FileWriter("data/" + user + ".json")){
			// todo richiesta al server
		}
		catch(Exception e){
			throw new RuntimeException("Errore sincronizzazione inbox: " + e.getMessage());
		}
	}
}
