package no.nav.skanmotreferansenr.exception.functional;

public class SkanmotreferansenrFunctionalException extends RuntimeException {

    public SkanmotreferansenrFunctionalException(String message) {
        super(message);
    }

    public SkanmotreferansenrFunctionalException(String message, Throwable cause) {
        super(message, cause);
    }
}
