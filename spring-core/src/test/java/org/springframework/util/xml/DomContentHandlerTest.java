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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class DomContentHandlerTest {

	private static final String XML_1 =
			"<?xml version='1.0' encoding='UTF-8'?>" + "<?pi content?>" + "<root xmlns='namespace'>" +
					"<prefix:child xmlns:prefix='namespace2' xmlns:prefix2='namespace3' prefix2:attr='value'>content</prefix:child>" +
					"</root>";

	private static final String XML_2_EXPECTED =
			"<?xml version='1.0' encoding='UTF-8'?>" + "<root xmlns='namespace'>" + "<child xmlns='namespace2' />" +
					"</root>";

	private static final String XML_2_SNIPPET =
			"<?xml version='1.0' encoding='UTF-8'?>" + "<child xmlns='namespace2' />";

	private Document expected;

	private DomContentHandler handler;

	private Document result;

	private XMLReader xmlReader;

	private DocumentBuilder documentBuilder;

	@Before
	public void setUp() throws Exception {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		documentBuilder = documentBuilderFactory.newDocumentBuilder();
		result = documentBuilder.newDocument();
		xmlReader = XMLReaderFactory.createXMLReader();
	}

	@Test
	public void contentHandlerDocumentNamespacePrefixes() throws Exception {
		xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
		handler = new DomContentHandler(result);
		expected = documentBuilder.parse(new InputSource(new StringReader(XML_1)));
		xmlReader.setContentHandler(handler);
		xmlReader.parse(new InputSource(new StringReader(XML_1)));
		assertXMLEqual("Invalid result", expected, result);
	}

	@Test
	public void contentHandlerDocumentNoNamespacePrefixes() throws Exception {
		handler = new DomContentHandler(result);
		expected = documentBuilder.parse(new InputSource(new StringReader(XML_1)));
		xmlReader.setContentHandler(handler);
		xmlReader.parse(new InputSource(new StringReader(XML_1)));
		assertXMLEqual("Invalid result", expected, result);
	}

	@Test
	public void contentHandlerElement() throws Exception {
		Element rootElement = result.createElementNS("namespace", "root");
		result.appendChild(rootElement);
		handler = new DomContentHandler(rootElement);
		expected = documentBuilder.parse(new InputSource(new StringReader(XML_2_EXPECTED)));
		xmlReader.setContentHandler(handler);
		xmlReader.parse(new InputSource(new StringReader(XML_2_SNIPPET)));
		assertXMLEqual("Invalid result", expected, result);

	}
}
