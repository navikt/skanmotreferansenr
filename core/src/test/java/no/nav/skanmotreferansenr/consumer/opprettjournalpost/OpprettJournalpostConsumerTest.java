package no.nav.skanmotreferansenr.consumer.opprettjournalpost;

import no.nav.skanmotreferansenr.config.props.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.consumer.opprettjournalpost.data.OpprettJournalpostResponse;
import no.nav.skanmotreferansenr.exceptions.functional.OpprettJournalpostFunctionalException;
import no.nav.skanmotreferansenr.itest.AbstractItest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.EnableRetry;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


@EnableRetry
class OpprettJournalpostConsumerTest extends AbstractItest {

	@Autowired
	OpprettJournalpostConsumer opprettJournalpostConsumer;

	@Autowired
	private SkanmotreferansenrProperties properties;

	@Test
	public void shouldGetJournalpostWhenResponseIs () {
		this.StubOpprettJournalpostResponseConflictWithValidResponse();

		OpprettJournalpostResponse response = opprettJournalpostConsumer.opprettJournalpost("token", null);
		assertEquals("567010363", response.getJournalpostId());
	}

	@Test
	public void shouldNotGetJournalpostWhenConflictDoesNotCorrectHaveBody() {
		this.StubOpprettJournalpostResponseConflictWithInvalidResponse();

		assertThrows(
				OpprettJournalpostFunctionalException.class,
				() -> opprettJournalpostConsumer.opprettJournalpost("token", null)
		);
		verify(exactly(5), postRequestedFor(urlMatching("/rest/journalpostapi/v1/journalpost\\?foersoekFerdigstill=false")));
	}
}