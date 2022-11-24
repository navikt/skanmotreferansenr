package no.nav.skanmotreferansenr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import no.nav.skanmotreferansenr.domain.Skanningmetadata;
import no.nav.skanmotreferansenr.exceptions.functional.ForsendelseNotCompleteException;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Builder
@Data
@RequiredArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"xml", "pdf"})
public class PostboksReferansenrEnvelope {
    private final String zipname;
    private final String filebasename;
    private Skanningmetadata skanningmetadata;
    private byte[] xml;
    private byte[] pdf;

    public void validate() {
        if (xml == null) {
            throw new ForsendelseNotCompleteException("Fant ikke filnavn=" + filebasename + ".xml i zip=" + zipname);
        }
        if (pdf == null) {
            throw new ForsendelseNotCompleteException("Fant ikke filnavn=" + filebasename + ".pdf i zip=" + zipname);
        }
    }

    public InputStream createZip() throws ArchiveException, IOException {
        ByteArrayOutputStream archiveStream = new ByteArrayOutputStream(4096);
        try (ArchiveOutputStream archive = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP, archiveStream)) {
            createZipEntry(archive, xml, "xml");
            createZipEntry(archive, pdf, "pdf");
        }
        return new ByteArrayInputStream(archiveStream.toByteArray());
    }

    private void createZipEntry(final ArchiveOutputStream archive, final byte[] bytes, final String extension) throws IOException {
        if (bytes != null) {
            ZipArchiveEntry entry = new ZipArchiveEntry(createEntryName(extension));
            archive.putArchiveEntry(entry);
            try (BufferedInputStream input = new BufferedInputStream(new ByteArrayInputStream(bytes))) {
                IOUtils.copy(input, archive);
            }
            archive.closeArchiveEntry();
        }
    }

    public String createEntryName(final String extension) {
        return filebasename + "." + extension;
    }
}
