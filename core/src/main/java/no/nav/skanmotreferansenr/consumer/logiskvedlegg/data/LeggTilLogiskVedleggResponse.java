package no.nav.skanmotreferansenr.consumer.logiskvedlegg.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
@AllArgsConstructor
public class LeggTilLogiskVedleggResponse {

    @NotNull
    private String logiskVedleggId;
}
