package no.nav.skanmotreferansenr.foersteside.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FoerstesideMetadata {

    private String arkivtittel;
    private Avsender avsender;
    private String behandlingstema;
    private Bruker bruker;
    private String enhetsnummer;
    private String navSkjemaId;
    private String tema;
    private List<String> vedleggsliste;

}
