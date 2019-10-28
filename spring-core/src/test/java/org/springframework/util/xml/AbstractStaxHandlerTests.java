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

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xmlunit.util.Predicate;

import org.springframework.tests.XmlContent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
abstract class AbstractStaxHandlerTests {

	private static final String COMPLEX_XML =
			"<?xml version='1.0' encoding='UTF-8'?>" +
					"<!DOCTYPE beans PUBLIC \"-//SPRING//DTD BEAN 2.0//EN\" \"https://www.springframework.org/dtd/spring-beans-2.0.dtd\">" +
					"<?pi content?><root xmlns='namespace'><prefix:child xmlns:prefix='namespace2' prefix:attr='value'>characters <![CDATA[cdata]]></prefix:child>" +
					"<!-- comment -->" +
					"</root>";

	private static final String SIMPLE_XML = "<?xml version='1.0' encoding='UTF-8'?>" +
					"<?pi content?><root xmlns='namespace'><prefix:child xmlns:prefix='namespace2' prefix:attr='value'>content</prefix:child>" +
					"</root>";

	private static final Predicate<Node> nodeFilter = (n -> n.getNodeType() != Node.COMMENT_NODE &&
			n.getNodeType() != Node.DOCUMENT_TYPE_NODE && n.getNodeType() != Node.PROCESSING_INSTRUCTION_NODE);


	private XMLReader xmlReader;


	@BeforeEach
	@SuppressWarnings("deprecation")  // on JDK 9
	void createXMLReader() throws Exception {
		xmlReader = org.xml.sax.helpers.XMLReaderFactory.createXMLReader();
		xmlReader.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
	}


	@Test
	void noNamespacePrefixes() throws Exception {
		StringWriter stringWriter = new StringWriter();
		AbstractStaxHandler handler = createStaxHandler(new StreamResult(stringWriter));
		xmlReader.setContentHandler(handler);
		xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);

		xmlReader.setFeature("http://xml.org/sax/features/namespaces", true);
		xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", false);

		xmlReader.parse(new InputSource(new StringReader(COMPLEX_XML)));

		assertThat(XmlContent.from(stringWriter)).isSimilarTo(COMPLEX_XML, nodeFilter);
	}

	@Test
	void namespacePrefixes() throws Exception {
		StringWriter stringWriter = new StringWriter();
		AbstractStaxHandler handler = createStaxHandler(new StreamResult(stringWriter));
		xmlReader.setContentHandler(handler);
		xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);

		xmlReader.setFeature("http://xml.org/sax/features/namespaces", true);
		xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);

		xmlReader.parse(new InputSource(new StringReader(COMPLEX_XML)));

		assertThat(XmlContent.from(stringWriter)).isSimilarTo(COMPLEX_XML, nodeFilter);
	}

	@Test
	void noNamespacePrefixesDom() throws Exception {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

		Document expected = documentBuilder.parse(new InputSource(new StringReader(SIMPLE_XML)));

		Document result = documentBuilder.newDocument();
		AbstractStaxHandler handler = createStaxHandler(new DOMResult(result));
		xmlReader.setContentHandler(handler);
		xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);

		xmlReader.setFeature("http://xml.org/sax/features/namespaces", true);
		xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", false);

		xmlReader.parse(new InputSource(new StringReader(SIMPLE_XML)));

		assertThat(XmlContent.of(result)).isSimilarTo(expected, nodeFilter);
	}

	@Test
	void namespacePrefixesDom() throws Exception {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

		Document expected = documentBuilder.parse(new InputSource(new StringReader(SIMPLE_XML)));

		Document result = documentBuilder.newDocument();
		AbstractStaxHandler handler = createStaxHandler(new DOMResult(result));
		xmlReader.setContentHandler(handler);
		xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);

		xmlReader.setFeature("http://xml.org/sax/features/namespaces", true);
		xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);

		xmlReader.parse(new InputSource(new StringReader(SIMPLE_XML)));

		assertThat(XmlContent.of(result)).isSimilarTo(expected, nodeFilter);
	}

	protected abstract AbstractStaxHandler createStaxHandler(Result result) throws XMLStreamException;

}
