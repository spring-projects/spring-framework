/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import org.springframework.util.Assert;

/**
 * SAX {@code ContentHandler} that transforms callback calls to DOM {@code Node}s.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see org.w3c.dom.Node
 */
class DomContentHandler implements ContentHandler {

	private final Document document;

	private final List<Element> elements = new ArrayList<>();

	private final Node node;

	/**
	 * Creates a new instance of the {@code DomContentHandler} with the given node.
	 *
	 * @param node the node to publish events to
	 */
	DomContentHandler(Node node) {
		Assert.notNull(node, "node must not be null");
		this.node = node;
		if (node instanceof Document) {
			this.document = (Document) node;
		}
		else {
			this.document = node.getOwnerDocument();
		}
		Assert.notNull(this.document, "document must not be null");
	}

	private Node getParent() {
		if (!this.elements.isEmpty()) {
			return this.elements.get(this.elements.size() - 1);
		}
		else {
			return this.node;
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		Node parent = getParent();
		Element element = this.document.createElementNS(uri, qName);
		for (int i = 0; i < attributes.getLength(); i++) {
			String attrUri = attributes.getURI(i);
			String attrQname = attributes.getQName(i);
			String value = attributes.getValue(i);
			if (!attrQname.startsWith("xmlns")) {
				element.setAttributeNS(attrUri, attrQname, value);
			}
		}
		element = (Element) parent.appendChild(element);
		this.elements.add(element);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		this.elements.remove(this.elements.size() - 1);
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		String data = new String(ch, start, length);
		Node parent = getParent();
		Node lastChild = parent.getLastChild();
		if (lastChild != null && lastChild.getNodeType() == Node.TEXT_NODE) {
			((Text) lastChild).appendData(data);
		}
		else {
			Text text = this.document.createTextNode(data);
			parent.appendChild(text);
		}
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		Node parent = getParent();
		ProcessingInstruction pi = this.document.createProcessingInstruction(target, data);
		parent.appendChild(pi);
	}

	/*
	 * Unsupported
	 */

	@Override
	public void setDocumentLocator(Locator locator) {
	}

	@Override
	public void startDocument() throws SAXException {
	}

	@Override
	public void endDocument() throws SAXException {
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
	}
}
