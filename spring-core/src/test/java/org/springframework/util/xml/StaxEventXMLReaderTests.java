/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util.xml;

import java.io.InputStream;
import java.io.StringReader;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.api.Test;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class StaxEventXMLReaderTests extends AbstractStaxXMLReaderTests {

	public static final String CONTENT = "<root xmlns='http://springframework.org/spring-ws'><child/></root>";

	@Override
	protected AbstractStaxXMLReader createStaxXmlReader(InputStream inputStream) throws XMLStreamException {
		return new StaxEventXMLReader(inputFactory.createXMLEventReader(inputStream));
	}

	@Test
	void partial() throws Exception {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLEventReader eventReader = inputFactory.createXMLEventReader(new StringReader(CONTENT));
		eventReader.nextTag();  // skip to root
		StaxEventXMLReader xmlReader = new StaxEventXMLReader(eventReader);
		ContentHandler contentHandler = mock();
		xmlReader.setContentHandler(contentHandler);
		xmlReader.parse(new InputSource());
		verify(contentHandler).startDocument();
		verify(contentHandler).startElement(eq("http://springframework.org/spring-ws"), eq("child"), eq("child"), any(Attributes.class));
		verify(contentHandler).endElement("http://springframework.org/spring-ws", "child", "child");
		verify(contentHandler).endDocument();
	}

}
