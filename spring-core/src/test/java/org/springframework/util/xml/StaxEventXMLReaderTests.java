/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
		ContentHandler contentHandler = mock(ContentHandler.class);
		xmlReader.setContentHandler(contentHandler);
		xmlReader.parse(new InputSource());
		verify(contentHandler).startDocument();
		verify(contentHandler).startElement(eq("http://springframework.org/spring-ws"), eq("child"), eq("child"), any(Attributes.class));
		verify(contentHandler).endElement("http://springframework.org/spring-ws", "child", "child");
		verify(contentHandler).endDocument();
	}

}
