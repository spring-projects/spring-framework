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

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Abstract base class for SAX {@code ContentHandler} implementations that use StAX as a basis. All methods
 * delegate to internal template methods, capable of throwing a {@code XMLStreamException}. Additionally, an
 * namespace context is used to keep track of declared namespaces.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
abstract class AbstractStaxContentHandler implements ContentHandler {

	private SimpleNamespaceContext namespaceContext = new SimpleNamespaceContext();

	private boolean namespaceContextChanged = false;

	@Override
	public final void startDocument() throws SAXException {
		namespaceContext.clear();
		namespaceContextChanged = false;
		try {
			startDocumentInternal();
		}
		catch (XMLStreamException ex) {
			throw new SAXException("Could not handle startDocument: " + ex.getMessage(), ex);
		}
	}

	protected abstract void startDocumentInternal() throws XMLStreamException;

	@Override
	public final void endDocument() throws SAXException {
		namespaceContext.clear();
		namespaceContextChanged = false;
		try {
			endDocumentInternal();
		}
		catch (XMLStreamException ex) {
			throw new SAXException("Could not handle startDocument: " + ex.getMessage(), ex);
		}
	}

	protected abstract void endDocumentInternal() throws XMLStreamException;

	/**
	 * Binds the given prefix to the given namespaces.
	 *
	 * @see SimpleNamespaceContext#bindNamespaceUri(String,String)
	 */
	@Override
	public final void startPrefixMapping(String prefix, String uri) {
		namespaceContext.bindNamespaceUri(prefix, uri);
		namespaceContextChanged = true;
	}

	/**
	 * Removes the binding for the given prefix.
	 *
	 * @see SimpleNamespaceContext#removeBinding(String)
	 */
	@Override
	public final void endPrefixMapping(String prefix) {
		namespaceContext.removeBinding(prefix);
		namespaceContextChanged = true;
	}

	@Override
	public final void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		try {
			startElementInternal(toQName(uri, qName), atts, namespaceContextChanged ? namespaceContext : null);
			namespaceContextChanged = false;
		}
		catch (XMLStreamException ex) {
			throw new SAXException("Could not handle startElement: " + ex.getMessage(), ex);
		}
	}

	protected abstract void startElementInternal(QName name, Attributes atts, SimpleNamespaceContext namespaceContext)
			throws XMLStreamException;

	@Override
	public final void endElement(String uri, String localName, String qName) throws SAXException {
		try {
			endElementInternal(toQName(uri, qName), namespaceContextChanged ? namespaceContext : null);
			namespaceContextChanged = false;
		}
		catch (XMLStreamException ex) {
			throw new SAXException("Could not handle endElement: " + ex.getMessage(), ex);
		}
	}

	protected abstract void endElementInternal(QName name, SimpleNamespaceContext namespaceContext)
			throws XMLStreamException;

	@Override
	public final void characters(char ch[], int start, int length) throws SAXException {
		try {
			charactersInternal(ch, start, length);
		}
		catch (XMLStreamException ex) {
			throw new SAXException("Could not handle characters: " + ex.getMessage(), ex);
		}
	}

	protected abstract void charactersInternal(char[] ch, int start, int length) throws XMLStreamException;

	@Override
	public final void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		try {
			ignorableWhitespaceInternal(ch, start, length);
		}
		catch (XMLStreamException ex) {
			throw new SAXException("Could not handle ignorableWhitespace:" + ex.getMessage(), ex);
		}
	}

	protected abstract void ignorableWhitespaceInternal(char[] ch, int start, int length) throws XMLStreamException;

	@Override
	public final void processingInstruction(String target, String data) throws SAXException {
		try {
			processingInstructionInternal(target, data);
		}
		catch (XMLStreamException ex) {
			throw new SAXException("Could not handle processingInstruction: " + ex.getMessage(), ex);
		}
	}

	protected abstract void processingInstructionInternal(String target, String data) throws XMLStreamException;

	@Override
	public final void skippedEntity(String name) throws SAXException {
		try {
			skippedEntityInternal(name);
		}
		catch (XMLStreamException ex) {
			throw new SAXException("Could not handle skippedEntity: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Convert a namespace URI and DOM or SAX qualified name to a {@code QName}. The qualified name can have the form
	 * {@code prefix:localname} or {@code localName}.
	 *
	 * @param namespaceUri  the namespace URI
	 * @param qualifiedName the qualified name
	 * @return a QName
	 */
	protected QName toQName(String namespaceUri, String qualifiedName) {
		int idx = qualifiedName.indexOf(':');
		if (idx == -1) {
			return new QName(namespaceUri, qualifiedName);
		}
		else {
			String prefix = qualifiedName.substring(0, idx);
			String localPart = qualifiedName.substring(idx + 1);
			return new QName(namespaceUri, localPart, prefix);
		}
	}

	protected abstract void skippedEntityInternal(String name) throws XMLStreamException;
}
