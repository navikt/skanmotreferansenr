package no.nav.skanmotreferansenr.filomraade;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.config.properties.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.exceptions.functional.LesZipFilFuntionalException;
import no.nav.skanmotreferansenr.exceptions.technical.SkanmotreferansenrSftpTechnicalException;
import no.nav.skanmotreferansenr.sftp.Sftp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
public class FilomraadeConsumer {

    private Sftp sftp;
    private String inboundDirectory;
    private String feilDirectory;

    @Autowired
    public FilomraadeConsumer(Sftp sftp, SkanmotreferansenrProperties skanmotreferansenrProperties) {
        this.sftp = sftp;
        this.inboundDirectory = skanmotreferansenrProperties.getFilomraade().getInngaaendemappe();
        this.feilDirectory = skanmotreferansenrProperties.getFilomraade().getFeilmappe();
    }

    public List<String> listZipFiles() {
        try {
            log.info("Skanmotreferansenr henter zipfiler fra {}", sftp.getHomePath() + inboundDirectory);
            List<String> files = sftp.listFiles(inboundDirectory + "/*.zip");
            return files;
        } catch (Exception e) {
            throw new LesZipFilFuntionalException("Skanmotreferansenr klarte ikke hente zipfiler", e);
        }
    }

    public byte[] getFile(String filename) throws SkanmotreferansenrSftpTechnicalException, IOException {
        InputStream fileStream = sftp.getFile(inboundDirectory + "/" + filename);
        byte[] file = fileStream.readAllBytes();
        fileStream.close();
        return file;
    }


    public void deleteFile(String filename) {
        log.info("Skanmotreferansenr sletter fil {}", filename);
        sftp.deleteFile(inboundDirectory, filename);
    }

    public void uploadFileToFeilomrade(InputStream file, String filename, String path) {
        log.info("Skanmotreferansenr laster opp fil {} til feilområde", filename);
        sftp.uploadFile(file, feilDirectory + "/" + path, filename);
    }

    public void moveFile(String from, String to, String newFilename) {
        String fromPath = inboundDirectory + "/" + from;
        String toPath = inboundDirectory + "/" + to;
        log.info("Skanmotreferansenr flytter fil {} til {}", fromPath, toPath);
        sftp.moveFile(fromPath, toPath, newFilename);
    }

    public void disconnect() {
        sftp.disconnect();
    }
}
