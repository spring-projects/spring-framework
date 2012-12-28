/*
 * Copyright 2002-2009 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.easymock.AbstractMatcher;
import org.easymock.MockControl;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLReaderFactory;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public abstract class AbstractStaxXMLReaderTestCase {

	protected static XMLInputFactory inputFactory;

	private XMLReader standardReader;

	private MockControl contentHandlerControl;

	private ContentHandler contentHandler;

	@Before
	public void setUp() throws Exception {
		inputFactory = XMLInputFactory.newInstance();
		standardReader = XMLReaderFactory.createXMLReader();
		contentHandlerControl = MockControl.createStrictControl(ContentHandler.class);
		contentHandlerControl.setDefaultMatcher(new SaxArgumentMatcher());
		ContentHandler contentHandlerMock = (ContentHandler) contentHandlerControl.getMock();
		contentHandler = new CopyingContentHandler(contentHandlerMock);
		standardReader.setContentHandler(contentHandler);
	}

	private InputStream createTestInputStream() {
		return getClass().getResourceAsStream("testContentHandler.xml");
	}

	@Test
	public void contentHandlerNamespacesNoPrefixes() throws SAXException, IOException, XMLStreamException {
		standardReader.setFeature("http://xml.org/sax/features/namespaces", true);
		standardReader.setFeature("http://xml.org/sax/features/namespace-prefixes", false);

		standardReader.parse(new InputSource(createTestInputStream()));
		contentHandlerControl.replay();

		AbstractStaxXMLReader staxXmlReader = createStaxXmlReader(createTestInputStream());
		staxXmlReader.setFeature("http://xml.org/sax/features/namespaces", true);
		staxXmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", false);

		staxXmlReader.setContentHandler(contentHandler);
		staxXmlReader.parse(new InputSource());
		contentHandlerControl.verify();
	}

	@Test
	public void contentHandlerNamespacesPrefixes() throws SAXException, IOException, XMLStreamException {
		standardReader.setFeature("http://xml.org/sax/features/namespaces", true);
		standardReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);

		standardReader.parse(new InputSource(createTestInputStream()));
		contentHandlerControl.replay();

		AbstractStaxXMLReader staxXmlReader = createStaxXmlReader(createTestInputStream());
		staxXmlReader.setFeature("http://xml.org/sax/features/namespaces", true);
		staxXmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);

		staxXmlReader.setContentHandler(contentHandler);
		staxXmlReader.parse(new InputSource());
		contentHandlerControl.verify();
	}

	@Test
	public void contentHandlerNoNamespacesPrefixes() throws SAXException, IOException, XMLStreamException {
		standardReader.setFeature("http://xml.org/sax/features/namespaces", false);
		standardReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);

		standardReader.parse(new InputSource(createTestInputStream()));
		contentHandlerControl.replay();

		AbstractStaxXMLReader staxXmlReader = createStaxXmlReader(createTestInputStream());
		staxXmlReader.setFeature("http://xml.org/sax/features/namespaces", false);
		staxXmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);

		staxXmlReader.setContentHandler(contentHandler);
		staxXmlReader.parse(new InputSource());
		contentHandlerControl.verify();
	}

	@Test
	public void lexicalHandler() throws SAXException, IOException, XMLStreamException {
		MockControl lexicalHandlerControl = MockControl.createStrictControl(LexicalHandler.class);
		lexicalHandlerControl.setDefaultMatcher(new SaxArgumentMatcher());
		LexicalHandler lexicalHandlerMock = (LexicalHandler) lexicalHandlerControl.getMock();
		LexicalHandler lexicalHandler = new CopyingLexicalHandler(lexicalHandlerMock);

		Resource testLexicalHandlerXml = new ClassPathResource("testLexicalHandler.xml", getClass());

		standardReader.setContentHandler(null);
		standardReader.setProperty("http://xml.org/sax/properties/lexical-handler", lexicalHandler);
		standardReader.parse(new InputSource(testLexicalHandlerXml.getInputStream()));
		lexicalHandlerControl.replay();

		inputFactory.setProperty("javax.xml.stream.isCoalescing", Boolean.FALSE);
		inputFactory.setProperty("http://java.sun.com/xml/stream/properties/report-cdata-event", Boolean.TRUE);
		inputFactory.setProperty("javax.xml.stream.isReplacingEntityReferences", Boolean.FALSE);
		inputFactory.setProperty("javax.xml.stream.isSupportingExternalEntities", Boolean.FALSE);

		AbstractStaxXMLReader staxXmlReader = createStaxXmlReader(testLexicalHandlerXml.getInputStream());

		staxXmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", lexicalHandler);
		staxXmlReader.parse(new InputSource());
		lexicalHandlerControl.verify();
	}

	protected abstract AbstractStaxXMLReader createStaxXmlReader(InputStream inputStream) throws XMLStreamException;

	/** Easymock {@code AbstractMatcher} implementation that matches SAX arguments. */
	protected static class SaxArgumentMatcher extends AbstractMatcher {

		@Override
		public boolean matches(Object[] expected, Object[] actual) {
			if (expected == actual) {
				return true;
			}
			if (expected == null || actual == null) {
				return false;
			}
			if (expected.length != actual.length) {
				throw new IllegalArgumentException("Expected and actual arguments must have the same size");
			}
			if (expected.length == 3 && expected[0] instanceof char[] && expected[1] instanceof Integer &&
					expected[2] instanceof Integer) {
				// handling of the character(char[], int, int) methods
				String expectedString = new String((char[]) expected[0], (Integer) expected[1], (Integer) expected[2]);
				String actualString = new String((char[]) actual[0], (Integer) actual[1], (Integer) actual[2]);
				return expectedString.equals(actualString);
			}
			else if (expected.length == 1 && (expected[0] instanceof Locator)) {
				return true;
			}
			else {
				return super.matches(expected, actual);
			}
		}

		@Override
		protected boolean argumentMatches(Object expected, Object actual) {
			if (expected instanceof char[]) {
				return Arrays.equals((char[]) expected, (char[]) actual);
			}
			else if (expected instanceof Attributes) {
				Attributes expectedAttributes = (Attributes) expected;
				Attributes actualAttributes = (Attributes) actual;
				if (expectedAttributes.getLength() != actualAttributes.getLength()) {
					return false;
				}
				for (int i = 0; i < expectedAttributes.getLength(); i++) {
					boolean found = false;
					for (int j = 0; j < actualAttributes.getLength(); j++) {
						if (expectedAttributes.getURI(i).equals(actualAttributes.getURI(j)) &&
								expectedAttributes.getQName(i).equals(actualAttributes.getQName(j)) &&
								expectedAttributes.getType(i).equals(actualAttributes.getType(j)) &&
								expectedAttributes.getValue(i).equals(actualAttributes.getValue(j))) {
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
			else {
				return super.argumentMatches(expected, actual);
			}
		}

		@Override
		public String toString(Object[] arguments) {
			if (arguments != null && arguments.length == 3 && arguments[0] instanceof char[] &&
					arguments[1] instanceof Integer && arguments[2] instanceof Integer) {
				return new String((char[]) arguments[0], (Integer) arguments[1], (Integer) arguments[2]);
			}
			else {
				return super.toString(arguments);
			}
		}

		@Override
		protected String argumentToString(Object argument) {
			if (argument instanceof char[]) {
				char[] array = (char[]) argument;
				StringBuilder buffer = new StringBuilder();
				for (char anArray : array) {
					buffer.append(anArray);
				}
				return buffer.toString();
			}
			else if (argument instanceof Attributes) {
				Attributes attributes = (Attributes) argument;
				StringBuilder buffer = new StringBuilder("[");
				for (int i = 0; i < attributes.getLength(); i++) {
					if (attributes.getURI(i).length() != 0) {
						buffer.append('{');
						buffer.append(attributes.getURI(i));
						buffer.append('}');
					}
					if (attributes.getQName(i).length() != 0) {
						buffer.append(attributes.getQName(i));
					}
					buffer.append('=');
					buffer.append(attributes.getValue(i));
					if (i < attributes.getLength() - 1) {
						buffer.append(", ");
					}
				}
				buffer.append(']');
				return buffer.toString();
			}
			else if (argument instanceof Locator) {
				Locator locator = (Locator) argument;
				StringBuilder buffer = new StringBuilder("[");
				buffer.append(locator.getLineNumber());
				buffer.append(',');
				buffer.append(locator.getColumnNumber());
				buffer.append(']');
				return buffer.toString();
			}
			else {
				return super.argumentToString(argument);
			}
		}
	}

	private static class CopyingContentHandler implements ContentHandler {

		private final ContentHandler wrappee;

		private CopyingContentHandler(ContentHandler wrappee) {
			this.wrappee = wrappee;
		}

		public void setDocumentLocator(Locator locator) {
			wrappee.setDocumentLocator(locator);
		}

		public void startDocument() throws SAXException {
			wrappee.startDocument();
		}

		public void endDocument() throws SAXException {
			wrappee.endDocument();
		}

		public void startPrefixMapping(String prefix, String uri) throws SAXException {
			wrappee.startPrefixMapping(prefix, uri);
		}

		public void endPrefixMapping(String prefix) throws SAXException {
			wrappee.endPrefixMapping(prefix);
		}

		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			wrappee.startElement(uri, localName, qName, new AttributesImpl(attributes));
		}

		public void endElement(String uri, String localName, String qName) throws SAXException {
			wrappee.endElement(uri, localName, qName);
		}

		public void characters(char ch[], int start, int length) throws SAXException {
			wrappee.characters(copy(ch), start, length);
		}

		public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
		}

		public void processingInstruction(String target, String data) throws SAXException {
			wrappee.processingInstruction(target, data);
		}

		public void skippedEntity(String name) throws SAXException {
			wrappee.skippedEntity(name);
		}
	}

	private static class CopyingLexicalHandler implements LexicalHandler {

		private final LexicalHandler wrappee;

		private CopyingLexicalHandler(LexicalHandler wrappee) {
			this.wrappee = wrappee;
		}

		public void startDTD(String name, String publicId, String systemId) throws SAXException {
			wrappee.startDTD("element", publicId, systemId);
		}

		public void endDTD() throws SAXException {
			wrappee.endDTD();
		}

		public void startEntity(String name) throws SAXException {
			wrappee.startEntity(name);
		}

		public void endEntity(String name) throws SAXException {
			wrappee.endEntity(name);
		}

		public void startCDATA() throws SAXException {
			wrappee.startCDATA();
		}

		public void endCDATA() throws SAXException {
			wrappee.endCDATA();
		}

		public void comment(char ch[], int start, int length) throws SAXException {
			wrappee.comment(copy(ch), start, length);
		}
	}

	private static char[] copy(char[] ch) {
		char[] copy = new char[ch.length];
		System.arraycopy(ch, 0, copy, 0, ch.length);
		return copy;
	}

}
