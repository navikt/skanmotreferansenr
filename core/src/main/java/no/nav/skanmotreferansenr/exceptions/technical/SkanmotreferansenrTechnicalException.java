package no.nav.skanmotreferansenr.exceptions.technical;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
public class SkanmotreferansenrTechnicalException extends AbstractSkanmotreferansenrTechnicalException {
    public SkanmotreferansenrTechnicalException(String message) {
        super(message);
    }

    public SkanmotreferansenrTechnicalException(String message, Throwable cause) {
        super(message, cause);
    }
}
