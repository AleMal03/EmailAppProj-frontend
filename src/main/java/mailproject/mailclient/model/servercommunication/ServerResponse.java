package mailproject.mailclient.model.servercommunication;

public class ServerResponse {
	private boolean success;
	private String message;
	private Object data;

	public Boolean isSuccess() {
		return success;
	}

	public String getMessage() {
		return message;
	}

	public Object getData() {
		return data;
	}
}
