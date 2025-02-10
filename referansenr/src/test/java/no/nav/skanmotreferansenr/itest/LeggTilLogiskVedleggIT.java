package no.nav.skanmotreferansenr.itest;

import no.nav.skanmotreferansenr.consumer.journalpostapi.JournalpostConsumer;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.LeggTilLogiskVedleggRequest;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.LeggTilLogiskVedleggResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class LeggTilLogiskVedleggIT extends AbstractItest {

	@Autowired
	private JournalpostConsumer journalpostConsumer;

	@BeforeEach
	public void setUp() {
		stubAzureToken();
		super.setUpStubs();
	}

	@Test
	public void shouldLeggTilLogiskVedlegg() {
		LeggTilLogiskVedleggRequest request = LeggTilLogiskVedleggRequest.builder().tittel("Tittel").build();
		LeggTilLogiskVedleggResponse response = journalpostConsumer.leggTilLogiskVedlegg(request, LOGISK_VEDLEGG_ID);
		assertEquals(LOGISK_VEDLEGG_ID, response.getLogiskVedleggId());
		verify(exactly(1), postRequestedFor(urlMatching(URL_DOKARKIV_DOKUMENTINFO_LOGISKVEDLEGG)));
	}
}
