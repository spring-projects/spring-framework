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
import java.util.Iterator;
import java.util.List;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;

/**
 * Implementation of the {@link javax.xml.stream.XMLStreamWriter} interface
 * that wraps an {@link XMLEventWriter}.
 *
 * @author Arjen Poutsma
 * @since 3.0.5
 * @see StaxUtils#createEventStreamWriter(javax.xml.stream.XMLEventWriter, javax.xml.stream.XMLEventFactory)
 */
class XMLEventStreamWriter implements XMLStreamWriter {

	private static final String DEFAULT_ENCODING = "UTF-8";

	private final XMLEventWriter eventWriter;

	private final XMLEventFactory eventFactory;

	private final List<EndElement> endElements = new ArrayList<EndElement>();

	private boolean emptyElement = false;


	public XMLEventStreamWriter(XMLEventWriter eventWriter, XMLEventFactory eventFactory) {
		this.eventWriter = eventWriter;
		this.eventFactory = eventFactory;
	}


	@Override
	public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
		this.eventWriter.setNamespaceContext(context);
	}

	@Override
	public NamespaceContext getNamespaceContext() {
		return this.eventWriter.getNamespaceContext();
	}

	@Override
	public void setPrefix(String prefix, String uri) throws XMLStreamException {
		this.eventWriter.setPrefix(prefix, uri);
	}

	@Override
	public String getPrefix(String uri) throws XMLStreamException {
		return this.eventWriter.getPrefix(uri);
	}

	@Override
	public void setDefaultNamespace(String uri) throws XMLStreamException {
		this.eventWriter.setDefaultNamespace(uri);
	}

	@Override
	public Object getProperty(String name) throws IllegalArgumentException {
		throw new IllegalArgumentException();
	}


	@Override
	public void writeStartDocument() throws XMLStreamException {
		closeEmptyElementIfNecessary();
		this.eventWriter.add(this.eventFactory.createStartDocument());
	}

	@Override
	public void writeStartDocument(String version) throws XMLStreamException {
		closeEmptyElementIfNecessary();
		this.eventWriter.add(this.eventFactory.createStartDocument(DEFAULT_ENCODING, version));
	}

	@Override
	public void writeStartDocument(String encoding, String version) throws XMLStreamException {
		closeEmptyElementIfNecessary();
		this.eventWriter.add(this.eventFactory.createStartDocument(encoding, version));
	}

	@Override
	public void writeStartElement(String localName) throws XMLStreamException {
		closeEmptyElementIfNecessary();
		doWriteStartElement(this.eventFactory.createStartElement(new QName(localName), null, null));
	}

	@Override
	public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
		closeEmptyElementIfNecessary();
		doWriteStartElement(this.eventFactory.createStartElement(new QName(namespaceURI, localName), null, null));
	}

	@Override
	public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
		closeEmptyElementIfNecessary();
		doWriteStartElement(this.eventFactory.createStartElement(new QName(namespaceURI, localName, prefix), null, null));
	}

	private void doWriteStartElement(StartElement startElement) throws XMLStreamException {
		this.eventWriter.add(startElement);
		this.endElements.add(this.eventFactory.createEndElement(startElement.getName(), startElement.getNamespaces()));
	}

	@Override
	public void writeEmptyElement(String localName) throws XMLStreamException {
		closeEmptyElementIfNecessary();
		writeStartElement(localName);
		this.emptyElement = true;
	}

	@Override
	public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
		closeEmptyElementIfNecessary();
		writeStartElement(namespaceURI, localName);
		this.emptyElement = true;
	}

	@Override
	public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
		closeEmptyElementIfNecessary();
		writeStartElement(prefix, localName, namespaceURI);
		this.emptyElement = true;
	}

	private void closeEmptyElementIfNecessary() throws XMLStreamException {
		if (this.emptyElement) {
			this.emptyElement = false;
			writeEndElement();
		}
	}

	@Override
	public void writeEndElement() throws XMLStreamException {
		closeEmptyElementIfNecessary();
		int last = this.endElements.size() - 1;
		EndElement lastEndElement = this.endElements.get(last);
		this.eventWriter.add(lastEndElement);
		this.endElements.remove(last);
	}

	@Override
	public void writeAttribute(String localName, String value) throws XMLStreamException {
		this.eventWriter.add(this.eventFactory.createAttribute(localName, value));
	}

	@Override
	public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
		this.eventWriter.add(this.eventFactory.createAttribute(new QName(namespaceURI, localName), value));
	}

	@Override
	public void writeAttribute(String prefix, String namespaceURI, String localName, String value)
			throws XMLStreamException {

		this.eventWriter.add(this.eventFactory.createAttribute(prefix, namespaceURI, localName, value));
	}

	@Override
	public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
		doWriteNamespace(this.eventFactory.createNamespace(prefix, namespaceURI));
	}

	@Override
	public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
		doWriteNamespace(this.eventFactory.createNamespace(namespaceURI));
	}

	@SuppressWarnings("rawtypes")
	private void doWriteNamespace(Namespace namespace) throws XMLStreamException {
		int last = this.endElements.size() - 1;
		EndElement oldEndElement = this.endElements.get(last);
		Iterator oldNamespaces = oldEndElement.getNamespaces();
		List<Namespace> newNamespaces = new ArrayList<Namespace>();
		while (oldNamespaces.hasNext()) {
			Namespace oldNamespace = (Namespace) oldNamespaces.next();
			newNamespaces.add(oldNamespace);
		}
		newNamespaces.add(namespace);
		EndElement newEndElement = this.eventFactory.createEndElement(oldEndElement.getName(), newNamespaces.iterator());
		this.eventWriter.add(namespace);
		this.endElements.set(last, newEndElement);
	}

	@Override
	public void writeCharacters(String text) throws XMLStreamException {
		closeEmptyElementIfNecessary();
		this.eventWriter.add(this.eventFactory.createCharacters(text));
	}

	@Override
	public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
		closeEmptyElementIfNecessary();
		this.eventWriter.add(this.eventFactory.createCharacters(new String(text, start, len)));
	}

	@Override
	public void writeCData(String data) throws XMLStreamException {
		closeEmptyElementIfNecessary();
		this.eventWriter.add(this.eventFactory.createCData(data));
	}

	@Override
	public void writeComment(String data) throws XMLStreamException {
		closeEmptyElementIfNecessary();
		this.eventWriter.add(this.eventFactory.createComment(data));
	}

	@Override
	public void writeProcessingInstruction(String target) throws XMLStreamException {
		closeEmptyElementIfNecessary();
		this.eventWriter.add(this.eventFactory.createProcessingInstruction(target, ""));
	}

	@Override
	public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
		closeEmptyElementIfNecessary();
		this.eventWriter.add(this.eventFactory.createProcessingInstruction(target, data));
	}

	@Override
	public void writeDTD(String dtd) throws XMLStreamException {
		closeEmptyElementIfNecessary();
		this.eventWriter.add(this.eventFactory.createDTD(dtd));
	}

	@Override
	public void writeEntityRef(String name) throws XMLStreamException {
		closeEmptyElementIfNecessary();
		this.eventWriter.add(this.eventFactory.createEntityReference(name, null));
	}

	@Override
	public void writeEndDocument() throws XMLStreamException {
		closeEmptyElementIfNecessary();
		this.eventWriter.add(this.eventFactory.createEndDocument());
	}

	@Override
	public void flush() throws XMLStreamException {
		this.eventWriter.flush();
	}

	@Override
	public void close() throws XMLStreamException {
		closeEmptyElementIfNecessary();
		this.eventWriter.close();
	}

}
