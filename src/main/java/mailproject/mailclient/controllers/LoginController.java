package mailproject.mailclient.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LoginController extends MyController{
	private Parent inboxView;

	@FXML Button btnLogin;
	@FXML Label lblLoginError;
	@FXML TextField txtEmailAddr;
	@FXML VBox boxLogin;

	public void setInboxView(Parent inboxView) {
		this.inboxView = inboxView;
	}

	@FXML
	protected void onLoginBtnClick(ActionEvent event) {
		boxLogin.setCursor(Cursor.WAIT);
		btnLogin.setDisable(true);
		lblLoginError.setText("");

		model.createSession(txtEmailAddr.getText(),
			(success)->{
				if(success){
					cambiaSchermata(inboxView, "Mail inbox", event);
					txtEmailAddr.setText("");
				}
				else{
					lblLoginError.setText("Errore imprevisto");     // Non accadrà mai perché non ritorna mai false
				}

				// Reset GUI asincrono
				btnLogin.setDisable(false);
				boxLogin.setCursor(Cursor.DEFAULT);
			},
			(errorMsg)->{
				System.err.println(errorMsg);   // Per debugging
				lblLoginError.setText(errorMsg);

				// Reset GUI asincrono
				btnLogin.setDisable(false);
				boxLogin.setCursor(Cursor.DEFAULT);
			}
		);
	}
}

