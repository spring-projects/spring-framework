/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.ext.LexicalHandler;

import org.springframework.lang.Nullable;

/**
 * SAX {@link org.xml.sax.ContentHandler} and {@link LexicalHandler}
 * that writes to a {@link javax.xml.stream.util.XMLEventConsumer}.
 *
 * @author Arjen Poutsma
 * @since 4.0.3
 */
class StaxEventHandler extends AbstractStaxHandler {

	private final XMLEventFactory eventFactory;

	private final XMLEventWriter eventWriter;


	/**
	 * Construct a new instance of the {@code StaxEventContentHandler} that writes to the
	 * given {@code XMLEventWriter}. A default {@code XMLEventFactory} will be created.
	 * @param eventWriter the writer to write events to
	 */
	public StaxEventHandler(XMLEventWriter eventWriter) {
		this.eventFactory = XMLEventFactory.newInstance();
		this.eventWriter = eventWriter;
	}

	/**
	 * Construct a new instance of the {@code StaxEventContentHandler} that uses the given
	 * event factory to create events and writes to the given {@code XMLEventConsumer}.
	 * @param eventWriter the writer to write events to
	 * @param factory the factory used to create events
	 */
	public StaxEventHandler(XMLEventWriter eventWriter, XMLEventFactory factory) {
		this.eventFactory = factory;
		this.eventWriter = eventWriter;
	}


	@Override
	public void setDocumentLocator(@Nullable Locator locator) {
		if (locator != null) {
			this.eventFactory.setLocation(new LocatorLocationAdapter(locator));
		}
	}

	@Override
	protected void startDocumentInternal() throws XMLStreamException {
		this.eventWriter.add(this.eventFactory.createStartDocument());
	}

	@Override
	protected void endDocumentInternal() throws XMLStreamException {
		this.eventWriter.add(this.eventFactory.createEndDocument());
	}

	@Override
	protected void startElementInternal(QName name, Attributes atts,
			Map<String, String> namespaceMapping) throws XMLStreamException {

		List<Attribute> attributes = getAttributes(atts);
		List<Namespace> namespaces = getNamespaces(namespaceMapping);
		this.eventWriter.add(
				this.eventFactory.createStartElement(name, attributes.iterator(), namespaces.iterator()));

	}

	private List<Namespace> getNamespaces(Map<String, String> namespaceMappings) {
		List<Namespace> result = new ArrayList<>(namespaceMappings.size());
		namespaceMappings.forEach((prefix, namespaceUri) ->
				result.add(this.eventFactory.createNamespace(prefix, namespaceUri)));
		return result;
	}

	private List<Attribute> getAttributes(Attributes attributes) {
		int attrLength = attributes.getLength();
		List<Attribute> result = new ArrayList<>(attrLength);
		for (int i = 0; i < attrLength; i++) {
			QName attrName = toQName(attributes.getURI(i), attributes.getQName(i));
			if (!isNamespaceDeclaration(attrName)) {
				result.add(this.eventFactory.createAttribute(attrName, attributes.getValue(i)));
			}
		}
		return result;
	}

	@Override
	protected void endElementInternal(QName name, Map<String, String> namespaceMapping) throws XMLStreamException {
		List<Namespace> namespaces = getNamespaces(namespaceMapping);
		this.eventWriter.add(this.eventFactory.createEndElement(name, namespaces.iterator()));
	}

	@Override
	protected void charactersInternal(String data) throws XMLStreamException {
		this.eventWriter.add(this.eventFactory.createCharacters(data));
	}

	@Override
	protected void cDataInternal(String data) throws XMLStreamException {
		this.eventWriter.add(this.eventFactory.createCData(data));
	}

	@Override
	protected void ignorableWhitespaceInternal(String data) throws XMLStreamException {
		this.eventWriter.add(this.eventFactory.createIgnorableSpace(data));
	}

	@Override
	protected void processingInstructionInternal(String target, String data) throws XMLStreamException {
		this.eventWriter.add(this.eventFactory.createProcessingInstruction(target, data));
	}

	@Override
	protected void dtdInternal(String dtd) throws XMLStreamException {
		this.eventWriter.add(this.eventFactory.createDTD(dtd));
	}

	@Override
	protected void commentInternal(String comment) throws XMLStreamException {
		this.eventWriter.add(this.eventFactory.createComment(comment));
	}

	// Ignored
	@Override
	protected void skippedEntityInternal(String name) {
	}


	private static final class LocatorLocationAdapter implements Location {

		private final Locator locator;

		public LocatorLocationAdapter(Locator locator) {
			this.locator = locator;
		}

		@Override
		public int getLineNumber() {
			return this.locator.getLineNumber();
		}

		@Override
		public int getColumnNumber() {
			return this.locator.getColumnNumber();
		}

		@Override
		public int getCharacterOffset() {
			return -1;
		}

		@Override
		public String getPublicId() {
			return this.locator.getPublicId();
		}

		@Override
		public String getSystemId() {
			return this.locator.getSystemId();
		}
	}

}
