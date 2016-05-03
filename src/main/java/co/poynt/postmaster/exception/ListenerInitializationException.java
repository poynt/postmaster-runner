package co.poynt.postmaster.exception;

public class ListenerInitializationException extends RuntimeException {
	public ListenerInitializationException(String message) {
		super(message);
	}

	public ListenerInitializationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ListenerInitializationException(Throwable cause) {
		super(cause);
	}
}
