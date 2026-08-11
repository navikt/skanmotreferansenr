package no.nav.skanmotreferansenr.consumer.pdl;

import no.nav.skanmotreferansenr.config.props.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.config.props.SkanmotreferansenrProperties.AzureEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static no.nav.skanmotreferansenr.consumer.NavHeaders.NAV_CALL_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.POST;

class PdlGraphQLConsumerTest {

	private static final String PDL_URL = "http://pdl.local/graphql";
	private static final String PDL_SCOPE = "api://pdl/.default";
	private static final String FOEDSELSNUMMER = "12345678901";

	private final AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();

	@Test
	void shouldReturnereTrueNaarIdenterFinnes() {
		String body = """
				{"data":{"hentIdenter":{"identer":[
					{"ident":"12345678901","historisk":false,"gruppe":"FOLKEREGISTERIDENT"}
				]}}}""";
		PdlGraphQLConsumer consumer = consumerSimulatingPdlReturning(HttpStatus.OK, body);

		assertThat(consumer.identFinnesIPdl(FOEDSELSNUMMER)).isTrue();

		ClientRequest request = capturedRequest.get();
		assertThat(request.method()).isEqualTo(POST);
		assertThat(request.url().toString()).isEqualTo(PDL_URL);
		assertThat(request.headers().getFirst(NAV_CALL_ID)).isNotBlank();
	}

	@Test
	void shouldReturnereFalseNaarIngenIdenter() {
		String body = """
				{"data":{"hentIdenter":{"identer":[]}}}""";
		PdlGraphQLConsumer consumer = consumerSimulatingPdlReturning(HttpStatus.OK, body);

		assertThat(consumer.identFinnesIPdl(FOEDSELSNUMMER)).isFalse();
	}

	@Test
	void shouldReturnereFalseNaarPdlIkkeFinnerPersonen() {
		String body = """
				{"errors":[
					{"message":"fant ikke person","extensions":{"code":"not_found"}}
				],"data":null}""";
		PdlGraphQLConsumer consumer = consumerSimulatingPdlReturning(HttpStatus.OK, body);

		assertThat(consumer.identFinnesIPdl(FOEDSELSNUMMER)).isFalse();
	}

	@Test
	void shouldKasteFunctionalExceptionNaarPdlReturnererFeil() {
		String body = """
				{"errors":[
					{"message":"noe gikk galt","extensions":{"code":"server_error","classification":"ExecutionAborted"}}
				],"data":null}""";
		PdlGraphQLConsumer consumer = consumerSimulatingPdlReturning(HttpStatus.OK, body);

		assertThatThrownBy(() -> consumer.identFinnesIPdl(FOEDSELSNUMMER))
				.isInstanceOf(PdlTechnicalException.class);
	}

	@Test
	void shouldKasteFunctionalExceptionNaarIngenTilgang() {
		String body = """
				{"errors":[
					{"message":"ingen tilgang","extensions":{"code":"unauthorized","details":{"policy":"pdl-policy"}}}
				],"data":null}""";
		PdlGraphQLConsumer consumer = consumerSimulatingPdlReturning(HttpStatus.OK, body);

		assertThatThrownBy(() -> consumer.identFinnesIPdl(FOEDSELSNUMMER))
				.isInstanceOf(PdlFunctionalException.class)
				.hasMessageContaining("pdl-policy");
	}

	@Test
	void shouldThrowTechnicalExceptionVedHttp4xx() {
		PdlGraphQLConsumer consumer = consumerSimulatingPdlReturning(HttpStatus.FORBIDDEN, "");

		assertThatThrownBy(() -> consumer.identFinnesIPdl(FOEDSELSNUMMER))
				.isInstanceOf(PdlTechnicalException.class);
	}

	@Test
	void shouldThrowTechnicalExceptionVedHttp5xx() {
		PdlGraphQLConsumer consumer = consumerSimulatingPdlReturning(HttpStatus.INTERNAL_SERVER_ERROR, "");

		assertThatThrownBy(() -> consumer.identFinnesIPdl(FOEDSELSNUMMER))
				.isInstanceOf(PdlTechnicalException.class);
	}

	private PdlGraphQLConsumer consumerSimulatingPdlReturning(HttpStatus status, String body) {
		ExchangeFunction exchangeFunction = request -> {
			capturedRequest.set(request);
			return Mono.just(ClientResponse.create(status)
					.header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.body(body)
					.build());
		};
		WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
		return new PdlGraphQLConsumer(webClient, properties());
	}

	private SkanmotreferansenrProperties properties() {
		SkanmotreferansenrProperties properties = new SkanmotreferansenrProperties();
		AzureEndpoint pdl = new AzureEndpoint();
		pdl.setUrl(PDL_URL);
		pdl.setScope(PDL_SCOPE);
		properties.getEndpoints().setPdl(pdl);
		return properties;
	}
}
