package no.nav.skanmotreferansenr.consumer.journalpostapi.data;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class LeggTilLogiskVedleggRequest {

    @NotNull(message = "Tittel kan ikke være null")
	String tittel;

}
