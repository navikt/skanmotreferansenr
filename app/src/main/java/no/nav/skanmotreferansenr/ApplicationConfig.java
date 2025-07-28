package no.nav.skanmotreferansenr;

import no.nav.skanmotreferansenr.config.props.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.consumer.azure.AzureProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableConfigurationProperties({
		SkanmotreferansenrProperties.class,
		AzureProperties.class,
})
@EnableRetry
@EnableScheduling
public class ApplicationConfig {

}