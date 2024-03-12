package no.nav.skanmotreferansenr.consumer.azure;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Konfigurert av naiserator. https://doc.nais.io/security/auth/azure-ad/usage/#variables-for-acquiring-tokens:~:text=nais.io/azure/-,Variables%20for%20Acquiring%20Tokens,-%C2%B6
 */
@Validated
@ConfigurationProperties(prefix = "azure")
public record AzureProperties(
		@NotEmpty String openidConfigTokenEndpoint,
		@NotEmpty String appClientId,
		@NotEmpty String appClientSecret
) {
	public static final String CLIENT_REGISTRATION_FOERSTESIDEGENERATOR = "azure-foerstesidegenerator";
}
