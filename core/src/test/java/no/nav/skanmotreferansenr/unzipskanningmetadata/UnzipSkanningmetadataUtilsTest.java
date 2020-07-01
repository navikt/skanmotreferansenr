package no.nav.skanmotreferansenr.unzipskanningmetadata;

import no.nav.skanmotreferansenr.domain.Filepair;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class UnzipSkanningmetadataUtilsTest {

    private static byte[] XML_FIL_1 = "xmlfil1".getBytes();
    private static byte[] XML_FIL_2 = "xmlfil2".getBytes();
    private static byte[] PDF_FIL_1 = "pdffil1".getBytes();
    private static byte[] PDF_FIL_2 = "pdffil1".getBytes();
    private static String NAVN_1 = "1";
    private static String NAVN_2 = "2";

    @Test
    public void shouldPairXmlPdfOK() {
        Map<String, byte[]> pdfs = Map.of(NAVN_1 + ".pdf", PDF_FIL_1, NAVN_2 + ".pdf", PDF_FIL_2);
        Map<String, byte[]> xmls = Map.of(NAVN_1 + ".xml", XML_FIL_1, NAVN_2 + ".xml", XML_FIL_2);
        List<Filepair> filepairs = UnzipSkanningmetadataUtils.pairFiles(pdfs, xmls);
        assertEquals(2, filepairs.size());
        assertEquals(NAVN_1, filepairs.get(0).getName());
        assertEquals(XML_FIL_1, filepairs.get(0).getXml());
        assertEquals(PDF_FIL_1, filepairs.get(0).getPdf());
        assertEquals(NAVN_2, filepairs.get(1).getName());
        assertEquals(XML_FIL_2, filepairs.get(1).getXml());
        assertEquals(PDF_FIL_2, filepairs.get(1).getPdf());
    }

    @Test
    public void shouldPairXmlPdfMissingPdf() {
        Map<String, byte[]> pdfs = Map.of(NAVN_1 + ".pdf", PDF_FIL_1, NAVN_2 + ".pdf", PDF_FIL_2);
        Map<String, byte[]> xmls = Map.of(NAVN_2 + ".xml", XML_FIL_2);
        List<Filepair> filepairs = UnzipSkanningmetadataUtils.pairFiles(pdfs, xmls);
        assertEquals(2, filepairs.size());
        assertEquals(NAVN_1, filepairs.get(0).getName());
        assertNull(filepairs.get(0).getXml());
        assertEquals(PDF_FIL_1, filepairs.get(0).getPdf());
        assertEquals(NAVN_2, filepairs.get(1).getName());
        assertEquals(XML_FIL_2, filepairs.get(1).getXml());
        assertEquals(PDF_FIL_2 ,filepairs.get(1).getPdf());
    }

    @Test
    public void shouldPairXmlPdfMissingXml() {
        Map<String, byte[]> pdfs = Map.of(NAVN_1 + ".pdf", PDF_FIL_1);
        Map<String, byte[]> xmls = Map.of(NAVN_1 + ".xml", XML_FIL_1, NAVN_2 + ".xml", XML_FIL_2);
        List<Filepair> filepairs = UnzipSkanningmetadataUtils.pairFiles(pdfs, xmls);
        assertEquals(2, filepairs.size());
        assertEquals(NAVN_1, filepairs.get(0).getName());
        assertEquals(XML_FIL_1 ,filepairs.get(0).getXml());
        assertEquals(PDF_FIL_1, filepairs.get(0).getPdf());
        assertEquals(NAVN_2, filepairs.get(1).getName());
        assertEquals(XML_FIL_2, filepairs.get(1).getXml());
        assertNull(filepairs.get(1).getPdf());
    }

}
