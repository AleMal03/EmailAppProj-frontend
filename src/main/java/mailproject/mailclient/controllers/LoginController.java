package mailproject.mailclient.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;

public class LoginController extends MyController{
	private Parent inboxView;

	@FXML
	Button btnLogin;

	@FXML
	Label lblLoginError;

	@FXML
	TextField txtEmailAddr;

	public void setInboxView(Parent inboxView) {
		this.inboxView = inboxView;
	}

	@FXML
	protected void onLoginBtnClick(ActionEvent event) {
		lblLoginError.setText("");

		try{
			model.createSession(txtEmailAddr.getText());
			cambiaSchermata(inboxView, "Mail inbox", event);
			txtEmailAddr.setText("");
		}
		catch(Exception e){
			System.out.println(e.toString());
			lblLoginError.setText(e.getMessage());
		}
	}

	// todo verifica stato server
}

