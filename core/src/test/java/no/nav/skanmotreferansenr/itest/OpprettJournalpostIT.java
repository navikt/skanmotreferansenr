package no.nav.skanmotreferansenr.itest;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.skanmotreferansenr.config.props.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.consumer.opprettjournalpost.OpprettJournalpostConsumer;
import no.nav.skanmotreferansenr.consumer.opprettjournalpost.data.Dokument;
import no.nav.skanmotreferansenr.consumer.opprettjournalpost.data.DokumentVariant;
import no.nav.skanmotreferansenr.consumer.opprettjournalpost.data.OpprettJournalpostRequest;
import no.nav.skanmotreferansenr.consumer.opprettjournalpost.data.OpprettJournalpostResponse;
import no.nav.skanmotreferansenr.consumer.opprettjournalpost.data.Tilleggsopplysning;
import no.nav.skanmotreferansenr.consumer.sts.STSConsumer;
import no.nav.skanmotreferansenr.consumer.sts.data.STSResponse;
import no.nav.skanmotreferansenr.exceptions.functional.OpprettJournalpostFunctionalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OpprettJournalpostIT extends AbstractItest {

	private final byte[] DUMMY_FILE = "dummyfile".getBytes();

	private OpprettJournalpostConsumer opprettJournalpostConsumer;
	private STSConsumer stsConsumer;

	@Autowired
	private SkanmotreferansenrProperties properties;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUpConsumer() {
		super.setUpStubs();
		stsConsumer = new STSConsumer(new RestTemplateBuilder(), properties);
		opprettJournalpostConsumer = new OpprettJournalpostConsumer(new RestTemplateBuilder(), properties, objectMapper);
	}

	@Test
	public void shouldOpprettJournalpost() {
		OpprettJournalpostRequest request = createOpprettJournalpostRequest();
		STSResponse stsResponse = stsConsumer.getSTSToken();
		OpprettJournalpostResponse res = opprettJournalpostConsumer.opprettJournalpost(stsResponse.getAccess_token(), request);
		String JOURNALPOST_ID = "467010363";
		assertEquals(JOURNALPOST_ID, res.getJournalpostId());
		assertEquals(1, res.getDokumenter().size());
		String DOKUMENT_INFO_ID = "485227498";
		assertEquals(DOKUMENT_INFO_ID, res.getDokumenter().get(0).getDokumentInfoId());
	}


	@Test
	public void shouldGetJournalpostWhenResponseIs() {
		this.stubOpprettJournalpostResponseConflictWithValidResponse();
		OpprettJournalpostRequest request = OpprettJournalpostRequest.builder()
				.eksternReferanseId("ekstern")
				.build();

		OpprettJournalpostResponse response = opprettJournalpostConsumer.opprettJournalpost("token", request);
		assertEquals("567010363", response.getJournalpostId());
	}

	@Test
	public void shouldNotGetJournalpostWhenConflictDoesNotCorrectHaveBody() {
		this.stubOpprettJournalpostResponseConflictWithInvalidResponse();

		assertThrows(
				OpprettJournalpostFunctionalException.class,
				() -> opprettJournalpostConsumer.opprettJournalpost("token", null)
		);
	}

	private OpprettJournalpostRequest createOpprettJournalpostRequest() {
		List<Tilleggsopplysning> tilleggsopplysninger = List.of(
				new Tilleggsopplysning("fysiskPostboks", "1400"),
				new Tilleggsopplysning("strekkodePostboks", "1400"),
				new Tilleggsopplysning("endorsernr", "3110190003NAV743506")
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
