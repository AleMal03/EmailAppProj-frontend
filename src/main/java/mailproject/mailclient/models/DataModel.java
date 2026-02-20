package mailproject.mailclient.models;


import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.util.converter.LocalDateStringConverter;
import javafx.util.converter.LocalDateTimeStringConverter;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class DataModel {
	private final SimpleStringProperty currentUser;     // Indirizzo email dell'utente loggato
	private final SimpleListProperty<Email> inbox;      // Inbox: lista email in arrivo
	private final SimpleObjectProperty<Email> selectedEmail;    // Email attualmente selezionata

	public DataModel() {
		currentUser = new SimpleStringProperty(null);
		inbox = new SimpleListProperty<>(javafx.collections.FXCollections.observableArrayList());
		selectedEmail = new SimpleObjectProperty<>(null);

		// Init di prova
		ArrayList<String> destinatari = new ArrayList<String>();
		destinatari.add("Ciao");

		inbox.add(new Email(0, "Questa è la prova 1","Prova 1", destinatari, "io", LocalDateTime.now(), false));
		inbox.add(new Email(1, "Questa è la prova 2, continua a provare no vabbe lol XD pazzesco","Prova 2", destinatari, "io", LocalDateTime.now(), false));
		inbox.add(new Email(2, "Questa è la prova 3, saaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaas","Prova 3", destinatari, "io", LocalDateTime.now(), false));
	}

	/**
	 * Esegue il login nella mailbox.
	 *
	 * @param emailAddress Indirizzo email dell'utente che effettua il login.
	 * @throws IllegalStateException Se si tenta di eseguire l'accesso quando un utente è già loggato.
	 * @throws IllegalArgumentException Se l'indirizzo email non è valido.
	 */
	public void createSession(String emailAddress) throws IllegalArgumentException, IllegalStateException{
		if(currentUser.get() != null){
			throw new IllegalStateException("Utente già loggato");
		}

		emailAddress = emailAddress.strip();

		if(!isEmailValid(emailAddress)){
			throw new IllegalArgumentException("Indirizzo email inserito non valido");
		}

		if(!emailExists(emailAddress)){
			throw new  IllegalArgumentException("L'indirizzo email inserito non esiste");
		}

		currentUser.set(emailAddress);
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

		if(email==null || email.isEmpty() || !email.matches(emailRegex)){
			return false;
		}

		return true;
	}

	/**
	 * Verifica esistenza dell'indirizzo email comunicando col server.
	 *
	 * @param email Indirizzo email da cercare.
	 * @return true se l'email esiste nel server, false altrimenti.
	 */
	private boolean emailExists(String email){
		//todo richiesta al server per email esistente
		return true;
	}

	public String getCurrentUser(){return currentUser.get();}

	public SimpleStringProperty currentUserProperty(){return currentUser;}

	public SimpleListProperty<Email> inboxProperty(){return inbox;}

	public void setSelectedEmail(Email selectedEmail){
		this.selectedEmail.set(selectedEmail);
	}

	public Email getSelectedEmail(){return selectedEmail.get();}

	public SimpleObjectProperty<Email> selectedEmailProperty(){return selectedEmail;}
}
