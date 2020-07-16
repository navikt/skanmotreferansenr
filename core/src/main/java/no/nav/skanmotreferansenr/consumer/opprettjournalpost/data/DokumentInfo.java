package no.nav.skanmotreferansenr.consumer.opprettjournalpost.data;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class DokumentInfo {

    @NotNull
    private String dokumentInfoId;
}
