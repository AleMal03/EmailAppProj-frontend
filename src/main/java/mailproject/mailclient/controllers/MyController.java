package mailproject.mailclient.controllers;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.Stage;
import mailproject.mailclient.model.DataModel;

public class MyController {
	protected DataModel model;

	public void initModel(DataModel model){
		if(this.model != null){
			throw new IllegalStateException("Il model può essere inizializzato solo una volta");
		}
		this.model = model;
	};

	/**
	 * Modifica la view.
	 *
	 * @param view  Vista da mostrare nello stage.
	 * @param titolo Titolo dello stage.
	 * @param event Evento che ha causato il cambio di schermata.
	 */
	protected void cambiaSchermata(Parent view, String titolo, ActionEvent event){
		// A. Recupera lo Stage attuale (la finestra)
		// Usiamo l'evento del click per risalire alla finestra in cui siamo
		Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

		// B. Imposta la scena sullo stage modificandone il contenuto con la view desiderata
		stage.getScene().setRoot(view);
		stage.setTitle(titolo); // Cambia il titolo dello stage
	}
}
