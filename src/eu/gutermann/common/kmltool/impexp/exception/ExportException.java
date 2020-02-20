package eu.gutermann.common.kmltool.impexp.exception;

public class ExportException extends RuntimeException {
	private static final long serialVersionUID = -7920646337762013189L;

	public ExportException(String message) {
		super(message);
	}
	
	public ExportException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public ExportException(Throwable cause) {
		super(cause);
	}
	
}
