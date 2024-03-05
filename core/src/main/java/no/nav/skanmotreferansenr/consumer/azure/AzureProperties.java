package no.nav.skanmotreferansenr.consumer.azure;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Konfigurert av naiserator. https://doc.nais.io/security/auth/azure-ad/#runtime-variables-credentials
 */
@Validated
@ConfigurationProperties(prefix = "azure.app")
public record AzureProperties(
		@NotEmpty String tokenUrl,
		@NotEmpty String clientId,
		@NotEmpty String clientSecret
) {
	public static final String CLIENT_REGISTRATION_FOERSTESIDEGENERATOR = "azure-foerstesidegenerator";
}
