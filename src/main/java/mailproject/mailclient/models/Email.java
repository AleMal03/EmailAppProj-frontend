package mailproject.mailclient.models;

import javafx.beans.property.*;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;
import java.util.List;

public class Email {
	private final long id;                  // Identificativo email
	private final String mittente;          // Mittente dell'email
	private final List<String> destinatari; // Lista dei destinatari (se molteplici) dell'email
	private final String oggetto;           // Oggetto dell'email
	private final String contenuto;         // Contenuto (testo) dell'email
	private BooleanProperty letta;          // Indica se l'email è stata già letta o meno (per visualizzazione)
	private final LocalDateTime dataRicezione;  // Timestamp ricezione email

	public Email(long id, String contenuto, String oggetto, List<String> destinatari, String mittente, LocalDateTime dataRicezione, boolean letta) {
		this.id = id;
		this.contenuto = contenuto;
		this.oggetto = oggetto;
		this.destinatari = destinatari;
		this.mittente = mittente;
		this.dataRicezione = dataRicezione;
		this.letta = new SimpleBooleanProperty(letta);
	}

	public long getId() {
		return id;
	}

	public void setLetta(boolean letta) {
		this.letta.set(letta);
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
		return letta.get();
	}

	public BooleanProperty lettaProperty() {
		return letta;
	}

	public LocalDateTime getDataRicezione() {
		return dataRicezione;
	}

	public String getDestinatariAsString() {
		return String.join(", ", getDestinatari());
	}

	public String getDataRicezioneAsString(){
		return dataRicezione.getDayOfMonth() + "/" +
				dataRicezione.getMonthValue() + "/" +
				dataRicezione.getYear() + " - " +
				dataRicezione.getHour() + ":" +
				dataRicezione.getMinute();
	}

	public SimpleStringProperty anteprimaProperty(){
		String anteprima = oggetto + " - " + contenuto;

		if (anteprima.length() > 50)
			anteprima = anteprima.substring(0,50) + "...";

		return new SimpleStringProperty(anteprima);
	}
}
