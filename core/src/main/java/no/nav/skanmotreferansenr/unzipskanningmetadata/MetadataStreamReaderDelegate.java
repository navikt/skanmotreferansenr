package no.nav.skanmotreferansenr.unzipskanningmetadata;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;


// Used to parse xml attributes in case insensitive manner

class MetadataStreamReaderDelegate extends StreamReaderDelegate {

    public MetadataStreamReaderDelegate(XMLStreamReader xsr) {
        super(xsr);
    }

    @Override
    public String getAttributeLocalName(int index) {
        return super.getAttributeLocalName(index).toLowerCase();
    }

    @Override
    public String getLocalName() {
        return super.getLocalName().toLowerCase();
    }

}