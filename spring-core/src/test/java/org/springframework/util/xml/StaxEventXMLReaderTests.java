/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.util.xml;

import java.io.InputStream;
import java.io.StringReader;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.AttributesImpl;

import static org.mockito.BDDMockito.*;

public class StaxEventXMLReaderTests extends AbstractStaxXMLReaderTestCase {

	public static final String CONTENT = "<root xmlns='http://springframework.org/spring-ws'><child/></root>";

	@Override
	protected AbstractStaxXMLReader createStaxXmlReader(InputStream inputStream) throws XMLStreamException {
		return new StaxEventXMLReader(inputFactory.createXMLEventReader(inputStream));
	}

	public void testPartial() throws Exception {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLEventReader eventReader = inputFactory.createXMLEventReader(new StringReader(CONTENT));
		eventReader.nextTag(); // skip to root
		StaxEventXMLReader xmlReader = new StaxEventXMLReader(eventReader);
		ContentHandler contentHandler = mock(ContentHandler.class);
		xmlReader.setContentHandler(contentHandler);
		xmlReader.parse(new InputSource());
		verify(contentHandler).startDocument();
		verify(contentHandler).startElement("http://springframework.org/spring-ws", "child", "child", new AttributesImpl());
		verify(contentHandler).endElement("http://springframework.org/spring-ws", "child", "child");
		verify(contentHandler).endDocument();
	}
}

