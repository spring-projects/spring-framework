/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util.xml;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import org.springframework.core.testfixture.xml.XmlContent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DomContentHandler}.
 */
class DomContentHandlerTests {

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


	@BeforeEach
	void setUp() throws Exception {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		documentBuilder = documentBuilderFactory.newDocumentBuilder();
		result = documentBuilder.newDocument();
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		saxParserFactory.setNamespaceAware(true);
		SAXParser saxParser = saxParserFactory.newSAXParser();
		xmlReader = saxParser.getXMLReader();
	}


	@Test
	void contentHandlerDocumentNamespacePrefixes() throws Exception {
		xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
		handler = new DomContentHandler(result);
		expected = documentBuilder.parse(new InputSource(new StringReader(XML_1)));
		xmlReader.setContentHandler(handler);
		xmlReader.parse(new InputSource(new StringReader(XML_1)));
		assertThat(XmlContent.of(result)).as("Invalid result").isSimilarTo(expected);
	}

	@Test
	void contentHandlerDocumentNoNamespacePrefixes() throws Exception {
		handler = new DomContentHandler(result);
		expected = documentBuilder.parse(new InputSource(new StringReader(XML_1)));
		xmlReader.setContentHandler(handler);
		xmlReader.parse(new InputSource(new StringReader(XML_1)));
		assertThat(XmlContent.of(result)).as("Invalid result").isSimilarTo(expected);
	}

	@Test
	void contentHandlerElement() throws Exception {
		Element rootElement = result.createElementNS("namespace", "root");
		result.appendChild(rootElement);
		handler = new DomContentHandler(rootElement);
		expected = documentBuilder.parse(new InputSource(new StringReader(XML_2_EXPECTED)));
		xmlReader.setContentHandler(handler);
		xmlReader.parse(new InputSource(new StringReader(XML_2_SNIPPET)));
		assertThat(XmlContent.of(result)).as("Invalid result").isSimilarTo(expected);
	}

}
