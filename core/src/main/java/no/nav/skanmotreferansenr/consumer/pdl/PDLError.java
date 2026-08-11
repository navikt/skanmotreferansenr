package no.nav.skanmotreferansenr.consumer.pdl;

public record PDLError(
	String message,
	ErrorExtensions extensions
) {
	public record ErrorExtensions(
		String code,
		ErrorDetails details,
		String classification) {
	}

	public record ErrorDetails(
		String type,
		String cause,
		String policy
	) {
	}
}
