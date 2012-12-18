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

package org.springframework.oxm.support;

import java.io.IOException;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;

import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

import org.springframework.oxm.Marshaller;
import org.springframework.util.Assert;

/**
 * {@link Source} implementation that uses a {@link Marshaller}.Can be constructed with a
 * {@code Marshaller} and an object to be marshalled.
 *
 * <p>Even though {@code MarshallingSource} extends from {@code SAXSource}, calling the methods of
 * {@code SAXSource} is <strong>not supported</strong>. In general, the only supported operation on this class is
 * to use the {@code XMLReader} obtained via {@link #getXMLReader()} to parse the input source obtained via {@link
 * #getInputSource()}. Calling {@link #setXMLReader(XMLReader)} or {@link #setInputSource(InputSource)} will result in
 * {@code UnsupportedOperationException}s.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see javax.xml.transform.Transformer
 */
public class MarshallingSource extends SAXSource {

	private final Marshaller marshaller;

	private final Object content;


	/**
	 * Create a new {@code MarshallingSource} with the given marshaller and content.
	 * @param marshaller the marshaller to use
	 * @param content the object to be marshalled
	 */
	public MarshallingSource(Marshaller marshaller, Object content) {
		super(new MarshallingXMLReader(marshaller, content), new InputSource());
		Assert.notNull(marshaller, "'marshaller' must not be null");
		Assert.notNull(content, "'content' must not be null");
		this.marshaller = marshaller;
		this.content = content;
	}


	/**
	 * Return the {@code Marshaller} used by this {@code MarshallingSource}.
	 */
	public Marshaller getMarshaller() {
		return this.marshaller;
	}

	/**
	 * Return the object to be marshalled.
	 */
	public Object getContent() {
		return this.content;
	}

	/**
	 * Throws a {@code UnsupportedOperationException}.
	 */
	@Override
	public void setInputSource(InputSource inputSource) {
		throw new UnsupportedOperationException("setInputSource is not supported");
	}

	/**
	 * Throws a {@code UnsupportedOperationException}.
	 */
	@Override
	public void setXMLReader(XMLReader reader) {
		throw new UnsupportedOperationException("setXMLReader is not supported");
	}


	private static class MarshallingXMLReader implements XMLReader {

		private final Marshaller marshaller;

		private final Object content;

		private DTDHandler dtdHandler;

		private ContentHandler contentHandler;

		private EntityResolver entityResolver;

		private ErrorHandler errorHandler;

		private LexicalHandler lexicalHandler;

		private MarshallingXMLReader(Marshaller marshaller, Object content) {
			Assert.notNull(marshaller, "'marshaller' must not be null");
			Assert.notNull(content, "'content' must not be null");
			this.marshaller = marshaller;
			this.content = content;
		}

		public void setContentHandler(ContentHandler contentHandler) {
			this.contentHandler = contentHandler;
		}

		public ContentHandler getContentHandler() {
			return this.contentHandler;
		}

		public void setDTDHandler(DTDHandler dtdHandler) {
			this.dtdHandler = dtdHandler;
		}

		public DTDHandler getDTDHandler() {
			return this.dtdHandler;
		}

		public void setEntityResolver(EntityResolver entityResolver) {
			this.entityResolver = entityResolver;
		}

		public EntityResolver getEntityResolver() {
			return this.entityResolver;
		}

		public void setErrorHandler(ErrorHandler errorHandler) {
			this.errorHandler = errorHandler;
		}

		public ErrorHandler getErrorHandler() {
			return this.errorHandler;
		}

		protected LexicalHandler getLexicalHandler() {
			return this.lexicalHandler;
		}

		public boolean getFeature(String name) throws SAXNotRecognizedException {
			throw new SAXNotRecognizedException(name);
		}

		public void setFeature(String name, boolean value) throws SAXNotRecognizedException {
			throw new SAXNotRecognizedException(name);
		}

		public Object getProperty(String name) throws SAXNotRecognizedException {
			if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
				return lexicalHandler;
			}
			else {
				throw new SAXNotRecognizedException(name);
			}
		}

		public void setProperty(String name, Object value) throws SAXNotRecognizedException {
			if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
				this.lexicalHandler = (LexicalHandler) value;
			}
			else {
				throw new SAXNotRecognizedException(name);
			}
		}

		public void parse(InputSource input) throws SAXException {
			parse();
		}

		public void parse(String systemId) throws SAXException {
			parse();
		}

		private void parse() throws SAXException {
			SAXResult result = new SAXResult(getContentHandler());
			result.setLexicalHandler(getLexicalHandler());
			try {
				this.marshaller.marshal(this.content, result);
			}
			catch (IOException ex) {
				SAXParseException saxException = new SAXParseException(ex.getMessage(), null, null, -1, -1, ex);
				ErrorHandler errorHandler = getErrorHandler();
				if (errorHandler != null) {
					errorHandler.fatalError(saxException);
				}
				else {
					throw saxException;
				}
			}
		}
	}

}
