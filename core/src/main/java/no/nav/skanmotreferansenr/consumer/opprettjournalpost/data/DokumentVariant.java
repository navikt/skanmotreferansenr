package no.nav.skanmotreferansenr.consumer.opprettjournalpost.data;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DokumentVariant {
    @NotNull(message = "filtype kan ikke være null")
	String filtype;

    @NotNull(message = "fysiskDokument kan ikke være null")
	byte[] fysiskDokument;

    @NotNull(message = "variantformat kan ikke være null")
	String variantformat;

    @NotNull(message = "navn kan ikke være null")
	String filnavn;

    @NotNull(message = "batchnavn kan ikke være null")
	String batchnavn;
}
