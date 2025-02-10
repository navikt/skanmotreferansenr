package no.nav.skanmotreferansenr.consumer.journalpostapi.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OpprettJournalpostResponse {

    @NotNull
    private String journalpostId;

    private List<DokumentInfo> dokumenter;
}
