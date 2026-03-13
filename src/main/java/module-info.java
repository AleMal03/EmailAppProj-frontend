module mailproject.mailclient {
	requires javafx.controls;
	requires javafx.fxml;

	requires org.kordamp.ikonli.javafx;
	requires javafx.graphics;
	requires com.google.gson;
	requires org.controlsfx.controls;
	requires java.compiler;
	requires java.desktop;
	requires javafx.media;

	opens mailproject.mailclient.controllers to javafx.fxml;
	opens mailproject.mailclient.model to javafx.base;
	opens mailproject.mailclient to javafx.fxml;
	exports mailproject.mailclient;
	opens mailproject.mailclient.model.adapters to javafx.base, com.google.gson;
	opens mailproject.mailclient.model.beans to javafx.base, com.google.gson;
	opens mailproject.mailclient.model.servercommunication to javafx.base, com.google.gson;
}