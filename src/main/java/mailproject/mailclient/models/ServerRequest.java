package mailproject.mailclient.models;

public class ServerRequest {
	private String user;
	private String request;
	private Object data;

	public ServerRequest(String user, String request, Object data) {
		this.user = user;
		this.request = request;
		this.data = data;
	}
}
