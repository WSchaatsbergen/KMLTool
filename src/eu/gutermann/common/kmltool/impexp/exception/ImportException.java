package eu.gutermann.common.kmltool.impexp.exception;

public class ImportException extends RuntimeException {
	private static final long serialVersionUID = -7920646337762013189L;

	public ImportException(String message) {
		super(message);
	}
	
	public ImportException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public ImportException(Throwable cause) {
		super(cause);
	}
	
}
