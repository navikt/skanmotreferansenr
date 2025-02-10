package no.nav.skanmotreferansenr.consumer.journalpostapi.data;

import jakarta.validation.constraints.NotNull;

public record DokumentInfo(@NotNull String dokumentInfoId) {
}
