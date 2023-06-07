/*
 * Copyright 2002-2023 the original author or authors.
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

import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.NotationDeclaration;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.Locator2;
import org.xml.sax.helpers.AttributesImpl;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * SAX {@code XMLReader} that reads from a StAX {@code XMLEventReader}. Consumes {@code XMLEvents} from
 * an {@code XMLEventReader}, and calls the corresponding methods on the SAX callback interfaces.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see XMLEventReader
 * @see #setContentHandler(org.xml.sax.ContentHandler)
 * @see #setDTDHandler(org.xml.sax.DTDHandler)
 * @see #setEntityResolver(org.xml.sax.EntityResolver)
 * @see #setErrorHandler(org.xml.sax.ErrorHandler)
 */
@SuppressWarnings("rawtypes")
class StaxEventXMLReader extends AbstractStaxXMLReader {

	private static final String DEFAULT_XML_VERSION = "1.0";

	private final XMLEventReader reader;

	private String xmlVersion = DEFAULT_XML_VERSION;

	@Nullable
	private String encoding;


	/**
	 * Constructs a new instance of the {@code StaxEventXmlReader} that reads from
	 * the given {@code XMLEventReader}. The supplied event reader must be in
	 * {@code XMLStreamConstants.START_DOCUMENT} or {@code XMLStreamConstants.START_ELEMENT} state.
	 * @param reader the {@code XMLEventReader} to read from
	 * @throws IllegalStateException if the reader is not at the start of a document or element
	 */
	StaxEventXMLReader(XMLEventReader reader) {
		try {
			XMLEvent event = reader.peek();
			if (event != null && !(event.isStartDocument() || event.isStartElement())) {
				throw new IllegalStateException("XMLEventReader not at start of document or element");
			}
		}
		catch (XMLStreamException ex) {
			throw new IllegalStateException("Could not read first element: " + ex.getMessage());
		}
		this.reader = reader;
	}


	@Override
	protected void parseInternal() throws SAXException, XMLStreamException {
		boolean documentStarted = false;
		boolean documentEnded = false;
		int elementDepth = 0;
		while (this.reader.hasNext() && elementDepth >= 0) {
			XMLEvent event = this.reader.nextEvent();
			if (!event.isStartDocument() && !event.isEndDocument() && !documentStarted) {
				handleStartDocument(event);
				documentStarted = true;
			}
			switch (event.getEventType()) {
				case XMLStreamConstants.START_DOCUMENT -> {
					handleStartDocument(event);
					documentStarted = true;
				}
				case XMLStreamConstants.START_ELEMENT -> {
					elementDepth++;
					handleStartElement(event.asStartElement());
				}
				case XMLStreamConstants.END_ELEMENT -> {
					elementDepth--;
					if (elementDepth >= 0) {
						handleEndElement(event.asEndElement());
					}
				}
				case XMLStreamConstants.PROCESSING_INSTRUCTION ->
						handleProcessingInstruction((ProcessingInstruction) event);
				case XMLStreamConstants.CHARACTERS, XMLStreamConstants.SPACE, XMLStreamConstants.CDATA ->
						handleCharacters(event.asCharacters());
				case XMLStreamConstants.END_DOCUMENT -> {
					handleEndDocument();
					documentEnded = true;
				}
				case XMLStreamConstants.NOTATION_DECLARATION -> handleNotationDeclaration((NotationDeclaration) event);
				case XMLStreamConstants.ENTITY_DECLARATION -> handleEntityDeclaration((EntityDeclaration) event);
				case XMLStreamConstants.COMMENT -> handleComment((Comment) event);
				case XMLStreamConstants.DTD -> handleDtd((DTD) event);
				case XMLStreamConstants.ENTITY_REFERENCE -> handleEntityReference((EntityReference) event);
			}
		}
		if (documentStarted && !documentEnded) {
			handleEndDocument();
		}

	}

	private void handleStartDocument(final XMLEvent event) throws SAXException {
		if (event.isStartDocument()) {
			StartDocument startDocument = (StartDocument) event;
			String xmlVersion = startDocument.getVersion();
			if (StringUtils.hasLength(xmlVersion)) {
				this.xmlVersion = xmlVersion;
			}
			if (startDocument.encodingSet()) {
				this.encoding = startDocument.getCharacterEncodingScheme();
			}
		}

		ContentHandler contentHandler = getContentHandler();
		if (contentHandler != null) {
			final Location location = event.getLocation();
			contentHandler.setDocumentLocator(new Locator2() {
				@Override
				public int getColumnNumber() {
					return (location != null ? location.getColumnNumber() : -1);
				}
				@Override
				public int getLineNumber() {
					return (location != null ? location.getLineNumber() : -1);
				}
				@Override
				@Nullable
				public String getPublicId() {
					return (location != null ? location.getPublicId() : null);
				}
				@Override
				@Nullable
				public String getSystemId() {
					return (location != null ? location.getSystemId() : null);
				}
				@Override
				public String getXMLVersion() {
					return xmlVersion;
				}
				@Override
				@Nullable
				public String getEncoding() {
					return encoding;
				}
			});
			contentHandler.startDocument();
		}
	}

	private void handleStartElement(StartElement startElement) throws SAXException {
		if (getContentHandler() != null) {
			QName qName = startElement.getName();
			if (hasNamespacesFeature()) {
				for (Iterator i = startElement.getNamespaces(); i.hasNext();) {
					Namespace namespace = (Namespace) i.next();
					startPrefixMapping(namespace.getPrefix(), namespace.getNamespaceURI());
				}
				for (Iterator i = startElement.getAttributes(); i.hasNext();){
					Attribute attribute = (Attribute) i.next();
					QName attributeName = attribute.getName();
					startPrefixMapping(attributeName.getPrefix(), attributeName.getNamespaceURI());
				}

				getContentHandler().startElement(qName.getNamespaceURI(), qName.getLocalPart(), toQualifiedName(qName),
						getAttributes(startElement));
			}
			else {
				getContentHandler().startElement("", "", toQualifiedName(qName), getAttributes(startElement));
			}
		}
	}

	private void handleCharacters(Characters characters) throws SAXException {
		char[] data = characters.getData().toCharArray();
		if (getContentHandler() != null && characters.isIgnorableWhiteSpace()) {
			getContentHandler().ignorableWhitespace(data, 0, data.length);
			return;
		}
		if (characters.isCData() && getLexicalHandler() != null) {
			getLexicalHandler().startCDATA();
		}
		if (getContentHandler() != null) {
			getContentHandler().characters(data, 0, data.length);
		}
		if (characters.isCData() && getLexicalHandler() != null) {
			getLexicalHandler().endCDATA();
		}
	}

	private void handleEndElement(EndElement endElement) throws SAXException {
		if (getContentHandler() != null) {
			QName qName = endElement.getName();
			if (hasNamespacesFeature()) {
				getContentHandler().endElement(qName.getNamespaceURI(), qName.getLocalPart(), toQualifiedName(qName));
				for (Iterator i = endElement.getNamespaces(); i.hasNext();) {
					Namespace namespace = (Namespace) i.next();
					endPrefixMapping(namespace.getPrefix());
				}
			}
			else {
				getContentHandler().endElement("", "", toQualifiedName(qName));
			}

		}
	}

	private void handleEndDocument() throws SAXException {
		if (getContentHandler() != null) {
			getContentHandler().endDocument();
		}
	}

	private void handleNotationDeclaration(NotationDeclaration declaration) throws SAXException {
		if (getDTDHandler() != null) {
			getDTDHandler().notationDecl(declaration.getName(), declaration.getPublicId(), declaration.getSystemId());
		}
	}

	private void handleEntityDeclaration(EntityDeclaration entityDeclaration) throws SAXException {
		if (getDTDHandler() != null) {
			getDTDHandler().unparsedEntityDecl(entityDeclaration.getName(), entityDeclaration.getPublicId(),
					entityDeclaration.getSystemId(), entityDeclaration.getNotationName());
		}
	}

	private void handleProcessingInstruction(ProcessingInstruction pi) throws SAXException {
		if (getContentHandler() != null) {
			getContentHandler().processingInstruction(pi.getTarget(), pi.getData());
		}
	}

	private void handleComment(Comment comment) throws SAXException {
		if (getLexicalHandler() != null) {
			char[] ch = comment.getText().toCharArray();
			getLexicalHandler().comment(ch, 0, ch.length);
		}
	}

	private void handleDtd(DTD dtd) throws SAXException {
		if (getLexicalHandler() != null) {
			Location location = dtd.getLocation();
			getLexicalHandler().startDTD(null, location.getPublicId(), location.getSystemId());
		}
		if (getLexicalHandler() != null) {
			getLexicalHandler().endDTD();
		}

	}

	private void handleEntityReference(EntityReference reference) throws SAXException {
		if (getLexicalHandler() != null) {
			getLexicalHandler().startEntity(reference.getName());
		}
		if (getLexicalHandler() != null) {
			getLexicalHandler().endEntity(reference.getName());
		}

	}

	private Attributes getAttributes(StartElement event) {
		AttributesImpl attributes = new AttributesImpl();
		for (Iterator i = event.getAttributes(); i.hasNext();) {
			Attribute attribute = (Attribute) i.next();
			QName qName = attribute.getName();
			String namespace = qName.getNamespaceURI();
			if (namespace == null || !hasNamespacesFeature()) {
				namespace = "";
			}
			String type = attribute.getDTDType();
			if (type == null) {
				type = "CDATA";
			}
			attributes.addAttribute(namespace, qName.getLocalPart(), toQualifiedName(qName), type, attribute.getValue());
		}
		if (hasNamespacePrefixesFeature()) {
			for (Iterator i = event.getNamespaces(); i.hasNext();) {
				Namespace namespace = (Namespace) i.next();
				String prefix = namespace.getPrefix();
				String namespaceUri = namespace.getNamespaceURI();
				String qName;
				if (StringUtils.hasLength(prefix)) {
					qName = "xmlns:" + prefix;
				}
				else {
					qName = "xmlns";
				}
				attributes.addAttribute("", "", qName, "CDATA", namespaceUri);
			}
		}

		return attributes;
	}

}
