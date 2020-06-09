package no.nav.skanmotreferansenr.opprettjournalpost;

import no.nav.skanmotreferansenr.domain.Filepair;
import no.nav.skanmotreferansenr.domain.Journalpost;
import no.nav.skanmotreferansenr.domain.SkanningInfo;
import no.nav.skanmotreferansenr.domain.Skanningmetadata;
import no.nav.skanmotreferansenr.foersteside.data.FoerstesideMetadata;
import no.nav.skanmotreferansenr.opprettjournalpost.data.AvsenderMottaker;
import no.nav.skanmotreferansenr.opprettjournalpost.data.Bruker;
import no.nav.skanmotreferansenr.opprettjournalpost.data.Dokument;
import no.nav.skanmotreferansenr.opprettjournalpost.data.DokumentVariant;
import no.nav.skanmotreferansenr.opprettjournalpost.data.OpprettJournalpostRequest;
import no.nav.skanmotreferansenr.opprettjournalpost.data.Tilleggsopplysning;
import no.nav.skanmotreferansenr.utils.Utils;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OpprettJournalpostRequestMapper {

    private static final String FILTYPE_PDFA = "PDFA";
    private static final String FILTYPE_XML = "XML";
    private static final String FIL_EXTENSION_PDF = "pdf";
    private static final String FIL_EXTENSION_XML = "xml";
    private static final String VARIANTFORMAT_PDF = "ARKIV";
    private static final String VARIANTFORMAT_XML = "SKANNING_META";
    private static final String DOKUMENTKATEGORI_IS = "IS";
    private static final String JOURNALPOSTTYPE = "INNGAAENDE";
    private static final String REFERANSENR = "referansenr";
    private static final String ENDORSERNR = "endorsernr";
    private static final String FYSISKPOSTBOKS = "fysiskPostboks";
    private static final String STREKKODEPOSTBOKS = "strekkodePostboks";
    private static final String BATCHNAVN = "batchNavn";

    private static final String PERSON = "PERSON";
    private static final String ORGANISASJON = "ORGANISASJON";
    private static final String FNR = "FNR";
    private static final String ORGNR = "ORGNR";
    private static final String TEMA_UKJENT = "UKJ";

    public OpprettJournalpostRequest mapMetadataToOpprettJournalpostRequest(Skanningmetadata skanningmetadata, FoerstesideMetadata foerstesideMetadata, Filepair filepair) {
        Journalpost journalpost = skanningmetadata.getJournalpost();
        SkanningInfo skanningInfo = skanningmetadata.getSkanningInfo();

        String tema = extractTema(foerstesideMetadata);
        String behandlingstema = foerstesideMetadata.getBehandlingstema();
        String tittel = foerstesideMetadata.getArkivtittel();
        String kanal = skanningmetadata.getJournalpost().getMottakskanal();
        String journalfoerendeEnhet = foerstesideMetadata.getEnhetsnummer();
        String eksternReferanseId = appendFileType(filepair.getName(), FIL_EXTENSION_PDF);
        Date datoMottatt = skanningmetadata.getJournalpost().getDatoMottatt();

        AvsenderMottaker avsenderMottaker = extractAvsenderMottaker(foerstesideMetadata);
        Bruker bruker = extractBruker(foerstesideMetadata);

        List<Tilleggsopplysning> tilleggsopplysninger = List.of(
                new Tilleggsopplysning(REFERANSENR, journalpost.getReferansenummer() + journalpost.getReferansenrChecksum()),
                new Tilleggsopplysning(ENDORSERNR, journalpost.getEndorsernr()),
                new Tilleggsopplysning(FYSISKPOSTBOKS, skanningInfo.getFysiskPostboks()),
                new Tilleggsopplysning(STREKKODEPOSTBOKS, skanningInfo.getStrekkodePostboks()),
                new Tilleggsopplysning(BATCHNAVN, journalpost.getBatchNavn())
        ).stream().filter(tilleggsopplysning -> tilleggsopplysning.getVerdi() != null).collect(Collectors.toList());

        DokumentVariant pdf = DokumentVariant.builder()
                .filtype(FILTYPE_PDFA)
                .variantformat(VARIANTFORMAT_PDF)
                .fysiskDokument(filepair.getPdf())
                .filnavn(appendFileType(filepair.getName(), FIL_EXTENSION_PDF))
                .build();

        DokumentVariant xml = DokumentVariant.builder()
                .filtype(FILTYPE_XML)
                .variantformat(VARIANTFORMAT_XML)
                .fysiskDokument(filepair.getXml())
                .filnavn(appendFileType(filepair.getName(), FIL_EXTENSION_XML))
                .build();

        Dokument dokument = Dokument.builder()
                .tittel(foerstesideMetadata.getArkivtittel())
                .brevkode(foerstesideMetadata.getNavSkjemaId())
                .dokumentKategori(DOKUMENTKATEGORI_IS)
                .dokumentVarianter(List.of(pdf, xml))
                .build();

        return OpprettJournalpostRequest.builder()
                .journalpostType(JOURNALPOSTTYPE)
                .avsenderMottaker(avsenderMottaker)
                .bruker(bruker)
                .tema(tema)
                .behandlingstema(behandlingstema)
                .tittel(tittel)
                .kanal(kanal)
                .journalfoerendeEnhet(journalfoerendeEnhet)
                .eksternReferanseId(eksternReferanseId)
                .datoMottatt(datoMottatt)
                .tilleggsopplysninger(tilleggsopplysninger)
                .dokumenter(List.of(dokument))
                .build();
    }

    private String extractTema(FoerstesideMetadata foerstesideMetadata) {
        if (foerstesideMetadata.getTema() == null) {
            return TEMA_UKJENT;
        }
        return foerstesideMetadata.getTema();
    }

    private AvsenderMottaker extractAvsenderMottaker(FoerstesideMetadata foerstesideMetadata) {
        if (foerstesideMetadata.getAvsender() == null) {
            return null;
        }
        return AvsenderMottaker.builder()
                .id(foerstesideMetadata.getAvsender().getAvsenderId())
                .idType(null == foerstesideMetadata.getAvsender().getAvsenderId() ? null : FNR)
                .navn(foerstesideMetadata.getAvsender().getAvsenderNavn())
                .build();
    }

    private Bruker extractBruker(FoerstesideMetadata foerstesideMetadata) {
        if (foerstesideMetadata.getBruker() == null) {
            return null;
        }
        String id = foerstesideMetadata.getBruker().getBrukerId();
        String idType = foerstesideMetadata.getBruker().getBrukerType();
        if (PERSON.equals(idType)) {
            idType = FNR;
        } else if (ORGANISASJON.equals(idType)) {
            idType = ORGNR;
        }
        return Bruker.builder()
                .id(id)
                .idType(idType)
                .build();
    }

    private boolean notNullOrEmpty(String string) {
        return string != null && !string.isBlank();
    }

    private static String appendFileType(String filename, String filetype) {
        return filename + "." + filetype;
    }
}
