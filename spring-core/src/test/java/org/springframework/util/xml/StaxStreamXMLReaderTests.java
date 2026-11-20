/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util.xml;

import java.io.InputStream;
import java.io.StringReader;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.Test;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class StaxStreamXMLReaderTests extends AbstractStaxXMLReaderTests {

	public static final String CONTENT = "<root xmlns='http://springframework.org/spring-ws'><child/></root>";

	@Override
	protected AbstractStaxXMLReader createStaxXmlReader(InputStream inputStream) throws XMLStreamException {
		return new StaxStreamXMLReader(inputFactory.createXMLStreamReader(inputStream));
	}

	@Test
	void partial() throws Exception {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(CONTENT));
		streamReader.nextTag();  // skip to root
		assertThat(streamReader.getName()).as("Invalid element").isEqualTo(new QName("http://springframework.org/spring-ws", "root"));
		streamReader.nextTag();  // skip to child
		assertThat(streamReader.getName()).as("Invalid element").isEqualTo(new QName("http://springframework.org/spring-ws", "child"));
		StaxStreamXMLReader xmlReader = new StaxStreamXMLReader(streamReader);

		ContentHandler contentHandler = mock();
		xmlReader.setContentHandler(contentHandler);
		xmlReader.parse(new InputSource());

		verify(contentHandler).setDocumentLocator(any(Locator.class));
		verify(contentHandler).startDocument();
		verify(contentHandler).startElement(eq("http://springframework.org/spring-ws"), eq("child"), eq("child"), any(Attributes.class));
		verify(contentHandler).endElement("http://springframework.org/spring-ws", "child", "child");
		verify(contentHandler).endDocument();
	}

}
