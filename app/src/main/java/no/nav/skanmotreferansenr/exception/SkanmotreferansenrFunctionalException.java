package no.nav.skanmotreferansenr.exception;

public abstract class SkanmotreferansenrFunctionalException extends RuntimeException {

    public SkanmotreferansenrFunctionalException(String message) {
        super(message);
    }

    public SkanmotreferansenrFunctionalException(String message, Throwable cause) {
        super(message, cause);
    }
}
