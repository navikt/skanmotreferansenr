package no.nav.skanmotreferansenr.opprettjournalpost.data;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class Dokument {
    private String tittel;
    private String brevkode;
    private String dokumentKategori;
    private List<DokumentVariant> dokumentVarianter;
}


