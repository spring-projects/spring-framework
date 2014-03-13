/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

import org.springframework.util.Assert;

/**
 * SAX {@code ContentHandler} and {@link LexicalHandler} that writes to a
 * {@code XMLStreamWriter}.
 *
 * @author Arjen Poutsma
 * @see XMLStreamWriter
 * @since 4.0.3
 */
class StaxStreamHandler extends AbstractStaxHandler {

	private final XMLStreamWriter streamWriter;

	boolean inCData = false;

	public StaxStreamHandler(XMLStreamWriter streamWriter) {
		Assert.notNull(streamWriter, "'streamWriter' must not be null");
		this.streamWriter = streamWriter;
	}

	@Override
	protected void startDocumentInternal() throws XMLStreamException {
		streamWriter.writeStartDocument();
	}

	@Override
	protected void endDocumentInternal() throws XMLStreamException {
		streamWriter.writeEndDocument();
	}

	@Override
	protected void startElementInternal(QName name, Attributes attributes,
			Map<String, String> namespaceMapping) throws XMLStreamException {

		streamWriter.writeStartElement(name.getPrefix(), name.getLocalPart(), name.getNamespaceURI());

		for (Map.Entry<String, String> entry : namespaceMapping.entrySet()) {
			String prefix = entry.getKey();
			String namespaceUri = entry.getValue();

			streamWriter.writeNamespace(prefix, namespaceUri);
			if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
				streamWriter.setDefaultNamespace(namespaceUri);
			} else {
				streamWriter.setPrefix(prefix, namespaceUri);
			}
		}
		for (int i = 0; i < attributes.getLength(); i++) {
			QName attrName = toQName(attributes.getURI(i), attributes.getQName(i));
			if (!isNamespaceDeclaration(attrName)) {
				streamWriter.writeAttribute(attrName.getPrefix(), attrName.getNamespaceURI(), attrName.getLocalPart(),
						attributes.getValue(i));
			}
		}

	}

	@Override
	protected void endElementInternal(QName name, Map<String, String> namespaceMapping)
			throws XMLStreamException {
		streamWriter.writeEndElement();
	}

	@Override
	protected void charactersInternal(String data) throws XMLStreamException {
		streamWriter.writeCharacters(data);
	}

	@Override
	protected void cDataInternal(String data) throws XMLStreamException {
		streamWriter.writeCData(data);
	}

	@Override
	protected void ignorableWhitespaceInternal(String data) throws XMLStreamException {
		streamWriter.writeCharacters(data);
	}

	@Override
	protected void processingInstructionInternal(String target, String data)
			throws XMLStreamException {
		streamWriter.writeProcessingInstruction(target, data);
	}

	@Override
	protected void dtdInternal(String dtd) throws XMLStreamException {
		streamWriter.writeDTD(dtd);
	}

	@Override
	protected void commentInternal(String comment) throws XMLStreamException {
		streamWriter.writeComment(comment);
	}
	// Ignored

	@Override
	public void startEntity(String name) throws SAXException {

	}

	@Override
	public void endEntity(String name) throws SAXException {

	}

	@Override
	public void setDocumentLocator(Locator locator) {
	}


	@Override
	protected void skippedEntityInternal(String name) throws XMLStreamException {

	}



}
