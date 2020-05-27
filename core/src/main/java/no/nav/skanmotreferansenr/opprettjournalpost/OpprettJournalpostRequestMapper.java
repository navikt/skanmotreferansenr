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

import java.util.List;
import java.util.stream.Collectors;

public class OpprettJournalpostRequestMapper {

    private static final String PDFA = "PDFA";
    private static final String XML = "XML";
    private static final String VARIANTFORMAT_PDF = "ARKIV";
    private static final String VARIANTFORMAT_XML = "SKANNING_META";
    private static final String FILTYPE_XML = "xml";
    private static final String DOKUMENTKATEGORI_IS = "IS";
    private static final String JOURNALPOSTTYPE = "INNGAAENDE";
    private static final String REFERANSENR = "referansenr";
    private static final String ENDORSERNR = "endorsernr";
    private static final String FYSISKPOSTBOKS = "fysiskPostboks";
    private static final String STREKKODEPOSTBOKS = "strekkodePostboks";
    private static final String BATCHNAVN = "batchNavn";

    private static final String FNR = "FNR";
    private static final String TEMA_UKJENT = "UKJ";

    public static OpprettJournalpostRequest generateRequestBody(Skanningmetadata skanningmetadata, FoerstesideMetadata foerstesideMetadata, Filepair filepair) {
        Journalpost journalpost = skanningmetadata.getJournalpost();
        SkanningInfo skanningInfo = skanningmetadata.getSkanningInfo();

        String tema = null == foerstesideMetadata.getTema() ? TEMA_UKJENT : foerstesideMetadata.getTema();
        String behandlingstema = foerstesideMetadata.getBehandlingstema();
        String tittel = foerstesideMetadata.getArkivtittel();
        String kanal = skanningmetadata.getJournalpost().getMottakskanal();
        String journalfoerendeEnhet = foerstesideMetadata.getEnhetsnummer();
        String eksternReferanseId = skanningmetadata.getJournalpost().getFilNavn();
        String datoMottatt = skanningmetadata.getJournalpost().getDatoMottatt().toString();

        AvsenderMottaker avsenderMottaker = extractAvsenderMottaker(foerstesideMetadata);

        Bruker bruker = Bruker.builder()
                .id(foerstesideMetadata.getBruker().getBrukerId())
                .idType(foerstesideMetadata.getBruker().getBrukerType().name())
                .build();

        List<Tilleggsopplysning> tilleggsopplysninger = List.of(
                new Tilleggsopplysning(REFERANSENR, journalpost.getReferansenummer() + journalpost.getReferansenrChecksum()),
                new Tilleggsopplysning(ENDORSERNR, journalpost.getEndorsernr()),
                new Tilleggsopplysning(FYSISKPOSTBOKS, skanningInfo.getFysiskPostboks()),
                new Tilleggsopplysning(STREKKODEPOSTBOKS, skanningInfo.getStrekkodePostboks()),
                new Tilleggsopplysning(BATCHNAVN, journalpost.getBatchNavn())
        ).stream().filter(tilleggsopplysning -> tilleggsopplysning.getVerdi() != null).collect(Collectors.toList());

        DokumentVariant pdf = DokumentVariant.builder()
                .filtype(PDFA)
                .variantformat(VARIANTFORMAT_PDF)
                .fysiskDokument(filepair.getPdf())
                .filnavn(journalpost.getFilNavn())
                .build();

        DokumentVariant xml = DokumentVariant.builder()
                .filtype(XML)
                .variantformat(VARIANTFORMAT_XML)
                .fysiskDokument(filepair.getXml())
                .filnavn(Utils.changeFiletypeInFilename(journalpost.getFilNavn(), FILTYPE_XML))
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

    private static AvsenderMottaker extractAvsenderMottaker(FoerstesideMetadata foerstesideMetadata) {
        if (foerstesideMetadata.getAvsender() == null) {
            return AvsenderMottaker.builder().build();
        }
        return AvsenderMottaker.builder()
                .id(foerstesideMetadata.getAvsender().getAvsenderId())
                .idType(null == foerstesideMetadata.getAvsender().getAvsenderId() ? null : FNR)
                .navn(foerstesideMetadata.getAvsender().getAvsenderNavn())
                .build();
    }

    private static boolean notNullOrEmpty(String string) {
        return string != null && !string.isBlank();
    }
}
