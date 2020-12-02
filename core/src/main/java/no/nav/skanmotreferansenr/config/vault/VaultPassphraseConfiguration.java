package no.nav.skanmotreferansenr.config.vault;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.annotation.VaultPropertySource;

@Configuration
@VaultPropertySource(
        value = "${skanmotreferansenr.vault.secretpath}",
        propertyNamePrefix = "skanmotreferansenr.secret.",
        ignoreSecretNotFound = false
)
@ConditionalOnProperty("spring.cloud.vault.enabled")
public class VaultPassphraseConfiguration {

}
