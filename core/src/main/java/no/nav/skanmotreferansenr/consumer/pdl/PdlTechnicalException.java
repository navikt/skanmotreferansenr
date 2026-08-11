package no.nav.skanmotreferansenr.consumer.pdl;

import no.nav.skanmotreferansenr.exceptions.technical.SkanmotreferansenrTechnicalException;

public class PdlTechnicalException extends SkanmotreferansenrTechnicalException {
	public PdlTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
}
