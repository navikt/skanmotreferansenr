package no.nav.skanmotreferansenr.consumer.logiskvedlegg.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import jakarta.validation.constraints.NotNull;

@Value
@Builder
@AllArgsConstructor
public class LeggTilLogiskVedleggRequest {

    @NotNull(message = "Tittel kan ikke være null")
    private String tittel;

}
