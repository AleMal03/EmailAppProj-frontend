package mailproject.mailclient.controllers;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import mailproject.mailclient.models.DataModel;
import mailproject.mailclient.models.Email;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class InboxController extends MyController{
	private Parent loginView;

	// Elementi header
	@FXML Label lblLoggedUsr;
	@FXML Button btnLogout;

	// Elementi visualizzazione email in entrata
	@FXML TableView<Email> tblEmails;
	@FXML TableColumn<Email, String> colContent;
	@FXML TableColumn<Email, String> colDate;
	@FXML TableColumn<Email, Email> colDelete;
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
		tblEmails.getSelectionModel().clearSelection();
		cambiaSchermata(loginView, "Login", event);
	}

	@FXML
	public void onWriteEmailBtnClick(ActionEvent event){
		tblEmails.getSelectionModel().clearSelection();
		boxWriteEmail.setVisible(true);
		resetCampiWrite();
	}

	@FXML
	public void onReplyBtnClick(ActionEvent event){
		Email replyEmail = model.getSelectedEmail();  // Email a cui si sta rispondendo

		// Setting info
		txtWriteTo.setText(model.getSelectedEmail().getMittente());
		txtWriteSubject.setText("Re: " + model.getSelectedEmail().getOggetto());
		txtAreaWriteContent.setText("\n\n" +
				"In data " + replyEmail.getDataRicezioneAsString() + " " + replyEmail.getMittente() + " ha scritto:\n\t" +
				replyEmail.getContenuto());

		// Cambio visualizzazione
		tblEmails.getSelectionModel().clearSelection();
		boxWriteEmail.setVisible(true);
	}

	@FXML
	public void onReplyAllBtnClick(ActionEvent event){
		Email replyEmail = model.getSelectedEmail();  // Email a cui si sta rispondendo
		List<String> destinatari = model.getSelectedEmail().getDestinatari();   // Prendo TUTTI i destinatari
		destinatari.remove(model.getCurrentUser());                 // Tolgo il current user
		destinatari.add(model.getSelectedEmail().getMittente());    // Aggiungo il mittente
		String destinatariStr = String.join(", ", destinatari);

		// Setting info
		txtWriteTo.setText(destinatariStr);
		txtWriteSubject.setText("Re: " + model.getSelectedEmail().getOggetto());
		txtAreaWriteContent.setText("\n\n" +
				"In data " + replyEmail.getDataRicezioneAsString() + " " + replyEmail.getMittente() + " ha scritto:\n\t" +
				replyEmail.getContenuto());

		// Cambio visualizzazione
		tblEmails.getSelectionModel().clearSelection();
		boxWriteEmail.setVisible(true);
	}

	@FXML
	public void onShareBtnClick(ActionEvent event){
		Email emailToShare = model.getSelectedEmail();  // Email da condividere

		//Setting info
		txtWriteSubject.setText("Fwd: " + model.getSelectedEmail().getOggetto());
		txtAreaWriteContent.setText("\n\n" +
				"---------- Forwarded message ----------\n" +
				"Da: " + emailToShare.getMittente() + "\n" +
				"Data: " + emailToShare.getDataRicezioneAsString() + "\n" +
				"Oggetto: " + emailToShare.getOggetto() + "\n" +
				"A: " + emailToShare.getDestinatariAsString() + "\n\n" +
				emailToShare.getContenuto());


		// Cambio visualizzazione
		tblEmails.getSelectionModel().clearSelection();
		boxWriteEmail.setVisible(true);
	}

	@FXML
	public void onDeleteBtnClick(ActionEvent event){
		model.inboxProperty().remove(model.getSelectedEmail());
	}

	@FXML
	public void onWriteSubmit(ActionEvent event){
		boxWriteEmail.setVisible(false);
		//todo invio email
		//todo verifica sintassi indirizzi email inseriti
		resetCampiWrite();
	}

	@FXML
	public void onWriteCanc(ActionEvent event){
		boxWriteEmail.setVisible(false);
		resetCampiWrite();
	}


	/**
	 * Setta tutti i bindings necessari alla view.
	 */
	private void setBindings(){
		// Binding per visualizzare l'utente corrente
		lblLoggedUsr.textProperty().bind(model.currentUserProperty());

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
		tblEmails.itemsProperty().bind(model.inboxProperty());
		// Colonna anteprima email
		colContent.setCellValueFactory(email -> email.getValue().anteprimaProperty());    // Una colonna conterrà l'anteprima dell'email
		colContent.setCellFactory(column -> new TableCell<>(){
			// Creiamo una label dentro la cella per avere maggiore controllo su di essa
			private final Label lblTesto = new Label();

			{
				// Diciamo alla cella di mostrare solo la label
				setGraphic(lblTesto);
				setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

				// Bindiamo il testo sulla label in base al contenuto della cella
				lblTesto.textProperty().bind(itemProperty());

				// Binding condizionale dello style per il grassetto
				lblTesto.styleProperty().bind(
						Bindings.when(emptyProperty().or(itemProperty().isNull()))
								.then("")   // Se la cella è vuota, cancella qualunque stile
								.otherwise(     // Altrimenti applica lo stile condizionale
										// TableRow -> Email (item) -> letta
										Bindings.when(Bindings.selectBoolean(tableRowProperty(), "item", "letta"))
												.then("-fx-font-weight: normal;")
												.otherwise("-fx-font-weight: bold;")
								)
				);
			}
		});
		//Colonna data di ricezione
		colDate.setCellValueFactory(email -> new ReadOnlyObjectWrapper<>(
				email.getValue().getDataRicezioneAsString()));
		// Colonna per il tasto delete
		colDelete.setCellValueFactory(email -> new ReadOnlyObjectWrapper<>(email.getValue()));    // La colonna per l'eliminazione contiene l'intera email da eliminare
		colDelete.setCellFactory(_ -> new TableCell<Email, Email>(){
			private final Button deleteButton = new Button();

			{
				deleteButton.getStyleClass().add("btnDelete");
				deleteButton.getStyleClass().add("btn");
				deleteButton.setGraphic(new FontIcon("far-trash-alt")); // Setta l'icona sul button

				// AZIONE: modifica dei dati sul model
				deleteButton.setOnAction(event -> {
					model.inboxProperty().remove(getItem());    // Elimina dalla lista del model l'Email presa dalla TableCell con getItem()
				});

				// Visualizzazione condizionale del button (solo se la riga contiene un'email)
				graphicProperty().bind(
						Bindings.when(emptyProperty().or(itemProperty().isNull()))
								.then((Node) null)
								.otherwise(deleteButton)
				);
			}
		});

		//Gestione selezione email dalla tabella
		model.selectedEmailProperty().bind(tblEmails.getSelectionModel().selectedItemProperty());
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
}

