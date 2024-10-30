package no.nav.skanmotreferansenr.consumer.logiskvedlegg.data;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class LeggTilLogiskVedleggResponse {

    @NotNull
	String logiskVedleggId;
}
