package no.nav.skanmotreferansenr.mapper;

import no.nav.skanmotreferansenr.consumer.foersteside.data.Avsender;
import no.nav.skanmotreferansenr.consumer.foersteside.data.Bruker;
import no.nav.skanmotreferansenr.consumer.foersteside.data.FoerstesideMetadata;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.AvsenderMottaker;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.Dokument;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.DokumentVariant;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.OpprettJournalpostRequest;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.Tilleggsopplysning;
import no.nav.skanmotreferansenr.domain.Filepair;
import no.nav.skanmotreferansenr.domain.Journalpost;
import no.nav.skanmotreferansenr.domain.SkanningInfo;
import no.nav.skanmotreferansenr.domain.Skanningmetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import static no.nav.skanmotreferansenr.mapper.OpprettJournalpostRequestMapper.AVSENDER_IDTYPE_ORGANISASJON;
import static no.nav.skanmotreferansenr.mapper.OpprettJournalpostRequestMapper.AVSENDER_IDTYPE_PERSON;
import static no.nav.skanmotreferansenr.mapper.OpprettJournalpostRequestMapper.BRUKER_IDTYPE_PERSON;
import static no.nav.skanmotreferansenr.mapper.OpprettJournalpostRequestMapper.DOUBLE_ZERO_PADDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class OpprettJournalpostRequestMapperTest {

	private final OpprettJournalpostRequestMapper opprettJournalpostRequestMapper = new OpprettJournalpostRequestMapper();

	private final String MOTTAKSKANAL = "SKAN_IM";
	private final String BATCHNAVN = "navnPaaBatch.zip";
	private final String FILNAVN_I_XML = "ABC.PDF";
	private final String FILNAVN = "filnavn";
	private final String FILNAVN_PDF = "filnavn.pdf";
	private final String FILNAVN_XML = "filnavn.xml";
	private final String REFERANSENR = "12345678901234";
	private final String ENDORSERNR = "222111NAV456";
	private final String ANTALL_SIDER = "10";
	private final String FYSISK_POSTBOKS = "1400";
	private final String STREKKODE_POSTBOKS = "1400";
	private final String JOURNALPOSTTYPE_INNGAAENDE = "INNGAAENDE";
	private final String TEMA = "mockTema";
	private final String NAV_SKJEMA_ID = "mockBrevKode";
	private final byte[] DUMMY_FILE = "dummyfile".getBytes();
	private final String BRUKER_ID = "12345678900";
	private final String ARKIVTITTEL = "mockArkivtittel";
	private final String AVSENDER_ID_PERSON = "12345678901";
	private final String AVSENDER_ID_ORGANISASJON = "889640782";
	private final String AVSENDER_NAVN_PERSON = "Bjarne Betjent";
	private final String AVSENDER_NAVN_ORGANISASJON = "Arbeids- og velferdsetaten";
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

		assertEquals(AVSENDER_ID_PERSON, opprettJournalpostRequest.getAvsenderMottaker().getId());
		assertEquals(AVSENDER_IDTYPE_PERSON, opprettJournalpostRequest.getAvsenderMottaker().getIdType());
		assertEquals(AVSENDER_NAVN_PERSON, opprettJournalpostRequest.getAvsenderMottaker().getNavn());

		assertEquals(BRUKER_ID, opprettJournalpostRequest.getBruker().id());
		assertEquals(BRUKER_IDTYPE_PERSON, opprettJournalpostRequest.getBruker().idType());

		assertEquals(TEMA, opprettJournalpostRequest.getTema());
		assertEquals(BEHANDLINGSTEMA, opprettJournalpostRequest.getBehandlingstema());
		assertEquals(ARKIVTITTEL, opprettJournalpostRequest.getTittel());
		assertEquals(MOTTAKSKANAL, opprettJournalpostRequest.getKanal());
		assertEquals(ENHETSNUMMER, opprettJournalpostRequest.getJournalfoerendeEnhet());
		assertEquals(FILNAVN_PDF, opprettJournalpostRequest.getEksternReferanseId());

		assertEquals(REFERANSENR, getTilleggsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "referansenr"));
		assertEquals(ENDORSERNR, getTilleggsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "endorsernr"));
		assertEquals(FYSISK_POSTBOKS, getTilleggsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "fysiskPostboks"));
		assertEquals(STREKKODE_POSTBOKS, getTilleggsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "strekkodePostboks"));
		assertEquals(ANTALL_SIDER, getTilleggsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "antallSider"));


		assertEquals(1, opprettJournalpostRequest.getDokumenter().size());
		Dokument dokument = opprettJournalpostRequest.getDokumenter().getFirst();
		assertEquals(ARKIVTITTEL, dokument.getTittel());
		assertEquals(NAV_SKJEMA_ID, dokument.getBrevkode());
		assertEquals("IS", dokument.getDokumentKategori());

		AtomicInteger pdfCounter = new AtomicInteger();
		AtomicInteger xmlCounter = new AtomicInteger();
		List<DokumentVariant> dokumentVarianter = opprettJournalpostRequest
				.getDokumenter()
				.getFirst()
				.getDokumentVarianter();

		dokumentVarianter.forEach(dokumentVariant -> {
			switch (dokumentVariant.getFiltype()) {
				case "PDFA":
					pdfCounter.getAndIncrement();
					assertEquals(FILNAVN_PDF, dokumentVariant.getFilnavn());
					assertEquals("ARKIV", dokumentVariant.getVariantformat());
					assertEquals(BATCHNAVN, dokumentVariant.getBatchnavn());
					assertArrayEquals(DUMMY_FILE, dokumentVariant.getFysiskDokument());
					break;
				case "XML":
					xmlCounter.getAndIncrement();
					assertEquals(FILNAVN_XML, dokumentVariant.getFilnavn());
					assertEquals("SKANNING_META", dokumentVariant.getVariantformat());
					assertEquals(BATCHNAVN, dokumentVariant.getBatchnavn());
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

		assertEquals(REFERANSENR, getTilleggsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "referansenr"));
		assertEquals(ENDORSERNR, getTilleggsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "endorsernr"));
		assertEquals(FYSISK_POSTBOKS, getTilleggsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "fysiskPostboks"));
		assertEquals(STREKKODE_POSTBOKS, getTilleggsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "strekkodePostboks"));
		assertEquals(ANTALL_SIDER, getTilleggsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "antallSider"));

		assertEquals(1, opprettJournalpostRequest.getDokumenter().size());
		Dokument dokument = opprettJournalpostRequest.getDokumenter().getFirst();
		assertNull(dokument.getTittel());
		assertNull(dokument.getBrevkode());
		assertEquals("IS", dokument.getDokumentKategori());

		AtomicInteger pdfCounter = new AtomicInteger();
		AtomicInteger xmlCounter = new AtomicInteger();
		List<DokumentVariant> dokumentVarianter = opprettJournalpostRequest
				.getDokumenter()
				.getFirst()
				.getDokumentVarianter();

		dokumentVarianter.forEach(dokumentVariant -> {
			switch (dokumentVariant.getFiltype()) {
				case "PDFA":
					pdfCounter.getAndIncrement();
					assertEquals(FILNAVN_PDF, dokumentVariant.getFilnavn());
					assertEquals("ARKIV", dokumentVariant.getVariantformat());
					assertEquals(BATCHNAVN, dokumentVariant.getBatchnavn());
					assertArrayEquals(DUMMY_FILE, dokumentVariant.getFysiskDokument());
					break;
				case "XML":
					xmlCounter.getAndIncrement();
					assertEquals(FILNAVN_XML, dokumentVariant.getFilnavn());
					assertEquals("SKANNING_META", dokumentVariant.getVariantformat());
					assertEquals(BATCHNAVN, dokumentVariant.getBatchnavn());
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
								.datoMottatt(new Date())
								.mottakskanal(MOTTAKSKANAL)
								.batchnavn(BATCHNAVN)
								.filnavn(FILNAVN_I_XML)
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

		assertEquals(2, opprettJournalpostRequest.getTilleggsopplysninger().size());
		assertEquals(REFERANSENR, getTilleggsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "referansenr"));
		assertEquals(STREKKODE_POSTBOKS, getTilleggsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "strekkodePostboks"));
		assertThrows(NoSuchElementException.class, () -> getTilleggsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "endorsernr"));
		assertThrows(NoSuchElementException.class, () -> getTilleggsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "fysiskPostboks"));
	}

	@ParameterizedTest
	@ValueSource(strings = {AVSENDER_ID_ORGANISASJON, DOUBLE_ZERO_PADDING + AVSENDER_ID_ORGANISASJON})
	void shouldMapAvsenderMottakerIdTypeOrgnr(String orgnr) {
		OpprettJournalpostRequest opprettJournalpostRequest = opprettJournalpostRequestMapper.mapMetadataToOpprettJournalpostRequest(
				generateSkanningMetadata(),
				baseFoerstesideMetadataBuilder()
						.avsender(Avsender.builder()
								.avsenderId(orgnr)
								.avsenderNavn(AVSENDER_NAVN_ORGANISASJON).build())
						.build(),
				generateFilepair()
		);

		AvsenderMottaker avsenderMottaker = opprettJournalpostRequest.getAvsenderMottaker();
		assertThat(avsenderMottaker.getId()).isEqualTo(AVSENDER_ID_ORGANISASJON);
		assertThat(avsenderMottaker.getIdType()).isEqualTo(AVSENDER_IDTYPE_ORGANISASJON);
		assertThat(avsenderMottaker.getNavn()).isEqualTo(AVSENDER_NAVN_ORGANISASJON);
	}

	@ParameterizedTest
	@ValueSource(strings = {"00abcdefghj", "01234567890a"})
	void shouldMapAvsenderMottakerIdTypeNullWhenAvsenderIdNotNumeric(String avsenderId) {
		OpprettJournalpostRequest opprettJournalpostRequest = opprettJournalpostRequestMapper.mapMetadataToOpprettJournalpostRequest(
				generateSkanningMetadata(),
				baseFoerstesideMetadataBuilder()
						.avsender(Avsender.builder()
								.avsenderId(avsenderId)
								.avsenderNavn("Noe annet").build())
						.build(),
				generateFilepair()
		);

		AvsenderMottaker avsenderMottaker = opprettJournalpostRequest.getAvsenderMottaker();
		assertThat(avsenderMottaker.getId()).isEqualTo(avsenderId);
		assertThat(avsenderMottaker.getIdType()).isNull();
		assertThat(avsenderMottaker.getNavn()).isEqualTo("Noe annet");
	}

	private String getTilleggsopplysningerVerdiFromNokkel(List<Tilleggsopplysning> tilleggsopplysninger, String nokkel) {
		return tilleggsopplysninger.stream().filter(pair -> nokkel.equals(pair.getNokkel())).findFirst().get().getVerdi();
	}

	private FoerstesideMetadata generateFoerstesideMetadata() {
		return baseFoerstesideMetadataBuilder().build();
	}


	private FoerstesideMetadata.FoerstesideMetadataBuilder baseFoerstesideMetadataBuilder() {
		return FoerstesideMetadata.builder()
				.arkivtittel(ARKIVTITTEL)
				.avsender(Avsender.builder()
						.avsenderId(AVSENDER_ID_PERSON)
						.avsenderNavn(AVSENDER_NAVN_PERSON)
						.build())
				.behandlingstema(BEHANDLINGSTEMA)
				.bruker(Bruker.builder()
						.brukerId(BRUKER_ID)
						.brukerType("PERSON")
						.build())
				.enhetsnummer(ENHETSNUMMER)
				.navSkjemaId(NAV_SKJEMA_ID)
				.tema(TEMA);
	}

	private Skanningmetadata generateSkanningMetadata() {
		return Skanningmetadata.builder()
				.journalpost(
						Journalpost.builder()
								.referansenummer(REFERANSENR)
								.datoMottatt(new Date())
								.mottakskanal(MOTTAKSKANAL)
								.batchnavn(BATCHNAVN)
								.filnavn(FILNAVN_I_XML)
								.endorsernr(ENDORSERNR)
								.antallSider(ANTALL_SIDER)
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
