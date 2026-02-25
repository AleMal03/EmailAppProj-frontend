package mailproject.mailclient.model.adapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDateTime;

public class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
	@Override
	public void write(JsonWriter out, LocalDateTime value) throws IOException {
		if (value == null) {
			out.nullValue();
		} else {
			out.value(value.toString()); // Scrive la data come stringa
		}
	}

	@Override
	public LocalDateTime read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}
		return LocalDateTime.parse(in.nextString()); // Ricrea l'oggetto dalla stringa
	}
}
