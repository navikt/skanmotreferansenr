package no.nav.skanmotreferansenr.opprettjournalpost.data;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class DokumentVariant {
    @NotNull(message = "filtype kan ikke være null")
    private String filtype;

    @NotNull(message = "fysiskDokument kan ikke være null")
    private byte[] fysiskDokument;

    @NotNull(message = "variantformat kan ikke være null")
    private String variantformat;

    @NotNull(message = "navn kan ikke være null")
    private String filnavn;
}
