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

import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

/**
 * Abstract base class for SAX {@code XMLReader} implementations. Contains properties as defined in {@link
 * XMLReader}, and does not recognize any features.
 *
 * @author Arjen Poutsma
 * @see #setContentHandler(org.xml.sax.ContentHandler)
 * @see #setDTDHandler(org.xml.sax.DTDHandler)
 * @see #setEntityResolver(org.xml.sax.EntityResolver)
 * @see #setErrorHandler(org.xml.sax.ErrorHandler)
 * @since 3.0
 */
abstract class AbstractXMLReader implements XMLReader {

	private DTDHandler dtdHandler;

	private ContentHandler contentHandler;

	private EntityResolver entityResolver;

	private ErrorHandler errorHandler;

	private LexicalHandler lexicalHandler;

	public ContentHandler getContentHandler() {
		return contentHandler;
	}

	public void setContentHandler(ContentHandler contentHandler) {
		this.contentHandler = contentHandler;
	}

	public void setDTDHandler(DTDHandler dtdHandler) {
		this.dtdHandler = dtdHandler;
	}

	public DTDHandler getDTDHandler() {
		return dtdHandler;
	}

	public EntityResolver getEntityResolver() {
		return entityResolver;
	}

	public void setEntityResolver(EntityResolver entityResolver) {
		this.entityResolver = entityResolver;
	}

	public ErrorHandler getErrorHandler() {
		return errorHandler;
	}

	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	protected LexicalHandler getLexicalHandler() {
		return lexicalHandler;
	}

	/**
	 * Throws a {@code SAXNotRecognizedException} exception.
	 *
	 * @throws org.xml.sax.SAXNotRecognizedException
	 *          always
	 */
	public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
		throw new SAXNotRecognizedException(name);
	}

	/**
	 * Throws a {@code SAXNotRecognizedException} exception.
	 *
	 * @throws SAXNotRecognizedException always
	 */
	public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
		throw new SAXNotRecognizedException(name);
	}

	/**
	 * Throws a {@code SAXNotRecognizedException} exception when the given property does not signify a lexical
	 * handler. The property name for a lexical handler is {@code http://xml.org/sax/properties/lexical-handler}.
	 */
	public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
		if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
			return lexicalHandler;
		}
		else {
			throw new SAXNotRecognizedException(name);
		}
	}

	/**
	 * Throws a {@code SAXNotRecognizedException} exception when the given property does not signify a lexical
	 * handler. The property name for a lexical handler is {@code http://xml.org/sax/properties/lexical-handler}.
	 */
	public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
		if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
			lexicalHandler = (LexicalHandler) value;
		}
		else {
			throw new SAXNotRecognizedException(name);
		}
	}
}
