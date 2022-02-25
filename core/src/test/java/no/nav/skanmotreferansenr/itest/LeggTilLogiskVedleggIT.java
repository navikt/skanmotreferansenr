package no.nav.skanmotreferansenr.itest;

import no.nav.skanmotreferansenr.config.props.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.consumer.logiskvedlegg.LeggTilLogiskVedleggConsumer;
import no.nav.skanmotreferansenr.consumer.logiskvedlegg.data.LeggTilLogiskVedleggRequest;
import no.nav.skanmotreferansenr.consumer.logiskvedlegg.data.LeggTilLogiskVedleggResponse;
import no.nav.skanmotreferansenr.consumer.sts.STSConsumer;
import no.nav.skanmotreferansenr.consumer.sts.data.STSResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class LeggTilLogiskVedleggIT extends AbstractItest {

	private LeggTilLogiskVedleggConsumer leggTilLogiskVedleggConsumer;
	private STSConsumer stsConsumer;

	@Autowired
	private SkanmotreferansenrProperties properties;

	@BeforeEach
	void setUpConsumer() {
		this.setUpStubs();
		stsConsumer = new STSConsumer(new RestTemplateBuilder(), properties);
		leggTilLogiskVedleggConsumer = new LeggTilLogiskVedleggConsumer(new RestTemplateBuilder(), properties);
	}

	@Test
	public void shouldLeggTilLogiskVedlegg() {
		LeggTilLogiskVedleggRequest request = LeggTilLogiskVedleggRequest.builder().tittel("Tittel").build();
		STSResponse stsResponse = stsConsumer.getSTSToken();
		LeggTilLogiskVedleggResponse response = leggTilLogiskVedleggConsumer.leggTilLogiskVedlegg(request, LOGISK_VEDLEGG_ID, stsResponse.getAccess_token());
		assertEquals(LOGISK_VEDLEGG_ID, response.getLogiskVedleggId());
		verify(exactly(1), postRequestedFor(urlMatching(URL_DOKARKIV_DOKUMENTINFO_LOGISKVEDLEGG)));
	}
}
