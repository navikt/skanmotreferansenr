package no.nav.skanmotreferansenr.exception.technical;

public class SkanmotreferansenrTechnicalException extends RuntimeException{

    public SkanmotreferansenrTechnicalException(String message) {
        super(message);
    }

    public SkanmotreferansenrTechnicalException(String message, Throwable cause) {
        super(message, cause);
    }
}
