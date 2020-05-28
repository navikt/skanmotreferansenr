package no.nav.skanmotreferansenr.opprettjournalpost.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AvsenderMottaker {
    private String id;
    private String idType;
    private String navn;
    private String land;
}
