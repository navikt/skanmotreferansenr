package no.nav.skanmotreferansenr.consumer.opprettjournalpost.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class Tilleggsopplysning {
    String nokkel;

    String verdi;
}
