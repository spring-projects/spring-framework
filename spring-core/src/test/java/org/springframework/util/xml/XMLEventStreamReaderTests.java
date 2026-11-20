/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util.xml;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.w3c.dom.Node;
import org.xmlunit.util.Predicate;

import org.springframework.core.testfixture.xml.XmlContent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link XMLEventStreamReader}.
 */
class XMLEventStreamReaderTests {

	private static final String XML =
			"<?pi content?><root xmlns='namespace'><prefix:child xmlns:prefix='namespace2'>content</prefix:child></root>";

	private final XMLEventStreamReader streamReader;


	XMLEventStreamReaderTests() throws Exception {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLEventReader eventReader = inputFactory.createXMLEventReader(new StringReader(XML));
		this.streamReader = new XMLEventStreamReader(eventReader);
	}


	@Test
	void readAll() throws Exception {
		while (streamReader.hasNext()) {
			streamReader.next();
		}
	}

	@Test
	void readCorrect() throws Exception {
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		StAXSource source = new StAXSource(streamReader);
		StringWriter writer = new StringWriter();
		transformer.transform(source, new StreamResult(writer));
		Predicate<Node> nodeFilter = n ->
				n.getNodeType() != Node.DOCUMENT_TYPE_NODE && n.getNodeType() != Node.PROCESSING_INSTRUCTION_NODE;
		assertThat(XmlContent.from(writer)).isSimilarTo(XML, nodeFilter);
	}

	@ParameterizedTest  // gh-36914
	@CsvSource(textBlock = """
			0, content
			1, ontent
			4, ent
			6, t
			7, ''
			""")
	void getTextCharactersHonorsSourceStart(int sourceStart, String expected) throws Exception {
		advanceToCharacters();

		// text node is "content" (7 chars); request an oversized buffer to ensure
		// getTextCharacters(sourceStart, ...) does not read past the source
		char[] target = new char[10];
		int count = streamReader.getTextCharacters(sourceStart, target, 0, 10);

		assertThat(count).isEqualTo(expected.length());
		assertThat(new String(target, 0, count)).isEqualTo(expected);
	}

	private void advanceToCharacters() throws Exception {
		while (streamReader.getEventType() != XMLStreamConstants.CHARACTERS) {
			streamReader.next();
		}
	}

}
