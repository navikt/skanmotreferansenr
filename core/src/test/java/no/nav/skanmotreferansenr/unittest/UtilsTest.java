package no.nav.skanmotreferansenr.unittest;

import no.nav.skanmotreferansenr.utils.Utils;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UtilsTest {

    private final String PDF_NAME = "mockDokument-1.pdf";
    private final String XML_NAME = "mockDokument-1.xml";

    @Test
    public void shouldConvertPdfFilenameToXmlFilename() {
        String xmlName = Utils.changeFiletypeInFilename(PDF_NAME, "xml");
        assertEquals(XML_NAME, xmlName);
    }
}
