package no.nav.skanmotreferansenr.consumer.opprettjournalpost.data;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class Bruker {
    @NotNull(message = "id kan ikke være null")
    private String id;
    @NotNull(message = "idType kan ikke være null")
    private String idType;
}
