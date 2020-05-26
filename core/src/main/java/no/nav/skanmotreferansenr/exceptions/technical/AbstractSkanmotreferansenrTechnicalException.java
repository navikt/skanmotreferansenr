package no.nav.skanmotreferansenr.exceptions.technical;

public abstract class AbstractSkanmotreferansenrTechnicalException extends RuntimeException{

    public AbstractSkanmotreferansenrTechnicalException(String message) {
        super(message);
    }

    public AbstractSkanmotreferansenrTechnicalException(String message, Throwable cause) {
        super(message, cause);
    }
}
