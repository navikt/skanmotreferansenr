package no.nav.skanmotreferansenr.consumer.pdl;

import no.nav.skanmotreferansenr.exceptions.functional.SkanmotreferansenrFunctionalException;

public class PdlFunctionalException extends SkanmotreferansenrFunctionalException {
	public PdlFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
