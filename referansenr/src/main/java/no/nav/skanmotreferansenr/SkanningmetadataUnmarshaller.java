package no.nav.skanmotreferansenr;

import no.nav.skanmotreferansenr.domain.Skanningmetadata;
import no.nav.skanmotreferansenr.exceptions.functional.InvalidMetadataException;
import no.nav.skanmotreferansenr.exceptions.technical.SkanmotreferansenrTechnicalException;
import no.nav.skanmotreferansenr.referansenr.PostboksReferansenrEnvelope;
import org.apache.camel.Body;
import org.apache.camel.Handler;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;

import static javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD;
import static javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA;
import static javax.xml.stream.XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES;
import static javax.xml.stream.XMLInputFactory.SUPPORT_DTD;
import static javax.xml.stream.XMLInputFactory.newInstance;
import static javax.xml.validation.SchemaFactory.newDefaultInstance;

public class SkanningmetadataUnmarshaller {

	private final JAXBContext jaxbContext;

	public SkanningmetadataUnmarshaller() {
		try {
			this.jaxbContext = JAXBContext.newInstance(Skanningmetadata.class);
		} catch (JAXBException e) {
			throw new SkanmotreferansenrTechnicalException("Klarte ikke instansiere JAXBContext", e);
		}
	}

	@Handler
	PostboksReferansenrEnvelope unmarshal(@Body PostboksReferansenrEnvelope envelope) {
		try {
			SchemaFactory schemaFactory = createXEEProtectedSchemaFactory();
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			jaxbUnmarshaller.setSchema(schemaFactory.newSchema(new StreamSource(this.getClass().getResourceAsStream("/postboks-referansenummer-3.0.0.xsd"))));
			XMLInputFactory xmlInputFactory = createXEEProtectedXMLInputFactory();
			XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(new ByteArrayInputStream(envelope.getXml()));
			final Skanningmetadata skanningmetadata = (Skanningmetadata) jaxbUnmarshaller.unmarshal(xmlStreamReader);
			envelope.setSkanningmetadata(skanningmetadata);
			return envelope;
		} catch (JAXBException | XMLStreamException | SAXException e) {
			final String message = ExceptionUtils.getRootCauseMessage(e);
			throw new InvalidMetadataException("Kunne ikke unmarshalle xml: " + message, e);
		}
	}

	// https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html
	private XMLInputFactory createXEEProtectedXMLInputFactory() {
		XMLInputFactory xmlInputFactory = newInstance();
		xmlInputFactory.setProperty(IS_SUPPORTING_EXTERNAL_ENTITIES, false);
		xmlInputFactory.setProperty(SUPPORT_DTD, false);
		return xmlInputFactory;
	}

	// https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html
	private SchemaFactory createXEEProtectedSchemaFactory() throws SAXNotRecognizedException, SAXNotSupportedException {
		SchemaFactory schemaFactory = newDefaultInstance();
		schemaFactory.setProperty(ACCESS_EXTERNAL_DTD, "");
		schemaFactory.setProperty(ACCESS_EXTERNAL_SCHEMA, "");
		return schemaFactory;
	}
}
