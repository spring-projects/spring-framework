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

package org.springframework.oxm.xmlbeans;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.lang.ref.WeakReference;
import java.nio.CharBuffer;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.xmlbeans.XMLStreamValidationException;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlSaxHandler;
import org.apache.xmlbeans.XmlValidationError;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

import org.springframework.oxm.Marshaller;
import org.springframework.oxm.MarshallingFailureException;
import org.springframework.oxm.UncategorizedMappingException;
import org.springframework.oxm.UnmarshallingFailureException;
import org.springframework.oxm.ValidationFailureException;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.support.AbstractMarshaller;
import org.springframework.util.xml.StaxUtils;

/**
 * Implementation of the {@link Marshaller} interface for Apache XMLBeans.
 *
 * <p>Options can be set by setting the {@code xmlOptions} property.
 * The {@link XmlOptionsFactoryBean} is provided to easily set up an {@link XmlOptions} instance.
 *
 * <p>Unmarshalled objects can be validated by setting the {@code validating} property,
 * or by calling the {@link #validate(XmlObject)} method directly. Invalid objects will
 * result in an {@link ValidationFailureException}.
 *
 * <p><b>NOTE:</b> Due to the nature of XMLBeans, this marshaller requires
 * all passed objects to be of type {@link XmlObject}.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see #setValidating
 * @see #setXmlOptions
 * @see XmlOptionsFactoryBean
 */
public class XmlBeansMarshaller extends AbstractMarshaller {

	private XmlOptions xmlOptions;

	private boolean validating = false;


	/**
	 * Set the {@code XmlOptions}.
	 * @see XmlOptionsFactoryBean
	 */
	public void setXmlOptions(XmlOptions xmlOptions) {
		this.xmlOptions = xmlOptions;
	}

	/**
	 * Return the {@code XmlOptions}.
	 */
	public XmlOptions getXmlOptions() {
		return this.xmlOptions;
	}

	/**
	 * Set whether this marshaller should validate in- and outgoing documents.
	 * Default is {@code false}.
	 */
	public void setValidating(boolean validating) {
		this.validating = validating;
	}

	/**
	 * Return whether this marshaller should validate in- and outgoing documents.
	 */
	public boolean isValidating() {
		return this.validating;
	}


	/**
	 * This implementation returns true if the given class is an implementation of {@link XmlObject}.
	 */
	@Override
	public boolean supports(Class<?> clazz) {
		return XmlObject.class.isAssignableFrom(clazz);
	}

	@Override
	protected final void marshalDomNode(Object graph, Node node) throws XmlMappingException {
		Document document = node.getNodeType() == Node.DOCUMENT_NODE ? (Document) node : node.getOwnerDocument();
		Node xmlBeansNode = ((XmlObject) graph).newDomNode(getXmlOptions());
		NodeList xmlBeansChildNodes = xmlBeansNode.getChildNodes();
		for (int i = 0; i < xmlBeansChildNodes.getLength(); i++) {
			Node xmlBeansChildNode = xmlBeansChildNodes.item(i);
			Node importedNode = document.importNode(xmlBeansChildNode, true);
			node.appendChild(importedNode);
		}
	}

	@Override
	protected final void marshalOutputStream(Object graph, OutputStream outputStream)
			throws XmlMappingException, IOException {

		((XmlObject) graph).save(outputStream, getXmlOptions());
	}

	@Override
	protected final void marshalSaxHandlers(Object graph, ContentHandler contentHandler, LexicalHandler lexicalHandler)
			throws XmlMappingException {
		try {
			((XmlObject) graph).save(contentHandler, lexicalHandler, getXmlOptions());
		}
		catch (SAXException ex) {
			throw convertXmlBeansException(ex, true);
		}
	}

	@Override
	protected final void marshalWriter(Object graph, Writer writer) throws XmlMappingException, IOException {
		((XmlObject) graph).save(writer, getXmlOptions());
	}

	@Override
	protected final void marshalXmlEventWriter(Object graph, XMLEventWriter eventWriter) {
		ContentHandler contentHandler = StaxUtils.createContentHandler(eventWriter);
		marshalSaxHandlers(graph, contentHandler, null);
	}

	@Override
	protected final void marshalXmlStreamWriter(Object graph, XMLStreamWriter streamWriter) throws XmlMappingException {
		ContentHandler contentHandler = StaxUtils.createContentHandler(streamWriter);
		marshalSaxHandlers(graph, contentHandler, null);
	}

	@Override
	protected final Object unmarshalDomNode(Node node) throws XmlMappingException {
		try {
			XmlObject object = XmlObject.Factory.parse(node, getXmlOptions());
			validate(object);
			return object;
		}
		catch (XmlException ex) {
			throw convertXmlBeansException(ex, false);
		}
	}

	@Override
	protected final Object unmarshalInputStream(InputStream inputStream) throws XmlMappingException, IOException {
		try {
			InputStream nonClosingInputStream = new NonClosingInputStream(inputStream);
			XmlObject object = XmlObject.Factory.parse(nonClosingInputStream, getXmlOptions());
			validate(object);
			return object;
		}
		catch (XmlException ex) {
			throw convertXmlBeansException(ex, false);
		}
	}

	@Override
	protected final Object unmarshalReader(Reader reader) throws XmlMappingException, IOException {
		try {
			Reader nonClosingReader = new NonClosingReader(reader);
			XmlObject object = XmlObject.Factory.parse(nonClosingReader, getXmlOptions());
			validate(object);
			return object;
		}
		catch (XmlException ex) {
			throw convertXmlBeansException(ex, false);
		}
	}

	@Override
	protected final Object unmarshalSaxReader(XMLReader xmlReader, InputSource inputSource)
			throws XmlMappingException, IOException {
		XmlSaxHandler saxHandler = XmlObject.Factory.newXmlSaxHandler(getXmlOptions());
		xmlReader.setContentHandler(saxHandler.getContentHandler());
		try {
			xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", saxHandler.getLexicalHandler());
		}
		catch (SAXNotRecognizedException e) {
			// ignore
		}
		catch (SAXNotSupportedException e) {
			// ignore
		}
		try {
			xmlReader.parse(inputSource);
			XmlObject object = saxHandler.getObject();
			validate(object);
			return object;
		}
		catch (SAXException ex) {
			throw convertXmlBeansException(ex, false);
		}
		catch (XmlException ex) {
			throw convertXmlBeansException(ex, false);
		}
	}

	@Override
	protected final Object unmarshalXmlEventReader(XMLEventReader eventReader) throws XmlMappingException {
		XMLReader reader = StaxUtils.createXMLReader(eventReader);
		try {
			return unmarshalSaxReader(reader, new InputSource());
		}
		catch (IOException ex) {
			throw convertXmlBeansException(ex, false);
		}
	}

	@Override
	protected final Object unmarshalXmlStreamReader(XMLStreamReader streamReader) throws XmlMappingException {
		try {
			XmlObject object = XmlObject.Factory.parse(streamReader, getXmlOptions());
			validate(object);
			return object;
		}
		catch (XmlException ex) {
			throw convertXmlBeansException(ex, false);
		}
	}


	/**
	 * Validate the given {@code XmlObject}.
	 * @param object the xml object to validate
	 * @throws ValidationFailureException if the given object is not valid
	 */
	protected void validate(XmlObject object) throws ValidationFailureException {
		if (isValidating() && object != null) {
			// create a temporary xmlOptions just for validation
			XmlOptions validateOptions = getXmlOptions() != null ? getXmlOptions() : new XmlOptions();
			List errorsList = new ArrayList();
			validateOptions.setErrorListener(errorsList);
			if (!object.validate(validateOptions)) {
				StringBuilder builder = new StringBuilder("Could not validate XmlObject :");
				for (Object anErrorsList : errorsList) {
					XmlError xmlError = (XmlError) anErrorsList;
					if (xmlError instanceof XmlValidationError) {
						builder.append(xmlError.toString());
					}
				}
				throw new ValidationFailureException("XMLBeans validation failure",
						new XmlException(builder.toString(), null, errorsList));
			}
		}
	}

	/**
	 * Convert the given XMLBeans exception to an appropriate exception from the
	 * {@code org.springframework.oxm} hierarchy.
	 * <p>A boolean flag is used to indicate whether this exception occurs during marshalling or
	 * unmarshalling, since XMLBeans itself does not make this distinction in its exception hierarchy.
	 * @param ex XMLBeans Exception that occured
	 * @param marshalling indicates whether the exception occurs during marshalling ({@code true}),
	 * or unmarshalling ({@code false})
	 * @return the corresponding {@code XmlMappingException}
	 */
	protected XmlMappingException convertXmlBeansException(Exception ex, boolean marshalling) {
		if (ex instanceof XMLStreamValidationException) {
			return new ValidationFailureException("XmlBeans validation exception", ex);
		}
		else if (ex instanceof XmlException || ex instanceof SAXException) {
			if (marshalling) {
				return new MarshallingFailureException("XMLBeans marshalling exception",  ex);
			}
			else {
				return new UnmarshallingFailureException("XMLBeans unmarshalling exception", ex);
			}
		}
		else {
			// fallback
			return new UncategorizedMappingException("Unknown XMLBeans exception", ex);
		}
	}

	/**
	 * See SPR-7034
	 */
	private static class NonClosingInputStream extends InputStream {

		private final WeakReference<InputStream> in;

		private NonClosingInputStream(InputStream in) {
			this.in = new WeakReference<InputStream>(in);
		}

		private InputStream getInputStream() {
			return this.in.get();
		}

		@Override
		public int read() throws IOException {
			InputStream in = getInputStream();
			return in != null ? in.read() : -1;
		}

		@Override
		public int read(byte[] b) throws IOException {
			InputStream in = getInputStream();
			return in != null ? in.read(b) : -1;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			InputStream in = getInputStream();
			return in != null ? in.read(b, off, len) : -1;
		}

		@Override
		public long skip(long n) throws IOException {
			InputStream in = getInputStream();
			return in != null ? in.skip(n) : 0;
		}

		@Override
		public boolean markSupported() {
			InputStream in = getInputStream();
			return in != null && in.markSupported();
		}

		@Override
		public void mark(int readlimit) {
			InputStream in = getInputStream();
			if (in != null) {
				in.mark(readlimit);
			}
		}

		@Override
		public void reset() throws IOException {
			InputStream in = getInputStream();
			if (in != null) {
				in.reset();
			}
		}

		@Override
		public int available() throws IOException {
			InputStream in = getInputStream();
			return in != null ? in.available() : 0;
		}

		@Override
		public void close() throws IOException {
			InputStream in = getInputStream();
			if(in != null) {
			  this.in.clear();
			}
		}
	}

	private static class NonClosingReader extends Reader {

		private final WeakReference<Reader> reader;

		private NonClosingReader(Reader reader) {
			this.reader = new WeakReference<Reader>(reader);
		}

		private Reader getReader() {
			return this.reader.get();
		}

		@Override
		public int read(CharBuffer target) throws IOException {
			Reader rdr = getReader();
			return rdr != null ? rdr.read(target) : -1;
		}

		@Override
		public int read() throws IOException {
			Reader rdr = getReader();
			return rdr != null ? rdr.read() : -1;
		}

		@Override
		public int read(char[] cbuf) throws IOException {
			Reader rdr = getReader();
			return rdr != null ? rdr.read(cbuf) : -1;
		}

		@Override
		public int read(char[] cbuf, int off, int len) throws IOException {
			Reader rdr = getReader();
			return rdr != null ? rdr.read(cbuf, off, len) : -1;
		}

		@Override
		public long skip(long n) throws IOException {
			Reader rdr = getReader();
			return rdr != null ? rdr.skip(n) : 0;
		}

		@Override
		public boolean ready() throws IOException {
			Reader rdr = getReader();
			return rdr != null && rdr.ready();
		}

		@Override
		public boolean markSupported() {
			Reader rdr = getReader();
			return rdr != null && rdr.markSupported();
		}

		@Override
		public void mark(int readAheadLimit) throws IOException {
			Reader rdr = getReader();
			if (rdr != null) {
				rdr.mark(readAheadLimit);
			}
		}

		@Override
		public void reset() throws IOException {
			Reader rdr = getReader();
			if (rdr != null) {
				rdr.reset();
			}
		}

		@Override
		public void close() throws IOException {
			Reader rdr = getReader();
			if (rdr != null) {
				this.reader.clear();
			}
		}

	}

}
