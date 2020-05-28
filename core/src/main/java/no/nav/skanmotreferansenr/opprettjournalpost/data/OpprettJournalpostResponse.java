package no.nav.skanmotreferansenr.opprettjournalpost.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OpprettJournalpostResponse {
    private String journalpostId;
}
