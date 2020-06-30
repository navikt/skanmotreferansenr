package no.nav.skanmotreferansenr.itest.config;

import no.nav.skanmotreferansenr.config.CoreConfig;
import no.nav.skanmotreferansenr.config.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.metrics.DokCounter;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.UserAuthNoneFactory;
import org.apache.sshd.server.config.keys.AuthorizedKeysAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties(SkanmotreferansenrProperties.class)
@Import({CoreConfig.class, DokCounter.class})
public class TestConfig {

    private static final String sftpPort = String.valueOf(ThreadLocalRandom.current().nextInt(2000, 2999));

    static {
        System.setProperty("skanmotreferansenr.sftp.port", sftpPort);
    }

    @Configuration
    static class SshdSftpServerConfig {
        @Bean
        public Path sshdPath() throws IOException {
            return Files.createTempDirectory("sshd");
        }

        @Bean(initMethod = "start", destroyMethod = "stop")
        public SshServer sshServer(final Path sshdPath,
                                   final SkanmotreferansenrProperties properties) throws IOException {
            SshServer sshd = SshServer.setUpDefaultServer();
            sshd.setPort(Integer.parseInt(properties.getSftp().getPort()));
            sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Path.of("src/test/resources/sftp/itest.ser")));
            sshd.setCommandFactory(new ScpCommandFactory());
            sshd.setSubsystemFactories(List.of(new SftpSubsystemFactory()));
            sshd.setPublickeyAuthenticator(new AuthorizedKeysAuthenticator(Paths.get("src/test/resources/sftp/itest_valid.pub")));
            sshd.setUserAuthFactories(Collections.singletonList(new UserAuthNoneFactory()));
            sshd.setFileSystemFactory(new VirtualFileSystemFactory(sshdPath));
            return sshd;
        }

    }
}
