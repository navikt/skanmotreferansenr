package no.nav.skanmotreferansenr.exceptions.functional;

public class InvalidMetadataException extends AbstractSkanmotreferansenrFunctionalException {

    public InvalidMetadataException(String message) {
        super(message);
    }

    public InvalidMetadataException(String message, Throwable cause) {
        super(message, cause);
    }
}
