package no.nav.skanmotreferansenr;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.skanmotreferansenr.config.props.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.config.props.SkanmotreferansenrVaultProperties;
import no.nav.skanmotreferansenr.consumer.azure.AzureProperties;
import no.nav.skanmotreferansenr.metrics.DokTimedAspect;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableConfigurationProperties(value = {SkanmotreferansenrProperties.class, SkanmotreferansenrVaultProperties.class, AzureProperties.class})
@EnableAspectJAutoProxy
@EnableRetry
@EnableScheduling
public class ApplicationConfig {

    @Bean
    public DokTimedAspect timedAspect(MeterRegistry meterRegistry) {
        return new DokTimedAspect(meterRegistry);
    }

}