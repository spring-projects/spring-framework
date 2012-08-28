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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.XMLEventConsumer;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

import org.springframework.util.StringUtils;

/**
 * SAX <code>ContentHandler</code> that transforms callback calls to <code>XMLEvent</code>s
 * and writes them to a <code>XMLEventConsumer</code>.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see XMLEvent
 * @see XMLEventConsumer
 */
class StaxEventContentHandler extends AbstractStaxContentHandler {

	private final XMLEventFactory eventFactory;

	private final XMLEventConsumer eventConsumer;


	/**
	 * Construct a new instance of the <code>StaxEventContentHandler</code> that writes to the given
	 * <code>XMLEventConsumer</code>. A default <code>XMLEventFactory</code> will be created.
	 * @param consumer the consumer to write events to
	 */
	StaxEventContentHandler(XMLEventConsumer consumer) {
		this.eventFactory = XMLEventFactory.newInstance();
		this.eventConsumer = consumer;
	}

	/**
	 * Construct a new instance of the <code>StaxEventContentHandler</code> that uses the given
	 * event factory to create events and writes to the given <code>XMLEventConsumer</code>.
	 * @param consumer the consumer to write events to
	 * @param factory  the factory used to create events
	 */
	StaxEventContentHandler(XMLEventConsumer consumer, XMLEventFactory factory) {
		this.eventFactory = factory;
		this.eventConsumer = consumer;
	}

	public void setDocumentLocator(Locator locator) {
		if (locator != null) {
			this.eventFactory.setLocation(new LocatorLocationAdapter(locator));
		}
	}

	@Override
	protected void startDocumentInternal() throws XMLStreamException {
		consumeEvent(this.eventFactory.createStartDocument());
	}

	@Override
	protected void endDocumentInternal() throws XMLStreamException {
		consumeEvent(this.eventFactory.createEndDocument());
	}

	@Override
	protected void startElementInternal(QName name, Attributes atts, SimpleNamespaceContext namespaceContext)
			throws XMLStreamException {

		List attributes = getAttributes(atts);
		List namespaces = createNamespaces(namespaceContext);
		consumeEvent(this.eventFactory.createStartElement(name, attributes.iterator(),
				(namespaces != null ? namespaces.iterator() : null)));
	}

	@Override
	protected void endElementInternal(QName name, SimpleNamespaceContext namespaceContext) throws XMLStreamException {
		List namespaces = createNamespaces(namespaceContext);
		consumeEvent(this.eventFactory.createEndElement(name, namespaces != null ? namespaces.iterator() : null));
	}

	@Override
	protected void charactersInternal(char[] ch, int start, int length) throws XMLStreamException {
		consumeEvent(this.eventFactory.createCharacters(new String(ch, start, length)));
	}

	@Override
	protected void ignorableWhitespaceInternal(char[] ch, int start, int length) throws XMLStreamException {
		consumeEvent(this.eventFactory.createIgnorableSpace(new String(ch, start, length)));
	}

	@Override
	protected void processingInstructionInternal(String target, String data) throws XMLStreamException {
		consumeEvent(this.eventFactory.createProcessingInstruction(target, data));
	}

	private void consumeEvent(XMLEvent event) throws XMLStreamException {
		this.eventConsumer.add(event);
	}

	/**
	 * Create and return a list of <code>NameSpace</code> objects from the <code>NamespaceContext</code>.
	 */
	private List<Namespace> createNamespaces(SimpleNamespaceContext namespaceContext) {
		if (namespaceContext == null) {
			return null;
		}

		List<Namespace> namespaces = new ArrayList<Namespace>();
		String defaultNamespaceUri = namespaceContext.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX);
		if (StringUtils.hasLength(defaultNamespaceUri)) {
			namespaces.add(this.eventFactory.createNamespace(defaultNamespaceUri));
		}
		for (Iterator iterator = namespaceContext.getBoundPrefixes(); iterator.hasNext();) {
			String prefix = (String) iterator.next();
			String namespaceUri = namespaceContext.getNamespaceURI(prefix);
			namespaces.add(this.eventFactory.createNamespace(prefix, namespaceUri));
		}
		return namespaces;
	}

	private List<Attribute> getAttributes(Attributes attributes) {
		List<Attribute> list = new ArrayList<Attribute>();
		for (int i = 0; i < attributes.getLength(); i++) {
			QName name = toQName(attributes.getURI(i), attributes.getQName(i));
			if (!("xmlns".equals(name.getLocalPart()) || "xmlns".equals(name.getPrefix()))) {
				list.add(this.eventFactory.createAttribute(name, attributes.getValue(i)));
			}
		}
		return list;
	}

	/* No operation */
	@Override
	protected void skippedEntityInternal(String name) throws XMLStreamException {
	}


	private static final class LocatorLocationAdapter implements Location {

		private final Locator locator;

		public LocatorLocationAdapter(Locator locator) {
			this.locator = locator;
		}

		public int getLineNumber() {
			return this.locator.getLineNumber();
		}

		public int getColumnNumber() {
			return this.locator.getColumnNumber();
		}

		public int getCharacterOffset() {
			return -1;
		}

		public String getPublicId() {
			return this.locator.getPublicId();
		}

		public String getSystemId() {
			return this.locator.getSystemId();
		}
	}

}
