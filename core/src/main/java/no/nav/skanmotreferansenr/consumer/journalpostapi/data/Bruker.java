package no.nav.skanmotreferansenr.consumer.journalpostapi.data;

import jakarta.validation.constraints.NotNull;

public record Bruker(
		@NotNull(message = "id kan ikke være null")
		String id,
		@NotNull(message = "idType kan ikke være null")
		String idType) {
}
