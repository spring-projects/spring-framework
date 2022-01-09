/*
 * Copyright 2002-2021 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.tests.MockitoUtils;
import org.springframework.tests.MockitoUtils.InvocationArgumentsAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

/**
 * @author Arjen Poutsma
 */
abstract class AbstractStaxXMLReaderTests {

	protected static XMLInputFactory inputFactory;

	private XMLReader standardReader;

	private ContentHandler standardContentHandler;


	@BeforeEach
	@SuppressWarnings("deprecation")  // on JDK 9
	void setUp() throws Exception {
		inputFactory = XMLInputFactory.newInstance();
		standardReader = org.xml.sax.helpers.XMLReaderFactory.createXMLReader();
		standardContentHandler = mockContentHandler();
		standardReader.setContentHandler(standardContentHandler);
	}


	@Test
	void contentHandlerNamespacesNoPrefixes() throws Exception {
		standardReader.setFeature("http://xml.org/sax/features/namespaces", true);
		standardReader.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
		standardReader.parse(new InputSource(createTestInputStream()));

		AbstractStaxXMLReader staxXmlReader = createStaxXmlReader(createTestInputStream());
		ContentHandler contentHandler = mockContentHandler();
		staxXmlReader.setFeature("http://xml.org/sax/features/namespaces", true);
		staxXmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
		staxXmlReader.setContentHandler(contentHandler);
		staxXmlReader.parse(new InputSource());

		verifyIdenticalInvocations(standardContentHandler, contentHandler);
	}

	@Test
	void contentHandlerNamespacesPrefixes() throws Exception {
		standardReader.setFeature("http://xml.org/sax/features/namespaces", true);
		standardReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
		standardReader.parse(new InputSource(createTestInputStream()));

		AbstractStaxXMLReader staxXmlReader = createStaxXmlReader(createTestInputStream());
		ContentHandler contentHandler = mockContentHandler();
		staxXmlReader.setFeature("http://xml.org/sax/features/namespaces", true);
		staxXmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
		staxXmlReader.setContentHandler(contentHandler);
		staxXmlReader.parse(new InputSource());

		verifyIdenticalInvocations(standardContentHandler, contentHandler);
	}

	@Test
	void contentHandlerNoNamespacesPrefixes() throws Exception {
		standardReader.setFeature("http://xml.org/sax/features/namespaces", false);
		standardReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
		standardReader.parse(new InputSource(createTestInputStream()));

		AbstractStaxXMLReader staxXmlReader = createStaxXmlReader(createTestInputStream());
		ContentHandler contentHandler = mockContentHandler();
		staxXmlReader.setFeature("http://xml.org/sax/features/namespaces", false);
		staxXmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
		staxXmlReader.setContentHandler(contentHandler);
		staxXmlReader.parse(new InputSource());

		verifyIdenticalInvocations(standardContentHandler, contentHandler);
	}

	@Test
	void whitespace() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><test><node1> </node1><node2> Some text </node2></test>";

		Transformer transformer = TransformerFactory.newInstance().newTransformer();

		AbstractStaxXMLReader staxXmlReader = createStaxXmlReader(
				new ByteArrayInputStream(xml.getBytes("UTF-8")));

		SAXSource source = new SAXSource(staxXmlReader, new InputSource());
		DOMResult result = new DOMResult();

		transformer.transform(source, result);

		Node node1 = result.getNode().getFirstChild().getFirstChild();
		assertThat(node1.getTextContent()).isEqualTo(" ");
		assertThat(node1.getNextSibling().getTextContent()).isEqualTo(" Some text ");
	}

	@Test
	void lexicalHandler() throws Exception {
		Resource testLexicalHandlerXml = new ClassPathResource("testLexicalHandler.xml", getClass());

		LexicalHandler expectedLexicalHandler = mockLexicalHandler();
		standardReader.setContentHandler(null);
		standardReader.setProperty("http://xml.org/sax/properties/lexical-handler", expectedLexicalHandler);
		standardReader.parse(new InputSource(testLexicalHandlerXml.getInputStream()));
		inputFactory.setProperty("javax.xml.stream.isCoalescing", Boolean.FALSE);
		inputFactory.setProperty("http://java.sun.com/xml/stream/properties/report-cdata-event", Boolean.TRUE);
		inputFactory.setProperty("javax.xml.stream.isReplacingEntityReferences", Boolean.FALSE);
		inputFactory.setProperty("javax.xml.stream.isSupportingExternalEntities", Boolean.FALSE);

		LexicalHandler actualLexicalHandler = mockLexicalHandler();
		willAnswer(invocation -> invocation.getArguments()[0] = "element").
				given(actualLexicalHandler).startDTD(anyString(), anyString(), anyString());
		AbstractStaxXMLReader staxXmlReader = createStaxXmlReader(testLexicalHandlerXml.getInputStream());
		staxXmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", actualLexicalHandler);
		staxXmlReader.parse(new InputSource());

		// TODO: broken comparison since Mockito 2.2 upgrade
		// verifyIdenticalInvocations(expectedLexicalHandler, actualLexicalHandler);
	}


	private LexicalHandler mockLexicalHandler() throws Exception {
		LexicalHandler lexicalHandler = mock(LexicalHandler.class);
		willAnswer(new CopyCharsAnswer()).given(lexicalHandler).comment(any(char[].class), anyInt(), anyInt());
		return lexicalHandler;
	}

	private InputStream createTestInputStream() {
		return getClass().getResourceAsStream("testContentHandler.xml");
	}

	protected final ContentHandler mockContentHandler() throws Exception {
		ContentHandler contentHandler = mock(ContentHandler.class);
		willAnswer(new CopyCharsAnswer()).given(contentHandler).characters(any(char[].class), anyInt(), anyInt());
		willAnswer(new CopyCharsAnswer()).given(contentHandler).ignorableWhitespace(any(char[].class), anyInt(), anyInt());
		willAnswer(invocation -> {
			invocation.getArguments()[3] = new AttributesImpl((Attributes) invocation.getArguments()[3]);
			return null;
		}).given(contentHandler).startElement(anyString(), anyString(), anyString(), any(Attributes.class));
		return contentHandler;
	}

	protected <T> void verifyIdenticalInvocations(T expected, T actual) {
		MockitoUtils.verifySameInvocations(expected, actual,
				new SkipLocatorArgumentsAdapter(), new CharArrayToStringAdapter(), new PartialAttributesAdapter());
	}

	protected abstract AbstractStaxXMLReader createStaxXmlReader(InputStream inputStream) throws XMLStreamException;


	private static class SkipLocatorArgumentsAdapter implements InvocationArgumentsAdapter {

		@Override
		public Object[] adaptArguments(Object[] arguments) {
			for (int i = 0; i < arguments.length; i++) {
				if (arguments[i] instanceof Locator) {
					arguments[i] = null;
				}
			}
			return arguments;
		}
	}


	private static class CharArrayToStringAdapter implements InvocationArgumentsAdapter {

		@Override
		public Object[] adaptArguments(Object[] arguments) {
			if (arguments.length == 3 && arguments[0] instanceof char[]
					&& arguments[1] instanceof Integer && arguments[2] instanceof Integer) {
				return new Object[] {new String((char[]) arguments[0], (Integer) arguments[1], (Integer) arguments[2])};
			}
			return arguments;
		}
	}


	private static class PartialAttributesAdapter implements InvocationArgumentsAdapter {

		@Override
		public Object[] adaptArguments(Object[] arguments) {
			for (int i = 0; i < arguments.length; i++) {
				if (arguments[i] instanceof Attributes) {
					arguments[i] = new PartialAttributes((Attributes) arguments[i]);
				}
			}
			return arguments;
		}
	}


	private static class CopyCharsAnswer implements Answer<Object> {

		@Override
		public Object answer(InvocationOnMock invocation) throws Throwable {
			char[] chars = (char[]) invocation.getArguments()[0];
			char[] copy = new char[chars.length];
			System.arraycopy(chars, 0, copy, 0, chars.length);
			invocation.getArguments()[0] = copy;
			return null;
		}
	}


	private static class PartialAttributes {

		private final Attributes attributes;

		public PartialAttributes(Attributes attributes) {
			this.attributes = attributes;
		}

		@Override
		public boolean equals(Object obj) {
			Attributes other = ((PartialAttributes) obj).attributes;
			if (this.attributes.getLength() != other.getLength()) {
				return false;
			}
			for (int i = 0; i < other.getLength(); i++) {
				boolean found = false;
				for (int j = 0; j < attributes.getLength(); j++) {
					if (other.getURI(i).equals(attributes.getURI(j))
							&& other.getQName(i).equals(attributes.getQName(j))
							&& other.getType(i).equals(attributes.getType(j))
							&& other.getValue(i).equals(attributes.getValue(j))) {
						found = true;
						break;
					}
				}
				if (!found) {
					return false;
				}
			}
			return true;
		}

		@Override
		public int hashCode() {
			return 1;
		}
	}

}
