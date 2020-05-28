package no.nav.skanmotreferansenr.itest;

import no.nav.skanmotreferansenr.config.properties.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.exceptions.technical.SkanmotreferansenrSftpTechnicalException;
import no.nav.skanmotreferansenr.itest.config.TestConfig;
import no.nav.skanmotreferansenr.sftp.Sftp;
import org.apache.sshd.common.FactoryManager;
import org.apache.sshd.common.PropertyResolverUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.config.keys.AuthorizedKeysAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@ActiveProfiles("itest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = {TestConfig.class})
public class SftpTestIT {
    private static final String ZIP_FILE_NAME = "xml_pdf_pairs_testdata.zip";
    private static final String RESOURCE_FOLDER_PATH = "src/test/resources/inbound";
    private static final String ZIP_FILE_PATH = "src/test/resources/inbound/" + ZIP_FILE_NAME;
    private static final String DIR_ONE_FOLDER_PATH = "src/test/resources/sftp/dirOne";
    private static final String DIR_TWO_FOLDER_PATH = "src/test/resources/sftp/dirTwo";
    private static final String INVALID_FOLDER_PATH = "foo/bar/baz";
    private static final String VALID_PUBLIC_KEY_PATH = "src/test/resources/sftp/itest_valid.pub";

    private int PORT = 2222;

    private SshServer sshd = SshServer.setUpDefaultServer();
    private Sftp sftp;

    private final Path MOCK_ZIP = Path.of("src/test/resources/__files/xml_pdf_pairs/" + ZIP_FILE_NAME);
    private final Path SKANMOTREFERANSENR_ZIP_PATH = Path.of("src/test/resources/inbound/" + ZIP_FILE_NAME);

    @Autowired
    SkanmotreferansenrProperties properties;

    @BeforeAll
    void startSftpServer() throws IOException {
        copyFileToSkanmotreferansenrFolder();

        sshd.setPort(PORT);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Path.of("src/test/resources/sftp/itest.ser")));
        sshd.setCommandFactory(new ScpCommandFactory());
        sshd.setSubsystemFactories(List.of(new SftpSubsystemFactory()));
        sshd.setPublickeyAuthenticator(new AuthorizedKeysAuthenticator(Paths.get(VALID_PUBLIC_KEY_PATH)));

        sshd.start();
        sftp = new Sftp(properties);
    }

    @Test
    public void shouldConnectAndReconnectToSftp() {
        try {
            PropertyResolverUtils.updateProperty(sshd, FactoryManager.IDLE_TIMEOUT, 2000L);

            sftp.listFiles();
            Assert.assertTrue(sftp.isConnected());
            Assert.assertEquals(1, sshd.getActiveSessions().size());
            Assert.assertEquals("itestUser", sshd.getActiveSessions().iterator().next().getUsername());

            Thread.sleep(3000);
            Assert.assertFalse(sftp.isConnected());

            sftp.listFiles();
            Assert.assertTrue(sftp.isConnected());
            Assert.assertEquals(1, sshd.getActiveSessions().size());
            Assert.assertEquals("itestUser", sshd.getActiveSessions().iterator().next().getUsername());
        } catch (Exception e) {
            Assert.fail();
        } finally {
            PropertyResolverUtils.updateProperty(sshd, FactoryManager.IDLE_TIMEOUT, 60000L);
        }
    }

    @Test
    void shouldChangeDirectoryAndListFiles() {
        try {
            String homePath = sftp.getHomePath() + "/";

            sftp.changeDirectory(homePath + DIR_ONE_FOLDER_PATH);
            Assert.assertTrue(sftp.presentWorkingDirectory().endsWith(homePath + DIR_ONE_FOLDER_PATH));
            Assert.assertTrue(sftp.listFiles().contains("fileOne"));

            sftp.changeDirectory(homePath + DIR_TWO_FOLDER_PATH);
            Assert.assertTrue(sftp.presentWorkingDirectory().endsWith(homePath + DIR_TWO_FOLDER_PATH));
            Assert.assertTrue(sftp.listFiles().contains("fileTwo"));

            sftp.changeDirectory(homePath + RESOURCE_FOLDER_PATH);
            Assert.assertTrue(sftp.presentWorkingDirectory().endsWith(homePath + RESOURCE_FOLDER_PATH));
            Assert.assertTrue(sftp.listFiles().contains(ZIP_FILE_NAME));
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void shouldFailToChangeDirectoryToInvalidPath() {
        try {
            sftp.changeDirectory(INVALID_FOLDER_PATH);
            Assert.fail();
        } catch (SkanmotreferansenrSftpTechnicalException e) {
            Assert.assertEquals("failed to change directory, path: foo/bar/baz", e.getMessage());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    void shouldGetFile() {
        try {
            File zipFile = Paths.get(ZIP_FILE_PATH).toFile();

            sftp.changeDirectory(RESOURCE_FOLDER_PATH);

            InputStream inputStream = sftp.getFile(ZIP_FILE_NAME);
            Assert.assertArrayEquals(inputStream.readAllBytes(), new FileInputStream(zipFile).readAllBytes());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    void shouldFailToGetFileWhenFileNameIsInvalid() {
        try {
            sftp.changeDirectory(RESOURCE_FOLDER_PATH);
            sftp.getFile("invalidFileName.zip");

            Assert.fail();
        } catch (SkanmotreferansenrSftpTechnicalException e) {
            Assert.assertEquals("failed to download invalidFileName.zip", e.getMessage());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @AfterAll
    void shutdownSftpServer() throws IOException {
        sshd.stop();
        sshd.close();
    }

    private void copyFileToSkanmotreferansenrFolder() {
        try {
            Path source = MOCK_ZIP;
            Path dest = SKANMOTREFERANSENR_ZIP_PATH;
            Files.copy(source, dest);
        } catch (IOException ignored) {
            // File either already exists or the test will crash and burn
        }
    }
}
