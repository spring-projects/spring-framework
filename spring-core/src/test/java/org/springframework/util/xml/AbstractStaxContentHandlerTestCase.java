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

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import javax.xml.stream.XMLStreamException;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public abstract class AbstractStaxContentHandlerTestCase {

	private static final String XML_CONTENT_HANDLER =
			"<?xml version='1.0' encoding='UTF-8'?><?pi content?><root xmlns='namespace'><prefix:child xmlns:prefix='namespace2' prefix:attr='value'>content</prefix:child></root>"
			;

	private XMLReader xmlReader;

	@Before
	public void createXMLReader() throws Exception {
		xmlReader = XMLReaderFactory.createXMLReader();
	}

	@Test
	public void contentHandler() throws Exception {
		StringWriter stringWriter = new StringWriter();
		AbstractStaxContentHandler handler = createStaxContentHandler(stringWriter);
		xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
		xmlReader.setContentHandler(handler);
		xmlReader.parse(new InputSource(new StringReader(XML_CONTENT_HANDLER)));
		assertXMLEqual("Invalid result", XML_CONTENT_HANDLER, stringWriter.toString());
	}

	@Test
	public void contentHandlerNamespacePrefixes() throws Exception {
		StringWriter stringWriter = new StringWriter();
		AbstractStaxContentHandler handler = createStaxContentHandler(stringWriter);
		xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
		xmlReader.setContentHandler(handler);
		xmlReader.parse(new InputSource(new StringReader(XML_CONTENT_HANDLER)));
		assertXMLEqual("Invalid result", XML_CONTENT_HANDLER, stringWriter.toString());
	}

	protected abstract AbstractStaxContentHandler createStaxContentHandler(Writer writer) throws XMLStreamException;

}
