package mailproject.mailclient;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import mailproject.mailclient.controllers.InboxController;
import mailproject.mailclient.controllers.LoginController;
import mailproject.mailclient.model.DataModel;

import java.io.IOException;

public class HelloApplication extends Application {
	@Override
	public void start(Stage stage) throws IOException {
		// Creazione Model
		DataModel model = new DataModel();

		// Creazione loader delle view (con rispettivi controller specificati nel file FXML)
		FXMLLoader loginLoader = new FXMLLoader(HelloApplication.class.getResource("LoginView.fxml"));
		FXMLLoader inboxLoader = new FXMLLoader(HelloApplication.class.getResource("InboxView.fxml"));

		// Loading delle view
		Parent loginView = loginLoader.load();
		Parent inboxView = inboxLoader.load();

		// Init model sui controller
		LoginController loginController = loginLoader.getController();
		loginController.initModel(model);

		InboxController inboxController = inboxLoader.getController();
		inboxController.initModel(model);

		// Passo la view al controller per il cambio di finestra
		loginController.setInboxView(inboxView);
		inboxController.setLoginView(loginView);


		Scene scene = new Scene(loginView, 1100, 500);
		scene.getStylesheets().add(HelloApplication.class.getResource("/style/style.css").toExternalForm());
		stage.setTitle("Login");
		stage.setScene(scene);
		stage.setMinWidth(1100);
		stage.show();
	}
}
