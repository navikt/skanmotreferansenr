package no.nav.skanmotreferansenr.itest;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.CoreConfig;
import no.nav.skanmotreferansenr.config.props.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.consumer.azure.AzureOAuthEnabledWebClientConfig;
import no.nav.skanmotreferansenr.consumer.azure.AzureProperties;
import no.nav.skanmotreferansenr.metrics.DokCounter;
import org.apache.camel.CamelContext;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.keyprovider.ClassLoadableResourceKeyPairProvider;
import org.apache.sshd.scp.server.ScpCommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.UserAuthNoneFactory;
import org.apache.sshd.server.auth.pubkey.AcceptAllPublickeyAuthenticator;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Integer.parseInt;
import static java.util.Collections.singletonList;
import static no.nav.skanmotreferansenr.CoreConfig.DEFAULT_ZONE_ID;

@Slf4j
@EnableAutoConfiguration
@EnableConfigurationProperties({
		SkanmotreferansenrProperties.class,
		AzureProperties.class
})
@Import({
		CoreConfig.class,
		AvstemTestConfig.Config.class,
		AvstemTestConfig.CamelTestStartupConfig.class,
		AvstemTestConfig.SshdSftpServerConfig.class,
		DokCounter.class,
		AzureOAuthEnabledWebClientConfig.class
})
public class AvstemTestConfig {

	@Configuration
	static class Config {
		@Bean
		@Primary
		@Profile("virkedag")
		Clock forrigeDagVirkedagClock() {
			Instant fixedInstant = Instant.parse("2025-08-01T10:00:00Z");
			return Clock.fixed(fixedInstant, DEFAULT_ZONE_ID);
		}

		@Bean
		@Primary
		@Profile("fridag")
		Clock forrigeDagFridagClock() {
			Instant fixedInstant = Instant.parse("2025-05-18T10:00:00Z");
			return Clock.fixed(fixedInstant, DEFAULT_ZONE_ID);
		}
	}

	@Configuration
	static class CamelTestStartupConfig {

		private final AtomicInteger sshServerStartupCounter = new AtomicInteger(0);

		@Bean
		CamelContextConfiguration contextConfiguration(SshServer sshServer) {
			return new CamelContextConfiguration() {

				@Override
				public void beforeApplicationStart(CamelContext camelContext) {
					while (!sshServer.isStarted() && sshServerStartupCounter.get() <= 5) {
						try {
							// Busy wait
							Thread.sleep(1000);
							log.info("Forsøkt å starte sshserver. retry={}", sshServerStartupCounter.getAndIncrement());
						} catch (InterruptedException e) {
							// noop
						}
					}
				}

				@Override
				public void afterApplicationStart(CamelContext camelContext) {

				}
			};
		}
	}

	@Configuration
	static class SshdSftpServerConfig {
		@Bean
		public Path sshdPath() throws IOException {
			return Files.createTempDirectory("sshd");
		}

		@Bean(initMethod = "start", destroyMethod = "stop")
		public SshServer sshServer(Path sshdPath,
								   SkanmotreferansenrProperties skanmotreferansenrProperties) {
			SshServer sshd = SshServer.setUpDefaultServer();
			sshd.setPort(parseInt(skanmotreferansenrProperties.getSftp().getPort()));
			sshd.setKeyPairProvider(new ClassLoadableResourceKeyPairProvider("sftp/server_id_rsa"));
			sshd.setCommandFactory(new ScpCommandFactory());
			sshd.setSubsystemFactories(singletonList(new SftpSubsystemFactory()));
			// aksepterer alle public keys som presenteres, behøver ikke authorized_keys
			sshd.setPublickeyAuthenticator(AcceptAllPublickeyAuthenticator.INSTANCE);
			sshd.setUserAuthFactories(singletonList(new UserAuthNoneFactory()));
			sshd.setFileSystemFactory(new VirtualFileSystemFactory(sshdPath));
			return sshd;
		}
	}
}



