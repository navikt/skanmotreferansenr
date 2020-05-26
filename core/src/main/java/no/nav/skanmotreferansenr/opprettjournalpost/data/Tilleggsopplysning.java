package no.nav.skanmotreferansenr.opprettjournalpost.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class Tilleggsopplysning {
    private String nokkel;

    private String verdi;
}
