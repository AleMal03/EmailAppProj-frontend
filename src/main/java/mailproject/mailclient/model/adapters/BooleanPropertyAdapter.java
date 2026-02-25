package mailproject.mailclient.model.adapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.io.IOException;

public class BooleanPropertyAdapter extends TypeAdapter<BooleanProperty> {
	@Override
	public void write(JsonWriter out, BooleanProperty value) throws IOException {
		if (value == null) {
			out.nullValue();
		} else {
			out.value(value.get()); // Estrae il boolean primitivo
		}
	}

	@Override
	public BooleanProperty read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}
		return new SimpleBooleanProperty(in.nextBoolean()); // Ricrea la property per JavaFX
	}
}