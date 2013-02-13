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
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;

import org.springframework.util.Assert;

/**
 * Implementation of the {@link javax.xml.stream.XMLStreamWriter} interface that wraps a {@link XMLEventWriter}.
 *
 * @author Arjen Poutsma
 * @since 3.0.5
 * @see StaxUtils#createEventStreamWriter(javax.xml.stream.XMLEventWriter, javax.xml.stream.XMLEventFactory)
 */
class XMLEventStreamWriter implements XMLStreamWriter {

	private static final String DEFAULT_ENCODING = "UTF-8";

	private final XMLEventWriter eventWriter;

	private final XMLEventFactory eventFactory;

	private List<EndElement> endElements = new ArrayList<EndElement>();

	public XMLEventStreamWriter(XMLEventWriter eventWriter, XMLEventFactory eventFactory) {
		Assert.notNull(eventWriter, "'eventWriter' must not be null");
		Assert.notNull(eventFactory, "'eventFactory' must not be null");

		this.eventWriter = eventWriter;
		this.eventFactory = eventFactory;
	}

	public NamespaceContext getNamespaceContext() {
		return eventWriter.getNamespaceContext();
	}

	public String getPrefix(String uri) throws XMLStreamException {
		return eventWriter.getPrefix(uri);
	}

	public void setPrefix(String prefix, String uri) throws XMLStreamException {
		eventWriter.setPrefix(prefix, uri);
	}

	public void setDefaultNamespace(String uri) throws XMLStreamException {
		eventWriter.setDefaultNamespace(uri);
	}

	public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
		eventWriter.setNamespaceContext(context);
	}

	public void writeStartDocument() throws XMLStreamException {
		eventWriter.add(eventFactory.createStartDocument());
	}

	public void writeStartDocument(String version) throws XMLStreamException {
		eventWriter.add(eventFactory.createStartDocument(DEFAULT_ENCODING, version));
	}

	public void writeStartDocument(String encoding, String version) throws XMLStreamException {
		eventWriter.add(eventFactory.createStartDocument(encoding, version));
	}

	public void writeStartElement(String localName) throws XMLStreamException {
		writeStartElement(eventFactory.createStartElement(new QName(localName), null, null));
	}

	public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
		writeStartElement(eventFactory.createStartElement(new QName(namespaceURI, localName), null, null));
	}

	public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
		writeStartElement(eventFactory.createStartElement(new QName(namespaceURI, localName, prefix), null, null));
	}

	public void writeEmptyElement(String localName) throws XMLStreamException {
		writeStartElement(localName);
		writeEndElement();
	}

	public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
		writeStartElement(namespaceURI, localName);
		writeEndElement();
	}

	public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
		writeStartElement(prefix, localName, namespaceURI);
		writeEndElement();
	}

	public void writeEndElement() throws XMLStreamException {
		int last = endElements.size() - 1;
		EndElement lastEndElement = endElements.get(last);
		eventWriter.add(lastEndElement);
		endElements.remove(last);
	}

	public void writeAttribute(String localName, String value) throws XMLStreamException {
		eventWriter.add(eventFactory.createAttribute(localName, value));
	}

	public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
		eventWriter.add(eventFactory.createAttribute(new QName(namespaceURI, localName), value));
	}

	public void writeAttribute(String prefix, String namespaceURI, String localName, String value)
			throws XMLStreamException {
		eventWriter.add(eventFactory.createAttribute(prefix, namespaceURI, localName, value));
	}

	public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
		writeNamespace(eventFactory.createNamespace(prefix, namespaceURI));
	}

	public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
		writeNamespace(eventFactory.createNamespace(namespaceURI));
	}

	public void writeCharacters(String text) throws XMLStreamException {
		eventWriter.add(eventFactory.createCharacters(text));
	}

	public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
		eventWriter.add(eventFactory.createCharacters(new String(text, start, len)));
	}

	public void writeCData(String data) throws XMLStreamException {
		eventWriter.add(eventFactory.createCData(data));
	}

	public void writeComment(String data) throws XMLStreamException {
		eventWriter.add(eventFactory.createComment(data));
	}

	public void writeProcessingInstruction(String target) throws XMLStreamException {
		eventWriter.add(eventFactory.createProcessingInstruction(target, ""));
	}

	public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
		eventWriter.add(eventFactory.createProcessingInstruction(target, data));
	}

	public void writeDTD(String dtd) throws XMLStreamException {
		eventWriter.add(eventFactory.createDTD(dtd));
	}

	public void writeEntityRef(String name) throws XMLStreamException {
		eventWriter.add(eventFactory.createEntityReference(name, null));
	}

	public void writeEndDocument() throws XMLStreamException {
		eventWriter.add(eventFactory.createEndDocument());
	}

	public Object getProperty(String name) throws IllegalArgumentException {
		throw new IllegalArgumentException();
	}

	public void flush() throws XMLStreamException {
		eventWriter.flush();
	}

	public void close() throws XMLStreamException {
		eventWriter.close();
	}

	private void writeStartElement(StartElement startElement) throws XMLStreamException {
		eventWriter.add(startElement);
		endElements.add(eventFactory.createEndElement(startElement.getName(), startElement.getNamespaces()));
	}

	private void writeNamespace(Namespace namespace) throws XMLStreamException {
		int last = endElements.size() - 1;
		EndElement oldEndElement = endElements.get(last);
		Iterator oldNamespaces = oldEndElement.getNamespaces();
		List<Namespace> newNamespaces = new ArrayList<Namespace>();
		while (oldNamespaces.hasNext()) {
			Namespace oldNamespace = (Namespace) oldNamespaces.next();
			newNamespaces.add(oldNamespace);
		}
		newNamespaces.add(namespace);
		EndElement newEndElement = eventFactory.createEndElement(oldEndElement.getName(), newNamespaces.iterator());
		eventWriter.add(namespace);
		endElements.set(last, newEndElement);
	}
}
