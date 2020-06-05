package no.nav.skanmotreferansenr.unzipskanningmetadata;

import no.nav.skanmotreferansenr.domain.Filepair;
import no.nav.skanmotreferansenr.domain.FilepairWithMetadata;
import no.nav.skanmotreferansenr.domain.Journalpost;
import no.nav.skanmotreferansenr.domain.Skanningmetadata;
import no.nav.skanmotreferansenr.exceptions.functional.SkanmotreferansenrUnzipperFunctionalException;
import no.nav.skanmotreferansenr.utils.Utils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public class UnzipSkanningmetadataUtils {

    public static List<Filepair> pairFiles(Map<String, byte[]> pdfs, Map<String, byte[]> xmls) {
        return pdfs.keySet().stream().map(pdfName ->
                Filepair.builder()
                        .name(Utils.removeFileExtensionInFilename(pdfName))
                        .pdf(pdfs.get(pdfName))
                        .xml(xmls.get(Utils.changeFiletypeInFilename(pdfName, "xml")))
                        .build()
        ).collect(Collectors.toList());
    }

    public static FilepairWithMetadata extractMetadata(Filepair filepair) {
        return FilepairWithMetadata.builder()
                .skanningmetadata(bytesToSkanningmetadata(filepair.getXml()))
                .pdf(filepair.getPdf())
                .xml(filepair.getXml())
                .build();
    }

    public static Skanningmetadata bytesToSkanningmetadata(byte[] bytes) {
        try {
            JAXBContext jaxbContext;
            jaxbContext = JAXBContext.newInstance(Skanningmetadata.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            XMLStreamReader xmlStreamReader = new MetadataStreamReaderDelegate(xmlInputFactory.createXMLStreamReader(new ByteArrayInputStream(bytes)));

            Skanningmetadata skanningmetadata = (Skanningmetadata) jaxbUnmarshaller.unmarshal(xmlStreamReader);

            skanningmetadata.verifyFields();

            return splitChecksumInReferansenummer(skanningmetadata);
        } catch (JAXBException | XMLStreamException e) {
            throw new SkanmotreferansenrUnzipperFunctionalException("Skanmotreferansenr klarte ikke lese metadata i zipfil", e);
        } catch (NullPointerException e) {
            throw new SkanmotreferansenrUnzipperFunctionalException("Xml fil mangler");
        }
    }

    public static String getFileType(ZipEntry file) {
        return file.getName().substring(file.getName().lastIndexOf(".") + 1);
    }

    // IM sender referansenr med checksum (Luhns algoritme), de er lagret uten checksummen i foerstesidedb, vi ønsker å inkludere checksummen i dokarkiv (og senere i foerstesidedb)
    public static Skanningmetadata splitChecksumInReferansenummer(Skanningmetadata old) {
        String referansenummer = old.getJournalpost().getReferansenummer();
        return Skanningmetadata.builder()
                .journalpost(Journalpost.builder()
                        .mottakskanal(old.getJournalpost().getMottakskanal())
                        .datoMottatt(old.getJournalpost().getDatoMottatt())
                        .batchNavn(old.getJournalpost().getBatchNavn())
                        .filNavn(old.getJournalpost().getFilNavn())
                        .endorsernr(old.getJournalpost().getEndorsernr())
                        .referansenummer(referansenummer.substring(0, 13))
                        .referansenrChecksum(referansenummer.substring(13))
                        .build())
                .skanningInfo(old.getSkanningInfo())
                .build();
    }
}
