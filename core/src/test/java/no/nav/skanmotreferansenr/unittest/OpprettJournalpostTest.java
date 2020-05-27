package no.nav.skanmotreferansenr.unittest;

import no.nav.skanmotreferansenr.domain.Filepair;
import no.nav.skanmotreferansenr.domain.Journalpost;
import no.nav.skanmotreferansenr.domain.SkanningInfo;
import no.nav.skanmotreferansenr.domain.Skanningmetadata;
import no.nav.skanmotreferansenr.foersteside.data.Avsender;
import no.nav.skanmotreferansenr.foersteside.data.Bruker;
import no.nav.skanmotreferansenr.foersteside.data.FoerstesideMetadata;
import no.nav.skanmotreferansenr.opprettjournalpost.data.Dokument;
import no.nav.skanmotreferansenr.opprettjournalpost.data.DokumentVariant;
import no.nav.skanmotreferansenr.opprettjournalpost.data.OpprettJournalpostRequest;
import no.nav.skanmotreferansenr.opprettjournalpost.data.Tilleggsopplysning;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static no.nav.skanmotreferansenr.opprettjournalpost.OpprettJournalpostRequestMapper.generateRequestBody;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class OpprettJournalpostTest {

    private final String MOTTAKSKANAL = "SKAN_IM";
    private final String BATCHNAVN = "navnPaaBatch.zip";
    private final String FILNAVN_PDF = "filnavn.pdf";
    private final String FILNAVN_XML = "filnavn.xml";
    private final String REFERANSENR = "1234567890123";
    private final String REFERANSENR_CHECKSUM = "4";
    private final String ENDORSERNR = "222111NAV456";
    private final String FYSISK_POSTBOKS = "1400";
    private final String STREKKODE_POSTBOKS = "1400";
    private final String JOURNALPOSTTYPE_INNGAAENDE = "INNGAAENDE";
    private final String TEMA = "mockTema";
    private final String NAV_SKJEMA_ID = "mockBrevKode";
    private final byte[] DUMMY_FILE = "dummyfile".getBytes();
    private final String BRUKER_ID = "12345678900";
    private final String BRUKER_IDTYPE = "PERSON";
    private final String ARKIVTITTEL = "mockArkivtittel";
    private final String AVSENDER_ID = "mockAvsenderID";
    private final String AVSENDER_NAVN = "mockAvsendernavn";
    private final String BEHANDLINGSTEMA = "mockBehandlingstema";
    private final String ENHETSNUMMER = "55544";

    @Test
    public void shouldExtractOpprettJournalpostRequestFromSkanningmetadata() {

        OpprettJournalpostRequest opprettJournalpostRequest = generateOpprettJournalpostRequest();

        assertEquals(JOURNALPOSTTYPE_INNGAAENDE, opprettJournalpostRequest.getJournalpostType());

        assertEquals(AVSENDER_ID, opprettJournalpostRequest.getAvsenderMottaker().getId());
        assertEquals("FNR", opprettJournalpostRequest.getAvsenderMottaker().getIdType());
        assertEquals(AVSENDER_NAVN, opprettJournalpostRequest.getAvsenderMottaker().getNavn());

        assertEquals(BRUKER_ID, opprettJournalpostRequest.getBruker().getId());
        assertEquals(BRUKER_IDTYPE, opprettJournalpostRequest.getBruker().getIdType());

        assertEquals(TEMA, opprettJournalpostRequest.getTema());
        assertEquals(BEHANDLINGSTEMA, opprettJournalpostRequest.getBehandlingstema());
        assertEquals(ARKIVTITTEL, opprettJournalpostRequest.getTittel());
        assertEquals(MOTTAKSKANAL, opprettJournalpostRequest.getKanal());
        assertEquals(ENHETSNUMMER, opprettJournalpostRequest.getJournalfoerendeEnhet());
        assertEquals(FILNAVN_PDF, opprettJournalpostRequest.getEksternReferanseId());

        assertEquals(REFERANSENR + REFERANSENR_CHECKSUM, getTillegsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "referansenr"));
        assertEquals(ENDORSERNR, getTillegsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "endorsernr"));
        assertEquals(FYSISK_POSTBOKS, getTillegsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "fysiskPostboks"));
        assertEquals(STREKKODE_POSTBOKS, getTillegsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "strekkodePostboks"));
        assertEquals(BATCHNAVN, getTillegsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "batchNavn"));

        assertEquals(1, opprettJournalpostRequest.getDokumenter().size());
        Dokument dokument = opprettJournalpostRequest.getDokumenter().iterator().next();
        assertEquals(ARKIVTITTEL, dokument.getTittel());
        assertEquals(NAV_SKJEMA_ID, dokument.getBrevkode());
        assertEquals("IS", dokument.getDokumentKategori());

        AtomicInteger pdfCounter = new AtomicInteger();
        AtomicInteger xmlCounter = new AtomicInteger();
        List<DokumentVariant> dokumentVarianter = opprettJournalpostRequest
                .getDokumenter()
                .iterator()
                .next()
                .getDokumentVarianter();

        dokumentVarianter.forEach(dokumentVariant -> {
            switch (dokumentVariant.getFiltype()) {
                case "PDFA":
                    pdfCounter.getAndIncrement();
                    assertEquals(FILNAVN_PDF, dokumentVariant.getFilnavn());
                    assertEquals("ARKIV", dokumentVariant.getVariantformat());
                    assertArrayEquals(DUMMY_FILE, dokumentVariant.getFysiskDokument());
                    break;
                case "XML":
                    xmlCounter.getAndIncrement();
                    assertEquals(FILNAVN_XML, dokumentVariant.getFilnavn());
                    assertEquals("SKANNING_META", dokumentVariant.getVariantformat());
                    assertArrayEquals(DUMMY_FILE, dokumentVariant.getFysiskDokument());
                    break;
                default:
                    fail();
            }
        });
        assertEquals(1, pdfCounter.get());
        assertEquals(1, xmlCounter.get());
    }

    private String getTillegsopplysningerVerdiFromNokkel(List<Tilleggsopplysning> tilleggsopplysninger, String nokkel) {
        return tilleggsopplysninger.stream().filter(pair -> nokkel.equals(pair.getNokkel())).findFirst().get().getVerdi();
    }

    private OpprettJournalpostRequest generateOpprettJournalpostRequest() {
        Skanningmetadata skanningmetadata = Skanningmetadata.builder()
                .journalpost(
                        Journalpost.builder()
                                .referansenummer(REFERANSENR)
                                .referansenrChecksum(REFERANSENR_CHECKSUM)
                                .datoMottatt(new Date())
                                .mottakskanal(MOTTAKSKANAL)
                                .batchNavn(BATCHNAVN)
                                .filNavn(FILNAVN_PDF)
                                .endorsernr(ENDORSERNR)
                                .build()
                )
                .skanningInfo(SkanningInfo.builder()
                        .fysiskPostboks(FYSISK_POSTBOKS)
                        .strekkodePostboks(STREKKODE_POSTBOKS)
                        .build())
                .build();

        Filepair filepair = Filepair.builder()
                .pdf(DUMMY_FILE)
                .xml(DUMMY_FILE)
                .build();

        FoerstesideMetadata foerstesideResponse = FoerstesideMetadata.builder()
                .arkivtittel(ARKIVTITTEL)
                .avsender(Avsender.builder()
                        .avsenderId(AVSENDER_ID)
                        .avsenderNavn(AVSENDER_NAVN)
                        .build())
                .behandlingstema(BEHANDLINGSTEMA)
                .bruker(Bruker.builder().brukerId(BRUKER_ID).brukerType(Bruker.BrukerType.PERSON).build())
                .enhetsnummer(ENHETSNUMMER)
                .navSkjemaId(NAV_SKJEMA_ID)
                .tema(TEMA)
                .build();

        return generateRequestBody(skanningmetadata, foerstesideResponse, filepair);
    }

}
