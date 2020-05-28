package no.nav.skanmotreferansenr.foersteside.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Bruker {

    private String brukerId;
    private String brukerType;

}
