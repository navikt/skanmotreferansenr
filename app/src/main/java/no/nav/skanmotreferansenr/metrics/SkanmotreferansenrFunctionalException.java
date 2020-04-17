package no.nav.skanmotreferansenr.metrics;

public class SkanmotreferansenrFunctionalException extends RuntimeException {

    public SkanmotreferansenrFunctionalException() {
        super();
    }

    public SkanmotreferansenrFunctionalException(String message) {
        super(message);
    }

    public SkanmotreferansenrFunctionalException(String message, Throwable cause) {
        super(message, cause);
    }

    public SkanmotreferansenrFunctionalException(Throwable cause) {
        super(cause);
    }

    protected SkanmotreferansenrFunctionalException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
