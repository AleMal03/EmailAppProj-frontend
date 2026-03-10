package mailproject.mailclient.controllers;

import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.RadialGradient;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import mailproject.mailclient.model.DataModel;
import mailproject.mailclient.model.beans.Email;
import mailproject.mailclient.model.beans.Notification;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.LinkedList;
import java.util.List;

public class InboxController extends MyController{
	private Parent loginView;

	// Overlays
	@FXML VBox boxInbox;
	@FXML VBox notificationsOverlay;
	@FXML VBox loadingOverlay;

	// Elementi header
	@FXML Label lblLoggedUsr;
	@FXML Button btnLogout;
	@FXML Circle ledConnectionStatus;

	// Elementi visualizzazione email in entrata
	@FXML ListView<Email> lstEmails;
	@FXML Label lblFrom;
	@FXML Label lblTo;
	@FXML Label lblSubject;
	@FXML TextArea txtAreaContent;
	@FXML VBox boxSelectedEmail;

	// Elementi scrittura email
	@FXML TextField txtWriteFrom;
	@FXML TextField txtWriteTo;
	@FXML TextField txtWriteSubject;
	@FXML TextArea txtAreaWriteContent;
	@FXML VBox boxWriteEmail;

	@Override
	public void initModel(DataModel model) {
		super.initModel(model);

		setBindings();
	}

	public void setLoginView(Parent loginView) {
		this.loginView = loginView;
	}

	@FXML
	public void onLogoutBtnClick(ActionEvent event){
		model.invalidateSession();
		lstEmails.getSelectionModel().clearSelection();
		cambiaSchermata(loginView, "Login", event);
	}

	@FXML
	public void onWriteEmailBtnClick(){
		resetCampiWrite();  // Reset view
		lstEmails.getSelectionModel().clearSelection();
		boxWriteEmail.setVisible(true);
	}

	@FXML
	public void onReplyBtnClick(){
		resetCampiWrite();  // Reset view

		Email replyEmail = model.getSelectedEmail();  // Email a cui si sta rispondendo

		// Setting info
		txtWriteTo.setText(model.getSelectedEmail().getMittente());
		txtWriteSubject.setText("Re: " + model.getSelectedEmail().getOggetto());
		txtAreaWriteContent.setText("\n\n" +
				"In data " + replyEmail.getDataSpedizioneAsString() + " " + replyEmail.getMittente() + " ha scritto:\n\t" +
				replyEmail.getContenuto());

		// Cambio visualizzazione
		lstEmails.getSelectionModel().clearSelection();
		boxWriteEmail.setVisible(true);
	}

	@FXML
	public void onReplyAllBtnClick(){
		resetCampiWrite();  // Reset view

		Email replyEmail = model.getSelectedEmail();  // Email a cui si sta rispondendo
		List<String> destinatari = new LinkedList<>(replyEmail.getDestinatari());   // Prendo (copio) TUTTI i destinatari
		destinatari.remove(model.getCurrentUser());        // Tolgo il current user
		destinatari.addFirst(replyEmail.getMittente());    // Aggiungo il mittente (in testa, come primo destinatario)
		String destinatariStr = String.join(", ", destinatari);

		// Setting info
		txtWriteTo.setText(destinatariStr);
		txtWriteSubject.setText("Re: " + replyEmail.getOggetto());
		txtAreaWriteContent.setText("\n\n" +
				"In data " + replyEmail.getDataSpedizioneAsString() + " " + replyEmail.getMittente() + " ha scritto:\n\t" +
				replyEmail.getContenuto());

		// Cambio visualizzazione
		lstEmails.getSelectionModel().clearSelection();
		boxWriteEmail.setVisible(true);
	}

	@FXML
	public void onShareBtnClick(){
		resetCampiWrite();  // Reset view

		Email emailToShare = model.getSelectedEmail();  // Email da condividere

		//Setting info
		txtWriteSubject.setText("Fwd: " + model.getSelectedEmail().getOggetto());
		txtAreaWriteContent.setText("\n\n" +
				"---------- Forwarded message ----------\n" +
				"Da: " + emailToShare.getMittente() + "\n" +
				"Data: " + emailToShare.getDataSpedizioneAsString() + "\n" +
				"Oggetto: " + emailToShare.getOggetto() + "\n" +
				"A: " + emailToShare.getDestinatariAsString() + "\n\n" +
				emailToShare.getContenuto());


		// Cambio visualizzazione
		lstEmails.getSelectionModel().clearSelection();
		boxWriteEmail.setVisible(true);
	}

	@FXML
	public void onDeleteBtnClick(){
		deleteEmail(model.getSelectedEmail());
	}

	@FXML
	public void onWriteSubmit(){
		// Verifico prima che l'utente abbia inserito almeno un mittente
		if(txtWriteTo.getText().isEmpty()){
			model.notificaUtenteProperty().setValue(new Notification(Notification.NotificationType.INFO,
					"Il campo destinatari è vuoto"));
			return;
		}

		model.sendEmail(new Email(
				txtWriteSubject.getText(),
				txtAreaWriteContent.getText(),
				txtWriteFrom.getText(),
				List.of(txtWriteTo.getText().split("\\s*,\\s*"))    // Split ignorando gli spazi
			),
			destinatariInesistenti -> {
				new Alert(Alert.AlertType.ERROR,
					"I seguenti indirizzi inseriti non esistono: " +
						String.join(", ", destinatariInesistenti) +
						".\nSi prega di verificare e riprovare."
				).showAndWait();
			},
			_ -> {
				resetCampiWrite();
				boxWriteEmail.setVisible(false);
			}
		);
	}

	@FXML
	public void onWriteCanc(){
		boxWriteEmail.setVisible(false);
		resetCampiWrite();
	}

	private void deleteEmail(Email toDelete){
		model.deleteEmail(toDelete);
	}


	/**
	 * Setta tutti i bindings necessari alla view.
	 */
	private void setBindings(){
		// Binding overlay caricamenti
		loadingOverlay.visibleProperty().bind(model.isLoadingProperty());

		// Binding per visualizzare l'utente corrente
		lblLoggedUsr.textProperty().bind(model.currentUserProperty());

		// Binding colore led
		ledConnectionStatus.fillProperty().bind(
				Bindings.when(model.isConnectionOnlineProperty())
						.then(RadialGradient.valueOf("focus-angle 0.0deg, focus-distance 0.0% , center 52.21238938053098% 47.348485570965394%, radius 100.0%, 0x4eff2fff 0.0%, 0xd5eb15b0 100.0%"))
						.otherwise(RadialGradient.valueOf("focus-angle 0.0deg, focus-distance 0.0% , center 52.21238938053098% 47.348485570965394%, radius 100.0%, 0xff1515ff 0.0%, 0xeb7915bb 100.0%"))
		);

		// Listener per visualizzare messaggi di errore sull'overlay man mano che si presentano
		model.notificaUtenteProperty().addListener((_, _, newValue) -> {
			if(newValue != null) {
				addNotificationToOverlay(newValue);
				model.notificaUtenteProperty().set(null);   // Reset notifica
			}
		});

		// Bindings per visualizzare l'email aperta
		boxSelectedEmail.visibleProperty().bind(model.selectedEmailProperty().isNotNull()); // Il pannello di DX si mostra solo se c'è un'email selezionata
		lblFrom.textProperty().bind(Bindings.createStringBinding(
				() -> {
					// Recupero l'email selezionata
					Email email = model.getSelectedEmail();

					if(email == null){
						return "<errore>";
					}

					return email.getMittente();
				}, model.selectedEmailProperty()    // Dipendenza
			)
		);
		lblTo.textProperty().bind(Bindings.createStringBinding(
				() -> {
					// Recupero l'email selezionata
					Email email = model.getSelectedEmail();

					if(email == null){
						return "<errore>";
					}

					return email.getDestinatariAsString();
				}, model.selectedEmailProperty()    // Dipendenza
			)
		);
		lblSubject.textProperty().bind(Bindings.createStringBinding(
				() -> {
					// Recupero l'email selezionata
					Email email = model.getSelectedEmail();

					if(email == null){
						return "<errore>";
					}

					return email.getOggetto();
				}, model.selectedEmailProperty()    // Dipendenza
			)
		);
		txtAreaContent.textProperty().bind(Bindings.createStringBinding(
				() -> {
					// Recupero l'email selezionata
					Email email = model.getSelectedEmail();

					if(email == null){
						return "<errore>";
					}

					return email.getContenuto();
				}, model.selectedEmailProperty()    // Dipendenza
			)
		);

		/* Bindings per visualizzare la lista di emails */
		lstEmails.itemsProperty().bind(model.inboxProperty());
		lstEmails.setCellFactory(l -> new ListCell<>(){

			// Contenuto riga della lista
			private final Label anteprimaEmail = new Label();
			private final Label dataInvio = new Label();
			private final Button deleteButton = new Button();
			private final HBox contentContainer = new HBox(anteprimaEmail, dataInvio, deleteButton);

			{
				deleteButton.getStyleClass().add("btnDelete");
				deleteButton.setGraphic(new FontIcon("far-trash-alt"));
				// Azione: rimuove l'email corrente
				deleteButton.setOnAction(_ -> deleteEmail(getItem()));
			}

			// La view viene aggiornata con le modifiche del model.inbox
			@Override
			protected void updateItem(Email item, boolean empty){
				super.updateItem(item, empty);

				if(empty || item == null){
					setGraphic(null);
				}
				else{
					anteprimaEmail.setText(item.getAnteprima());
					dataInvio.setText(item.getDataSpedizioneAsString());
					setGraphic(contentContainer);
				}
			}
		});

		//Gestione selezione email dalla lista
		model.selectedEmailProperty().bind(lstEmails.getSelectionModel().selectedItemProperty());
		model.selectedEmailProperty().addListener((_, _, newEmail) -> {
			if (newEmail != null) {
				boxWriteEmail.setVisible(false);
				newEmail.setLetta(true);
			}
		});

		// Bindings scrittura email
		txtWriteFrom.textProperty().bind(model.currentUserProperty());
	}

	/**
	 * Resetta il contenuto dei campi della scrittura dell'email
	 */
	private void resetCampiWrite(){
		txtWriteTo.setText("");
		txtWriteSubject.setText("");
		txtAreaWriteContent.setText("");
	}

	/**
	 * Permette stackare nuove notifiche nell'apposito overlay della GUI quando si presentano.
	 *
	 * @param notification Notifica da far comparire.
	 */
	private void addNotificationToOverlay(Notification notification) {
		Label lblNotification = new Label(notification.getMessage());
		String cssClass = switch (notification.getType()) {
			case ERROR -> "lblError";
			case INFO -> "lblInfo";
			case SUCCESS -> "lblSuccess";
		};

		lblNotification.getStyleClass().add("lblNotification");     // Stile CSS
		lblNotification.getStyleClass().add(cssClass);              // Stile CSS

		notificationsOverlay.getChildren().add(lblNotification);  // Aggiungo la label al VBox in sovra impressione

		// Imposto un timer di 4 secondi per farla sparire e liberare lo spazio
		PauseTransition delay = new PauseTransition(Duration.seconds(4));
		delay.setOnFinished(_ -> notificationsOverlay.getChildren().remove(lblNotification));
		delay.play();
	}
}

