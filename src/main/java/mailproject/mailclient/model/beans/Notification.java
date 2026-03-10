package mailproject.mailclient.model.beans;

public class Notification {
	public enum NotificationType {ERROR, INFO, SUCCESS}
	private final NotificationType type;
	private final String message;

	public Notification(NotificationType type, String message) {
		this.type = type;
		this.message = message;
	}

	public NotificationType getType() {
		return type;
	}

	public String getMessage() {
		return message;
	}
}
