package no.nav.skanmotreferansenr.foersteside.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Avsender {

    private String avsenderId;
    private String avsenderNavn;
}
