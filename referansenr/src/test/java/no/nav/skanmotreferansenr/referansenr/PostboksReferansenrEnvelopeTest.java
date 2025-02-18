package no.nav.skanmotreferansenr.referansenr;

import no.nav.skanmotreferansenr.exceptions.functional.ForsendelseNotCompleteException;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static no.nav.skanmotreferansenr.referansenr.PostboksReferansenrTestObjects.FILEBASENAME;
import static no.nav.skanmotreferansenr.referansenr.PostboksReferansenrTestObjects.PDF_FIL;
import static no.nav.skanmotreferansenr.referansenr.PostboksReferansenrTestObjects.XML_FIL;
import static no.nav.skanmotreferansenr.referansenr.PostboksReferansenrTestObjects.ZIPNAME;
import static no.nav.skanmotreferansenr.referansenr.PostboksReferansenrTestObjects.createBaseEnvelope;
import static no.nav.skanmotreferansenr.referansenr.PostboksReferansenrTestObjects.createEnvelope;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PostboksReferansenrEnvelopeTest {

    @Test
    void shouldThrowExceptionWhenValidateNoXml() {
        assertThrows(ForsendelseNotCompleteException.class, () -> createBaseEnvelope().xml(null).build()
				.validate(), "Fant ikke filnavn=" + FILEBASENAME + ".xml i zip=" + ZIPNAME);
    }

    @Test
    void shouldThrowExceptionWhenValidateNoPdf() {
        assertThrows(ForsendelseNotCompleteException.class, () -> createBaseEnvelope().pdf(null).build()
				.validate(), "Fant ikke filnavn=" + FILEBASENAME + ".pdf i zip=" + ZIPNAME);
    }

    @Test
    void shouldCreateZip() throws IOException, ArchiveException {
        final PostboksReferansenrEnvelope envelope = createEnvelope();
        ByteArrayInputStream zip = (ByteArrayInputStream) envelope.createZip();
        SeekableInMemoryByteChannel inMemoryByteChannel = new SeekableInMemoryByteChannel(zip.readAllBytes());
        ZipFile zipFile = ZipFile.builder().setSeekableByteChannel(inMemoryByteChannel).get();
        assertThat(readEntry(zipFile, FILEBASENAME + ".xml")).containsExactly(XML_FIL);
        assertThat(readEntry(zipFile, FILEBASENAME + ".pdf")).containsExactly(PDF_FIL);
    }

    private byte[] readEntry(final ZipFile zipFile, final String name) throws IOException {
        ZipArchiveEntry archiveEntry = zipFile.getEntry(name);
        InputStream inputStream = zipFile.getInputStream(archiveEntry);
        return IOUtils.toByteArray(inputStream);
    }
}