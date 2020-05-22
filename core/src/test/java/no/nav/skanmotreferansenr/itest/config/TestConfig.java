package no.nav.skanmotreferansenr.itest.config;

import no.nav.skanmotreferansenr.config.CoreConfig;
import no.nav.skanmotreferansenr.config.properties.SkanmotreferansenrProperties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties(SkanmotreferansenrProperties.class)
@Import(CoreConfig.class)
public class TestConfig {
}
