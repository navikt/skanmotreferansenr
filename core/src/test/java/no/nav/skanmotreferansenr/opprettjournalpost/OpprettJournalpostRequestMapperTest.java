package no.nav.skanmotreferansenr.opprettjournalpost;

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
import no.nav.skanmotreferansenr.validators.SkanningMetadataValidator;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class OpprettJournalpostRequestMapperTest {

    private final OpprettJournalpostRequestMapper opprettJournalpostRequestMapper = new OpprettJournalpostRequestMapper();

    private final String MOTTAKSKANAL = "SKAN_IM";
    private final String BATCHNAVN = "navnPaaBatch.zip";
    private final String FILNAVN_I_XML = "ABC.PDF";
    private final String FILNAVN = "filnavn";
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
    private final String BRUKER_ID_INVALID = "123";
    private final String BRUKER_IDTYPE = "FNR";
    private final String ARKIVTITTEL = "mockArkivtittel";
    private final String AVSENDER_ID = "mockAvsenderID";
    private final String AVSENDER_NAVN = "mockAvsendernavn";
    private final String BEHANDLINGSTEMA = "mockBehandlingstema";
    private final String ENHETSNUMMER = "55544";

    @Test
    public void shouldExtractOpprettJournalpostRequestFromSkanningmetadata() {

        OpprettJournalpostRequest opprettJournalpostRequest = opprettJournalpostRequestMapper.mapMetadataToOpprettJournalpostRequest(
                generateSkanningMetadata(),
                generateFoerstesideMetadata(),
                generateFilepair()
        );

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

    @Test
    public void shouldExtractEvenWhenNoFoerstesideMetadata() {
        OpprettJournalpostRequest opprettJournalpostRequest = opprettJournalpostRequestMapper.mapMetadataToOpprettJournalpostRequest(
                generateSkanningMetadata(),
                new FoerstesideMetadata(),
                generateFilepair()
        );

        assertEquals(JOURNALPOSTTYPE_INNGAAENDE, opprettJournalpostRequest.getJournalpostType());

        assertNull(opprettJournalpostRequest.getAvsenderMottaker());

        assertNull(opprettJournalpostRequest.getBruker());

        assertEquals("UKJ", opprettJournalpostRequest.getTema());
        assertNull(opprettJournalpostRequest.getBehandlingstema());
        assertNull(opprettJournalpostRequest.getTittel());
        assertEquals(MOTTAKSKANAL, opprettJournalpostRequest.getKanal());
        assertNull(opprettJournalpostRequest.getJournalfoerendeEnhet());
        assertEquals(FILNAVN_PDF, opprettJournalpostRequest.getEksternReferanseId());

        assertEquals(REFERANSENR + REFERANSENR_CHECKSUM, getTillegsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "referansenr"));
        assertEquals(ENDORSERNR, getTillegsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "endorsernr"));
        assertEquals(FYSISK_POSTBOKS, getTillegsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "fysiskPostboks"));
        assertEquals(STREKKODE_POSTBOKS, getTillegsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "strekkodePostboks"));
        assertEquals(BATCHNAVN, getTillegsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "batchNavn"));

        assertEquals(1, opprettJournalpostRequest.getDokumenter().size());
        Dokument dokument = opprettJournalpostRequest.getDokumenter().iterator().next();
        assertNull(dokument.getTittel());
        assertNull(dokument.getBrevkode());
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

    @Test
    public void shouldFilterOutMissingTilleggsopplysninger() {
        Skanningmetadata skanningmetadataNoEndorsernrOrFysiskPostboks = Skanningmetadata.builder()
                .journalpost(
                        Journalpost.builder()
                                .referansenummer(REFERANSENR)
                                .referansenrChecksum(REFERANSENR_CHECKSUM)
                                .datoMottatt(new Date())
                                .mottakskanal(MOTTAKSKANAL)
                                .batchNavn(BATCHNAVN)
                                .filNavn(FILNAVN_I_XML)
                                .endorsernr(null)
                                .build()
                )
                .skanningInfo(SkanningInfo.builder()
                        .fysiskPostboks("")
                        .strekkodePostboks(STREKKODE_POSTBOKS)
                        .build())
                .build();
        OpprettJournalpostRequest opprettJournalpostRequest = opprettJournalpostRequestMapper.mapMetadataToOpprettJournalpostRequest(
                skanningmetadataNoEndorsernrOrFysiskPostboks,
                generateFoerstesideMetadata(),
                generateFilepair()
        );

        assertEquals(3, opprettJournalpostRequest.getTilleggsopplysninger().size());
        assertEquals(REFERANSENR + REFERANSENR_CHECKSUM, getTillegsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "referansenr"));
        assertEquals(STREKKODE_POSTBOKS, getTillegsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "strekkodePostboks"));
        assertEquals(BATCHNAVN, getTillegsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "batchNavn"));
        assertThrows(NoSuchElementException.class, () -> getTillegsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "endorsernr"));
        assertThrows(NoSuchElementException.class, () -> getTillegsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "fysiskPostboks"));
    }

    private String getTillegsopplysningerVerdiFromNokkel(List<Tilleggsopplysning> tilleggsopplysninger, String nokkel) {
        return tilleggsopplysninger.stream().filter(pair -> nokkel.equals(pair.getNokkel())).findFirst().get().getVerdi();
    }

    private FoerstesideMetadata generateFoerstesideMetadata() {
        return FoerstesideMetadata.builder()
                .arkivtittel(ARKIVTITTEL)
                .avsender(Avsender.builder()
                        .avsenderId(AVSENDER_ID)
                        .avsenderNavn(AVSENDER_NAVN)
                        .build())
                .behandlingstema(BEHANDLINGSTEMA)
                .bruker(Bruker.builder()
                        .brukerId(BRUKER_ID)
                        .brukerType("PERSON")
                        .build())
                .enhetsnummer(ENHETSNUMMER)
                .navSkjemaId(NAV_SKJEMA_ID)
                .tema(TEMA)
                .build();
    }

    private Skanningmetadata generateSkanningMetadata() {
        return Skanningmetadata.builder()
                .journalpost(
                        Journalpost.builder()
                                .referansenummer(REFERANSENR)
                                .referansenrChecksum(REFERANSENR_CHECKSUM)
                                .datoMottatt(new Date())
                                .mottakskanal(MOTTAKSKANAL)
                                .batchNavn(BATCHNAVN)
                                .filNavn(FILNAVN_I_XML)
                                .endorsernr(ENDORSERNR)
                                .build()
                )
                .skanningInfo(SkanningInfo.builder()
                        .fysiskPostboks(FYSISK_POSTBOKS)
                        .strekkodePostboks(STREKKODE_POSTBOKS)
                        .build())
                .build();
    }

    private Filepair generateFilepair() {
        return Filepair.builder()
                .name(FILNAVN)
                .pdf(DUMMY_FILE)
                .xml(DUMMY_FILE)
                .build();
    }
}
