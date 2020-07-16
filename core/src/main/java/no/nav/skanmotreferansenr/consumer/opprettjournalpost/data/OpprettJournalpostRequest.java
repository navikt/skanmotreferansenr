package no.nav.skanmotreferansenr.consumer.opprettjournalpost.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Value
@Builder
@AllArgsConstructor
public class OpprettJournalpostRequest {
    private String tittel;

    private AvsenderMottaker avsenderMottaker;

    @NotNull(message = "Mottakskanal kan ikke være null")
    private String journalpostType;

    private String tema;

    private String behandlingstema;

    private String kanal;

    private Date datoMottatt;

    private String journalfoerendeEnhet;

    private String eksternReferanseId;

    private List<Tilleggsopplysning> tilleggsopplysninger;

    private Bruker bruker;

    @NotNull(message = "dokumenter kan ikke være null")
    private List<Dokument> dokumenter;
}
