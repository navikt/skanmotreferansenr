package no.nav.skanmotreferansenr.itest;

import no.nav.skanmotreferansenr.consumer.journalpostapi.JournalpostConsumer;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.Dokument;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.DokumentVariant;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.OpprettJournalpostRequest;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.OpprettJournalpostResponse;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.Tilleggsopplysning;
import no.nav.skanmotreferansenr.exceptions.functional.SkanmotreferansenrFunctionalException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OpprettJournalpostIT extends AbstractItest {

	private final byte[] DUMMY_FILE = "dummyfile".getBytes();

	@Autowired
	private JournalpostConsumer journalpostConsumer;

	@BeforeEach
	void setUpConsumer() {
		stubAzureToken();
		super.setUpStubs();
	}

	@Test
	public void shouldOpprettJournalpost() {
		OpprettJournalpostRequest request = createOpprettJournalpostRequest();
		OpprettJournalpostResponse res = journalpostConsumer.opprettJournalpost(request);
		String JOURNALPOST_ID = "467010363";
		assertEquals(JOURNALPOST_ID, res.getJournalpostId());
		assertEquals(1, res.getDokumenter().size());
		String DOKUMENT_INFO_ID = "485227498";
		assertEquals(DOKUMENT_INFO_ID, res.getDokumenter().getFirst().dokumentInfoId());
	}


	@Test
	public void shouldGetJournalpostWhenResponseIs() {
		this.stubOpprettJournalpostResponseConflictWithValidResponse();
		OpprettJournalpostRequest request = OpprettJournalpostRequest.builder()
				.eksternReferanseId("ekstern")
				.build();

		SkanmotreferansenrFunctionalException exception = assertThrows(SkanmotreferansenrFunctionalException.class, () -> journalpostConsumer.opprettJournalpost(request));
		Assertions.assertThat(exception.getMessage()).contains("Det eksisterer allerede en journalpost i dokarkiv med journalpostId=567010363.");
	}

	@Test
	public void shouldNotGetJournalpostWhenConflictDoesNotCorrectHaveBody() {
		this.stubOpprettJournalpostResponseConflictWithInvalidResponse();

		assertThrows(
				IllegalArgumentException.class,
				() -> journalpostConsumer.opprettJournalpost(null)
		);
	}

	private OpprettJournalpostRequest createOpprettJournalpostRequest() {
		List<Tilleggsopplysning> tilleggsopplysninger = List.of(
				new Tilleggsopplysning("fysiskPostboks", "1400"),
				new Tilleggsopplysning("strekkodePostboks", "1400"),
				new Tilleggsopplysning("endorsernr", "3110190003NAV743506"),
				new Tilleggsopplysning("antallSider", "10")
		);

		DokumentVariant pdf = DokumentVariant.builder()
				.filtype("pdf")
				.variantformat("ARKIV")
				.fysiskDokument(DUMMY_FILE)
				.filnavn("dummy.pdf")
				.batchnavn("xml_pdf_pairs_testdata.zip")
				.build();

		DokumentVariant xml = DokumentVariant.builder()
				.filtype("xml")
				.variantformat("ORIGINAL")
				.fysiskDokument(DUMMY_FILE)
				.filnavn("dummy.xml")
				.batchnavn("xml_pdf_pairs_testdata.zip")
				.build();

		List<Dokument> dokumenter = List.of(
				Dokument.builder()
						.dokumentVarianter(List.of(pdf, xml))
						.build()
		);

		return OpprettJournalpostRequest.builder()
				.tilleggsopplysninger(tilleggsopplysninger)
				.dokumenter(dokumenter)
				.build();
	}
}
