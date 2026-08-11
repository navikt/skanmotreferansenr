package no.nav.skanmotreferansenr.consumer.pdl;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.config.props.SkanmotreferansenrProperties;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static no.nav.skanmotreferansenr.consumer.NavHeaders.NAV_CALL_ID;
import static no.nav.skanmotreferansenr.consumer.azure.AzureOAuthEnabledWebClientConfig.CLIENT_REGISTRATION_PDL;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Component
public class PdlGraphQLConsumer {

	private static final String PDL_ERROR_EXTENSION_CODE_NOT_FOUND = "not_found";
	private static final String PDL_ERROR_EXTENSION_CODE_UNAUTHORIZED = "unauthorized";

	private final WebClient webClient;

	private enum PersonLookupResult {
		FOUND,
		NOT_FOUND
	}

	public PdlGraphQLConsumer(WebClient webClient,
							  SkanmotreferansenrProperties skanmotreferansenrProperties
	) {
		this.webClient = webClient.mutate()
			.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
			.defaultHeaders(httpHeaders -> httpHeaders.setAccept(List.of(APPLICATION_JSON)))
			.baseUrl(skanmotreferansenrProperties.getEndpoints().getPdl().getUrl())
			.build();
	}

	public boolean identFinnesIPdl(String foedselsnummer) {
		PDLHentIdenterResponse response = hentIdenter(foedselsnummer);
		return toPersonLookupResult(response) == PersonLookupResult.FOUND;
	}

	private PDLHentIdenterResponse hentIdenter(String foedselsnummer) {
		return requireNonNull(webClient.post()
			.header(NAV_CALL_ID, getCallId())
			.bodyValue(mapRequest(foedselsnummer, hentIdenterQuery))
			.attributes(clientRegistrationId(CLIENT_REGISTRATION_PDL))
			.retrieve()
			.bodyToMono(PDLHentIdenterResponse.class)
			.onErrorMap(WebClientResponseException.class, this::mapHttpProtocolError)
			.block());
	}

	private Throwable mapHttpProtocolError(WebClientResponseException e) {
		return new PdlTechnicalException("Teknisk feil ved kall mot PDL.", e);
	}

	private PDLRequest mapRequest(String aktoerId, String query) {
		final HashMap<String, Object> variables = new HashMap<>();
		variables.put("ident", aktoerId);
		return new PDLRequest(query, variables);
	}

	private PersonLookupResult toPersonLookupResult(PDLHentIdenterResponse response) {
		List<PDLError> errors = response.errors();
		if (errors != null && !errors.isEmpty()) {
			return handlePdlGraphqlProtocolErrors(errors);
		}
		if (response.data() != null
			&& response.data().hentIdenter() != null
			&& response.data().hentIdenter().identer() != null
			&& !response.data().hentIdenter().identer().isEmpty()) {
			return PersonLookupResult.FOUND;
		} else {
			return PersonLookupResult.NOT_FOUND;
		}
	}

	private PersonLookupResult handlePdlGraphqlProtocolErrors(List<PDLError> errors) {
		if (hasErrorCode(errors, PDL_ERROR_EXTENSION_CODE_NOT_FOUND)) {
			return PersonLookupResult.NOT_FOUND;
		}

		errors.stream()
			.filter(p -> p.extensions() != null)
			.filter(p -> PDL_ERROR_EXTENSION_CODE_UNAUTHORIZED.equals(p.extensions().code()))
			.findFirst()
			.ifPresent(pdlError -> {
				throw new PdlFunctionalException("Ingen tilgang til å se data om person. Avvist av policy=" +
					(pdlError.extensions().details() != null ? pdlError.extensions().details().policy() : "[null]"), null);
			});

		var feilmeldinger = errors.stream()
			.map(PDLError::message)
			.collect(Collectors.joining(", "));

		log.warn("Kunne ikke hente person fra Pdl. Feilmeldinger={}", feilmeldinger);
		throw new PdlTechnicalException("Kunne ikke hente person fra Pdl " + errors, null);
	}

	private boolean hasErrorCode(List<PDLError> errors, String errorCode) {
		return errors.stream()
			.filter(p -> p.extensions() != null)
			.anyMatch(p -> errorCode.equals(p.extensions().code()));
	}

	private static final String hentIdenterQuery = """
		query($ident: ID!) {
		    hentIdenter(ident: $ident, historikk:false) {
		        identer {
		            ident,
		            historisk,
		            gruppe
		        }
		    }
		}
		""";

	public static String getCallId() {
		return isBlank(MDC.get(NAV_CALL_ID)) ? UUID.randomUUID().toString() : MDC.get(NAV_CALL_ID);
	}
}
