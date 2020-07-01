package no.nav.skanmotreferansenr.itest;

import no.nav.skanmotreferansenr.config.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.exceptions.technical.SkanmotreferansenrSftpTechnicalException;
import no.nav.skanmotreferansenr.itest.config.TestConfig;
import no.nav.skanmotreferansenr.sftp.Sftp;
import org.apache.sshd.common.FactoryManager;
import org.apache.sshd.common.PropertyResolverUtils;
import org.apache.sshd.server.SshServer;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import wiremock.org.apache.commons.io.FileUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@ActiveProfiles("itest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = {TestConfig.class})
public class SftpTestIT {

    private static final String INNGAAENDE = "inngaaende";
    private static final String ZIP_FILE_NAME = "xml_pdf_pairs_testdata.zip";
    private static final String ZIP_FILE_PATH = "__files/xml_pdf_pairs/" + ZIP_FILE_NAME;
    private static final String DIR_ONE = "dirOne";
    private static final String DIR_TWO = "dirTwo";

    private Sftp sftp;

    @Autowired
    SkanmotreferansenrProperties properties;

    @Inject
    private Path sshdPath;

    @Inject
    private SshServer sshd;

    @BeforeAll
    void startSftpServer() throws IOException {
        final Path inngaaende = sshdPath.resolve(INNGAAENDE);
        final Path dir1 = sshdPath.resolve(DIR_ONE);
        final Path dir2 = sshdPath.resolve(DIR_TWO);
        preparePath(inngaaende);
        preparePath(dir1);
        preparePath(dir2);

        sftp = new Sftp(properties);

        moveFilesToDirectory();
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
            sftp.changeDirectory(sftp.getHomePath() + DIR_ONE);
            Assert.assertTrue(sftp.presentWorkingDirectory().endsWith(sftp.getHomePath() + DIR_ONE));
            Assert.assertTrue(sftp.listFiles().contains("fileOne"));

            sftp.changeDirectory(sftp.getHomePath() + DIR_TWO);
            Assert.assertTrue(sftp.presentWorkingDirectory().endsWith(sftp.getHomePath() + DIR_TWO));
            Assert.assertTrue(sftp.listFiles().contains("fileTwo"));

            sftp.changeDirectory(sftp.getHomePath() + INNGAAENDE);
            Assert.assertTrue(sftp.presentWorkingDirectory().endsWith(sftp.getHomePath() + INNGAAENDE));
            Assert.assertTrue(sftp.listFiles().contains(ZIP_FILE_NAME));
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void shouldFailToChangeDirectoryToInvalidPath() {
        try {
            sftp.changeDirectory("ikke/en/gyldig/path");
            Assert.fail();
        } catch (SkanmotreferansenrSftpTechnicalException e) {
            Assert.assertEquals("failed to change directory, path: ikke/en/gyldig/path", e.getMessage());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    void shouldGetFile() {
        try {
            sftp.changeDirectory(INNGAAENDE);

            InputStream inputStream = sftp.getFile(ZIP_FILE_NAME);
            Assert.assertArrayEquals(new ClassPathResource(ZIP_FILE_PATH).getInputStream().readAllBytes(), inputStream.readAllBytes());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    void shouldFailToGetFileWhenFileNameIsInvalid() {
        try {
            sftp.changeDirectory(INNGAAENDE);
            sftp.getFile("invalidFileName.zip");

            Assert.fail();
        } catch (SkanmotreferansenrSftpTechnicalException e) {
            Assert.assertEquals("failed to download invalidFileName.zip", e.getMessage());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    private void preparePath(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectory(path);
        } else {
            FileUtils.cleanDirectory(path.toFile());
        }
    }

    private void moveFilesToDirectory() throws IOException {
        Files.copy(new ClassPathResource("sftp/" + DIR_ONE + "/fileOne").getInputStream(), sshdPath.resolve(DIR_ONE).resolve("fileOne"));
        Files.copy(new ClassPathResource("sftp/" + DIR_TWO + "/fileTwo").getInputStream(), sshdPath.resolve(DIR_TWO).resolve("fileTwo"));
        Files.copy(new ClassPathResource(ZIP_FILE_PATH).getInputStream(), sshdPath.resolve(INNGAAENDE).resolve(ZIP_FILE_NAME));
    }
}
