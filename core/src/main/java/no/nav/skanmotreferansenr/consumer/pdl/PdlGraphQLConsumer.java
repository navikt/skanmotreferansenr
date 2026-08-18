package no.nav.skanmotreferansenr.consumer.pdl;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.config.props.SkanmotreferansenrProperties;
import org.slf4j.MDC;
import org.springframework.graphql.ResponseError;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.GraphQlClientException;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static no.nav.skanmotreferansenr.consumer.azure.AzureOAuthEnabledWebClientConfig.CLIENT_REGISTRATION_PDL;
import static no.nav.skanmotreferansenr.mdc.MDCConstants.MDC_CALL_ID;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Component
public class PdlGraphQLConsumer {

	static final String NAV_CALL_ID_PDL = "Nav-Call-Id";
	private static final String PDL_ERROR_EXTENSION_CODE_NOT_FOUND = "not_found";
	private static final String PDL_ERROR_EXTENSION_CODE_UNAUTHORIZED = "unauthorized";
	private static final String HENT_IDENTER_QUERY = """
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

	private final HttpGraphQlClient graphQlClient;

	public PdlGraphQLConsumer(WebClient webClient,
							  SkanmotreferansenrProperties skanmotreferansenrProperties
	) {
		this.graphQlClient = HttpGraphQlClient.builder(webClient)
			.url(skanmotreferansenrProperties.getEndpoints().getPdl().getUrl())
			.build();
	}

	public boolean identFinnesIPdl(String foedselsnummer) {
		ClientGraphQlResponse response = hentIdenter(foedselsnummer);
		return toPersonLookupResult(response) == PersonLookupResult.FOUND;
	}

	private ClientGraphQlResponse hentIdenter(String foedselsnummer) {
		try {
			return graphQlClient.mutate()
				.header(NAV_CALL_ID_PDL, getCallId())
				.build()
				.document(HENT_IDENTER_QUERY)
				.variable("ident", foedselsnummer)
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_PDL))
				.executeSync();
		} catch (WebClientResponseException | GraphQlClientException e) {
			throw new PdlTechnicalException("Teknisk feil ved kall mot PDL.", e);
		}
	}

	private PersonLookupResult toPersonLookupResult(ClientGraphQlResponse response) {
		List<ResponseError> errors = response.getErrors();
		if (!errors.isEmpty()) {
			return handlePdlGraphqlProtocolErrors(errors);
		}

		PDLHentIdenter data = response.toEntity(PDLHentIdenter.class);
		if (data.hentIdenter() != null &&
			data.hentIdenter().identer() != null &&
			!data.hentIdenter().identer().isEmpty()) {
			return PersonLookupResult.FOUND;
		} else {
			return PersonLookupResult.NOT_FOUND;
		}
	}

	private PersonLookupResult handlePdlGraphqlProtocolErrors(List<ResponseError> errors) {
		if (hasErrorCode(errors, PDL_ERROR_EXTENSION_CODE_NOT_FOUND)) {
			return PersonLookupResult.NOT_FOUND;
		}

		errors.stream()
			.filter(e -> PDL_ERROR_EXTENSION_CODE_UNAUTHORIZED.equals(e.getExtensions().get("code")))
			.findFirst()
			.ifPresent(pdlError -> {
				throw new PdlFunctionalException("Ingen tilgang til å se data om person. Avvist av policy=" +
					policyFra(pdlError), null);
			});

		var feilmeldinger = errors.stream()
			.map(ResponseError::getMessage)
			.collect(Collectors.joining(", "));

		log.warn("Kunne ikke henteidenter for person fra PDL. Feilmeldinger={}", feilmeldinger);
		throw new PdlTechnicalException("Kunne ikke hente person fra Pdl " + errors, null);
	}

	private boolean hasErrorCode(List<ResponseError> errors, String errorCode) {
		return errors.stream()
			.anyMatch(e -> errorCode.equals(e.getExtensions().get("code")));
	}

	private String policyFra(ResponseError error) {
		Object details = error.getExtensions().get("details");
		if (details instanceof Map<?, ?> detailsMap && detailsMap.get("policy") != null) {
			return detailsMap.get("policy").toString();
		}
		return "[null]";
	}

	public static String getCallId() {
		return isBlank(MDC.get(MDC_CALL_ID)) ? UUID.randomUUID().toString() : MDC.get(MDC_CALL_ID);
	}

	private enum PersonLookupResult {
		FOUND,
		NOT_FOUND
	}
}
