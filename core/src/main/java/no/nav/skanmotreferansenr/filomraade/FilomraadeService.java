package no.nav.skanmotreferansenr.filomraade;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.exceptions.functional.LesZipFilFuntionalException;
import no.nav.skanmotreferansenr.exceptions.technical.SkanmotreferansenrSftpTechnicalException;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.util.List;

@Slf4j
@Service
public class FilomraadeService {

    private FilomraadeConsumer filomraadeConsumer;

    @Inject
    public FilomraadeService(FilomraadeConsumer filomraadeConsumer) {
        this.filomraadeConsumer = filomraadeConsumer;
    }

    public List<String> getFileNames() throws LesZipFilFuntionalException, SkanmotreferansenrSftpTechnicalException {
        return filomraadeConsumer.listZipFiles();
    }

    public void uploadFileToFeilomrade(byte[] file, String filename, String path) {
        try {
            filomraadeConsumer.uploadFileToFeilomrade(new ByteArrayInputStream(file), filename, path);
        } catch (Exception e) {
            log.error("Skanmotreferansenr klarte ikke laste opp fil {}", filename, e);
            throw e;
        }
    }


    public void deleteZipFiles(List<String> zipFiles) {
        zipFiles.stream().forEach(this::deleteZipFile);
    }

    public void moveZipFiles(List<String> files, String destination) {
        files.stream().forEach(file -> {
            moveFile(file, destination, file + ".processed");
        });
    }

    public void moveZipFile(String file, String destination) {
        moveFile(file, destination, file + ".processed");
    }

    private void deleteZipFile(String filename) {
        try {
            filomraadeConsumer.deleteFile(filename);
        } catch (Exception e) {
            log.error("Skanmotreferansenr klarte ikke slette fil {}", filename, e);
        }
    }

    private void moveFile(String from, String to, String newFilename) {
        try {
            filomraadeConsumer.moveFile(from, to, newFilename);
        } catch (Exception e) {
            log.error("Skanmotreferansenr klarte ikke flytte fil {} til {}/{}", from, to, newFilename, e);
        }
    }

    public byte[] getZipFile(String fileName) {
        try {
            return filomraadeConsumer.getFile(fileName);
        } catch (Exception e) {
            log.error("Skanmotreferansenr klarte ikke hente filen {}", fileName, e);
            return null;
        }
    }

    public void disconnect() {
        filomraadeConsumer.disconnect();
    }
}
