package no.nav.skanmotreferansenr.consumer.azure;

import no.nav.skanmotreferansenr.config.props.SkanmotreferansenrProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientCredentialsReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;

import static java.time.temporal.ChronoUnit.SECONDS;
import static no.nav.skanmotreferansenr.consumer.azure.AzureProperties.CLIENT_REGISTRATION_FOERSTESIDEGENERATOR;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS;
import static org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC;

@Configuration
public class AzureOAuthEnabledWebClientConfig {

	@Bean
	WebClient webClient(ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager) {
		ServerOAuth2AuthorizedClientExchangeFilterFunction oAuth2AuthorizedClientExchangeFilterFunction = new ServerOAuth2AuthorizedClientExchangeFilterFunction(oAuth2AuthorizedClientManager);

		var nettyHttpClient = HttpClient.create()
				.responseTimeout(Duration.of(40, SECONDS));
		var clientHttpConnector = new ReactorClientHttpConnector(nettyHttpClient);

		return WebClient.builder()
				.clientConnector(clientHttpConnector)
				.filter(oAuth2AuthorizedClientExchangeFilterFunction)
				.build();
	}

	@Bean
	ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager(
			ReactiveClientRegistrationRepository clientRegistrationRepository,
			ReactiveOAuth2AuthorizedClientService oAuth2AuthorizedClientService
	) {
		ClientCredentialsReactiveOAuth2AuthorizedClientProvider authorizedClientProvider = new ClientCredentialsReactiveOAuth2AuthorizedClientProvider();
		var nettyHttpClient = HttpClient.create()
				.proxyWithSystemProperties()
				.responseTimeout(Duration.of(20, SECONDS));
		var clientHttpConnector = new ReactorClientHttpConnector(nettyHttpClient);

		WebClient webClientWithProxy = WebClient.builder()
				.clientConnector(clientHttpConnector)
				.build();

		WebClientReactiveClientCredentialsTokenResponseClient client = new WebClientReactiveClientCredentialsTokenResponseClient();
		client.setWebClient(webClientWithProxy);

		authorizedClientProvider.setAccessTokenResponseClient(client);

		AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistrationRepository, oAuth2AuthorizedClientService);
		authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
		return authorizedClientManager;
	}

	@Bean
	ReactiveOAuth2AuthorizedClientService oAuth2AuthorizedClientService(ReactiveClientRegistrationRepository clientRegistrationRepository) {
		return new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrationRepository);
	}

	@Bean
	ReactiveClientRegistrationRepository clientRegistrationRepository(List<ClientRegistration> clientRegistration) {
		return new InMemoryReactiveClientRegistrationRepository(clientRegistration);
	}

	@Bean
	List<ClientRegistration> clientRegistration(AzureProperties azureProperties, SkanmotreferansenrProperties skanmotreferansenrProperties) {
		return List.of(
				ClientRegistration.withRegistrationId(CLIENT_REGISTRATION_FOERSTESIDEGENERATOR)
						.tokenUri(azureProperties.openidConfigTokenEndpoint())
						.clientId(azureProperties.clientId())
						.clientSecret(azureProperties.clientSecret())
						.clientAuthenticationMethod(CLIENT_SECRET_BASIC)
						.authorizationGrantType(CLIENT_CREDENTIALS)
						.scope(skanmotreferansenrProperties.getEndpoints().getFoerstesidegenerator().getScope())
						.build()
		);
	}
}
