module mailproject.mailclient {
	requires javafx.controls;
	requires javafx.fxml;

	requires org.kordamp.ikonli.javafx;
	requires javafx.graphics;
	requires com.google.gson;

	opens mailproject.mailclient.controllers to javafx.fxml;
	opens mailproject.mailclient.models to javafx.base;
	opens mailproject.mailclient to javafx.fxml;
	exports mailproject.mailclient;
}