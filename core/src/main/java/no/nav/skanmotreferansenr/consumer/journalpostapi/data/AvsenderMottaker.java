package no.nav.skanmotreferansenr.consumer.journalpostapi.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AvsenderMottaker {
    String id;
    String idType;
    String navn;
    String land;
}
