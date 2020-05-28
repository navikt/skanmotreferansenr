package no.nav.skanmotreferansenr.exceptions.functional;

public abstract class AbstractSkanmotreferansenrFunctionalException extends RuntimeException {

    public AbstractSkanmotreferansenrFunctionalException(String message) {
        super(message);
    }

    public AbstractSkanmotreferansenrFunctionalException(String message, Throwable cause) {
        super(message, cause);
    }
}
