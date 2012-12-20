/*
 * Copyright 2002-2012 the original author or authors.
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
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.easymock.MockControl;
import static org.junit.Assert.*;
import org.junit.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.AttributesImpl;

public class StaxStreamXMLReaderTests extends AbstractStaxXMLReaderTestCase {

	public static final String CONTENT = "<root xmlns='http://springframework.org/spring-ws'><child/></root>";

	@Override
	protected AbstractStaxXMLReader createStaxXmlReader(InputStream inputStream) throws XMLStreamException {
		return new StaxStreamXMLReader(inputFactory.createXMLStreamReader(inputStream));
	}

	@Test
	public void testPartial() throws Exception {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(CONTENT));
		streamReader.nextTag(); // skip to root
		assertEquals("Invalid element", new QName("http://springframework.org/spring-ws", "root"),
				streamReader.getName());
		streamReader.nextTag(); // skip to child
		assertEquals("Invalid element", new QName("http://springframework.org/spring-ws", "child"),
				streamReader.getName());
		StaxStreamXMLReader xmlReader = new StaxStreamXMLReader(streamReader);

		MockControl mockControl = MockControl.createStrictControl(ContentHandler.class);
		mockControl.setDefaultMatcher(new SaxArgumentMatcher());
		ContentHandler contentHandlerMock = (ContentHandler) mockControl.getMock();

		contentHandlerMock.setDocumentLocator(null);
		mockControl.setMatcher(MockControl.ALWAYS_MATCHER);
		contentHandlerMock.startDocument();
		contentHandlerMock.startElement("http://springframework.org/spring-ws", "child", "child", new AttributesImpl());
		contentHandlerMock.endElement("http://springframework.org/spring-ws", "child", "child");
		contentHandlerMock.endDocument();

		xmlReader.setContentHandler(contentHandlerMock);
		mockControl.replay();
		xmlReader.parse(new InputSource());
		mockControl.verify();
	}


}
