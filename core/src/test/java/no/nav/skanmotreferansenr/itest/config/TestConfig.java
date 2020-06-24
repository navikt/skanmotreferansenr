package no.nav.skanmotreferansenr.itest.config;

import no.nav.skanmotreferansenr.config.CoreConfig;
import no.nav.skanmotreferansenr.config.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.metrics.DokCounter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties(SkanmotreferansenrProperties.class)
@Import({CoreConfig.class, DokCounter.class})
public class TestConfig {
}
