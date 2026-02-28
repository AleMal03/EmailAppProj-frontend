package mailproject.mailclient.model.beans;

import javafx.beans.property.*;

import java.time.LocalDateTime;
import java.util.List;

public class Email {
	private final int id;                   // Identificativo email
	private final String mittente;          // Mittente dell'email
	private final List<String> destinatari; // Lista dei destinatari (se molteplici) dell'email
	private final String oggetto;           // Oggetto dell'email
	private final String contenuto;         // Contenuto (testo) dell'email
	private BooleanProperty letta;          // Indica se l'email è stata già letta o meno (per visualizzazione)
	private final LocalDateTime dataSpedizione;  // Timestamp spedizione email

	public Email(int id, String contenuto, String oggetto, List<String> destinatari, String mittente, LocalDateTime dataSpedizione, boolean letta) {
		this.id = id;
		this.contenuto = contenuto;
		this.oggetto = oggetto;
		this.destinatari = destinatari;
		this.mittente = mittente;
		this.dataSpedizione = dataSpedizione;
		this.letta = new SimpleBooleanProperty(letta);
	}

	public int getId() {
		return id;
	}

	public void setLetta(boolean letta) {
		lettaProperty().set(letta);
	}

	public String getMittente() {
		return mittente;
	}

	public List<String> getDestinatari() {
		return destinatari;
	}

	public String getOggetto() {
		return oggetto;
	}

	public String getContenuto() {
		return contenuto;
	}

	public boolean isLetta() {
		return lettaProperty().get();
	}

	public BooleanProperty lettaProperty() {
		if (letta == null) {
			letta = new SimpleBooleanProperty(false);
		}
		return letta;
	}

	public LocalDateTime getDataSpedizione() {
		return dataSpedizione;
	}

	public String getDestinatariAsString() {
		return String.join(", ", getDestinatari());
	}

	public String getDataSpedizioneAsString(){
		return dataSpedizione.getDayOfMonth() + "/" +
				dataSpedizione.getMonthValue() + "/" +
				dataSpedizione.getYear() + " - " +
				dataSpedizione.getHour() + ":" +
				dataSpedizione.getMinute();
	}

	public String getAnteprima(){
		String anteprima = oggetto + " - " + contenuto;

		if (anteprima.length() > 50)
			anteprima = anteprima.substring(0,50) + "...";

		return anteprima;
	}
}
