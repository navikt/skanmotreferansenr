package no.nav.skanmotreferansenr.unzipskanningmetadata;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.domain.Filepair;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.nav.skanmotreferansenr.utils.Utils.changeFiletypeInFilename;


@Slf4j
public class Unzipper {

    public static List<Filepair> unzipXmlPdf(byte[] zip) throws IOException {
        ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(new ByteArrayInputStream(zip));
        return unzipXmlPdf(zipInputStream);
    }

    public static List<Filepair> unzipXmlPdf(File zip) throws IOException {
        ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(new FileInputStream(zip));
        return unzipXmlPdf(zipInputStream);
    }

    public static List<Filepair> unzipXmlPdf(ZipArchiveInputStream zipInputStream) throws IOException {
        Map<String, byte[]> xmls = new HashMap<>();
        Map<String, byte[]> pdfs = new HashMap<>();
        byte[] buffer = new byte[1024];
        ZipArchiveEntry zipEntry = zipInputStream.getNextZipEntry();

        while (zipEntry != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int len;
            while ((len = zipInputStream.read(buffer)) > 0) {
                byteArrayOutputStream.write(buffer, 0, len);
            }
            if ("xml".equalsIgnoreCase(UnzipSkanningmetadataUtils.getFileType(zipEntry))) {
                String xmlName = changeFiletypeInFilename(zipEntry.getName(), "xml");
                xmls.put(xmlName, byteArrayOutputStream.toByteArray());
            }
            if ("pdf".equalsIgnoreCase(UnzipSkanningmetadataUtils.getFileType(zipEntry))) {
                String pdfName = changeFiletypeInFilename(zipEntry.getName(), "pdf");
                pdfs.put(pdfName, byteArrayOutputStream.toByteArray());
            }
            byteArrayOutputStream.close();
            zipEntry = zipInputStream.getNextZipEntry();
        }
        zipInputStream.close();

        return UnzipSkanningmetadataUtils.pairFiles(pdfs, xmls);
    }
}
