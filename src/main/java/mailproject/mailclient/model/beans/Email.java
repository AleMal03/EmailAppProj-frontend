package mailproject.mailclient.model.beans;

import javafx.beans.property.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class Email {
	private final long id;                   // Identificativo email
	private final String mittente;          // Mittente dell'email
	private final Set<String> destinatari; // Lista dei destinatari (se molteplici) dell'email
	private final String oggetto;           // Oggetto dell'email
	private final String contenuto;         // Contenuto (testo) dell'email
	private BooleanProperty letta;          // Indica se l'email è stata già letta o meno (per visualizzazione)
	private final LocalDateTime dataSpedizione;  // Timestamp spedizione email

	public Email(String oggetto, String contenuto, String mittente, Set<String> destinatari) {
		this.id = -1;
		this.contenuto = contenuto;
		this.oggetto = oggetto;
		this.destinatari = new HashSet<>(destinatari);
		this.mittente = mittente;
		this.dataSpedizione = null;
		this.letta = null;
	}

	public long getId() {
		return id;
	}

	public void setLetta(boolean letta) {
		lettaProperty().set(letta);
	}

	public String getMittente() {
		return mittente;
	}

	public Set<String> getDestinatari() {
		return Set.copyOf(destinatari);
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
		if(dataSpedizione != null){
			int day = dataSpedizione.getDayOfMonth();
			int month = dataSpedizione.getMonthValue();
			int hour = dataSpedizione.getHour();
			int minute = dataSpedizione.getMinute();

			return  (day < 10 ? "0" : "") + day + "/" +
					(month < 10 ? "0" : "") + month + "/" +
					dataSpedizione.getYear() + " – " +
					(hour < 10 ? "0" : "") + hour + ":" +
					(minute < 10 ? "0" : "") + minute;
		}

		return "null";
	}

	public String getAnteprima(){
		String anteprima = oggetto + " – " + contenuto;

		return anteprima.replaceAll("\\R", " ");    // Elimino tutti gli "a capo" (\n, \r, \r\n)
	}
}
