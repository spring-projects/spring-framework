/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.oxm.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.UnmarshallingFailureException;
import org.springframework.oxm.XmlMappingException;
import org.springframework.util.Assert;
import org.springframework.util.xml.StaxUtils;

/**
 * Abstract implementation of the {@code Marshaller} and {@code Unmarshaller} interface.
 * This implementation inspects the given {@code Source} or {@code Result}, and
 * delegates further handling to overridable template methods.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public abstract class AbstractMarshaller implements Marshaller, Unmarshaller {

	private static final EntityResolver NO_OP_ENTITY_RESOLVER =
			(publicId, systemId) -> new InputSource(new StringReader(""));

	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	private boolean supportDtd = false;

	private boolean processExternalEntities = false;

	private volatile @Nullable DocumentBuilderFactory documentBuilderFactory;

	private volatile @Nullable SAXParserFactory saxParserFactory;


	/**
	 * Indicate whether DTD parsing should be supported.
	 * <p>Default is {@code false} meaning that DTD is disabled.
	 */
	public void setSupportDtd(boolean supportDtd) {
		this.supportDtd = supportDtd;
		this.documentBuilderFactory = null;
		this.saxParserFactory = null;
	}

	/**
	 * Return whether DTD parsing is supported.
	 */
	public boolean isSupportDtd() {
		return this.supportDtd;
	}

	/**
	 * Indicate whether external XML entities are processed when unmarshalling.
	 * <p>Default is {@code false}, meaning that external entities are not resolved.
	 * Note that processing of external entities will only be enabled/disabled when the
	 * {@code Source} passed to {@link #unmarshal(Source)} is a {@link SAXSource} or
	 * {@link StreamSource}. It has no effect for {@link DOMSource} or {@link StAXSource}
	 * instances.
	 * <p><strong>Note:</strong> setting this option to {@code true} also
	 * automatically sets {@link #setSupportDtd} to {@code true}.
	 */
	public void setProcessExternalEntities(boolean processExternalEntities) {
		this.processExternalEntities = processExternalEntities;
		if (processExternalEntities) {
			this.supportDtd = true;
		}
		this.documentBuilderFactory = null;
		this.saxParserFactory = null;
	}

	/**
	 * Return whether XML external entities are allowed.
	 * @see #createXmlReader()
	 */
	public boolean isProcessExternalEntities() {
		return this.processExternalEntities;
	}


	/**
	 * Build a new {@link Document} from this marshaller's {@link DocumentBuilderFactory},
	 * as a placeholder for a DOM node.
	 * @see #createDocumentBuilderFactory()
	 * @see #createDocumentBuilder(DocumentBuilderFactory)
	 */
	protected Document buildDocument() {
		try {
			DocumentBuilderFactory builderFactory = this.documentBuilderFactory;
			if (builderFactory == null) {
				builderFactory = createDocumentBuilderFactory();
				this.documentBuilderFactory = builderFactory;
			}
			DocumentBuilder builder = createDocumentBuilder(builderFactory);
			return builder.newDocument();
		}
		catch (ParserConfigurationException ex) {
			throw new UnmarshallingFailureException("Could not create document placeholder: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Create a {@code DocumentBuilder} that this marshaller will use for creating
	 * DOM documents when passed an empty {@code DOMSource}.
	 * <p>The resulting {@code DocumentBuilderFactory} is cached, so this method
	 * will only be called once.
	 * @return the DocumentBuilderFactory
	 * @throws ParserConfigurationException if thrown by JAXP methods
	 */
	protected DocumentBuilderFactory createDocumentBuilderFactory() throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", !isSupportDtd());
		factory.setFeature("http://xml.org/sax/features/external-general-entities", isProcessExternalEntities());
		return factory;
	}

	/**
	 * Create a {@code DocumentBuilder} that this marshaller will use for creating
	 * DOM documents when passed an empty {@code DOMSource}.
	 * <p>Can be overridden in subclasses, adding further initialization of the builder.
	 * @param factory the {@code DocumentBuilderFactory} that the DocumentBuilder should be created with
	 * @return the {@code DocumentBuilder}
	 * @throws ParserConfigurationException if thrown by JAXP methods
	 */
	protected DocumentBuilder createDocumentBuilder(DocumentBuilderFactory factory)
			throws ParserConfigurationException {

		DocumentBuilder builder = factory.newDocumentBuilder();
		if (!isProcessExternalEntities()) {
			builder.setEntityResolver(NO_OP_ENTITY_RESOLVER);
		}
		return builder;
	}

	/**
	 * Create an {@code XMLReader} that this marshaller will when passed an empty {@code SAXSource}.
	 * @return the XMLReader
	 * @throws SAXException if thrown by JAXP methods
	 * @throws ParserConfigurationException if thrown by JAXP methods
	 */
	protected XMLReader createXmlReader() throws SAXException, ParserConfigurationException {
		SAXParserFactory parserFactory = this.saxParserFactory;
		if (parserFactory == null) {
			parserFactory = SAXParserFactory.newInstance();
			parserFactory.setNamespaceAware(true);
			parserFactory.setFeature(
					"http://apache.org/xml/features/disallow-doctype-decl", !isSupportDtd());
			parserFactory.setFeature(
					"http://xml.org/sax/features/external-general-entities", isProcessExternalEntities());
			this.saxParserFactory = parserFactory;
		}
		SAXParser saxParser = parserFactory.newSAXParser();
		XMLReader xmlReader = saxParser.getXMLReader();
		if (!isProcessExternalEntities()) {
			xmlReader.setEntityResolver(NO_OP_ENTITY_RESOLVER);
		}
		return xmlReader;
	}

	/**
	 * Determine the default encoding to use for marshalling or unmarshalling from
	 * a byte stream, or {@code null} if none.
	 * <p>The default implementation returns {@code null}.
	 */
	protected @Nullable String getDefaultEncoding() {
		return null;
	}


	// Marshalling

	/**
	 * Marshals the object graph with the given root into the provided {@code javax.xml.transform.Result}.
	 * <p>This implementation inspects the given result, and calls {@code marshalDomResult},
	 * {@code marshalSaxResult}, or {@code marshalStreamResult}.
	 * @param graph the root of the object graph to marshal
	 * @param result the result to marshal to
	 * @throws IOException if an I/O exception occurs
	 * @throws XmlMappingException if the given object cannot be marshalled to the result
	 * @throws IllegalArgumentException if {@code result} if neither a {@code DOMResult},
	 * a {@code SAXResult}, nor a {@code StreamResult}
	 * @see #marshalDomResult(Object, javax.xml.transform.dom.DOMResult)
	 * @see #marshalSaxResult(Object, javax.xml.transform.sax.SAXResult)
	 * @see #marshalStreamResult(Object, javax.xml.transform.stream.StreamResult)
	 */
	@Override
	public final void marshal(Object graph, Result result) throws IOException, XmlMappingException {
		if (result instanceof DOMResult domResult) {
			marshalDomResult(graph, domResult);
		}
		else if (StaxUtils.isStaxResult(result)) {
			marshalStaxResult(graph, result);
		}
		else if (result instanceof SAXResult saxResult) {
			marshalSaxResult(graph, saxResult);
		}
		else if (result instanceof StreamResult streamResult) {
			marshalStreamResult(graph, streamResult);
		}
		else {
			throw new IllegalArgumentException("Unknown Result type: " + result.getClass());
		}
	}

	/**
	 * Template method for handling {@code DOMResult}s.
	 * <p>This implementation delegates to {@code marshalDomNode}.
	 * @param graph the root of the object graph to marshal
	 * @param domResult the {@code DOMResult}
	 * @throws XmlMappingException if the given object cannot be marshalled to the result
	 * @throws IllegalArgumentException if the {@code domResult} is empty
	 * @see #marshalDomNode(Object, org.w3c.dom.Node)
	 */
	protected void marshalDomResult(Object graph, DOMResult domResult) throws XmlMappingException {
		if (domResult.getNode() == null) {
			domResult.setNode(buildDocument());
		}
		marshalDomNode(graph, domResult.getNode());
	}

	/**
	 * Template method for handling {@code StaxResult}s.
	 * <p>This implementation delegates to {@code marshalXMLSteamWriter} or
	 * {@code marshalXMLEventConsumer}, depending on what is contained in the
	 * {@code StaxResult}.
	 * @param graph the root of the object graph to marshal
	 * @param staxResult a JAXP 1.4 {@link StAXSource}
	 * @throws XmlMappingException if the given object cannot be marshalled to the result
	 * @throws IllegalArgumentException if the {@code domResult} is empty
	 * @see #marshalDomNode(Object, org.w3c.dom.Node)
	 */
	protected void marshalStaxResult(Object graph, Result staxResult) throws XmlMappingException {
		XMLStreamWriter streamWriter = StaxUtils.getXMLStreamWriter(staxResult);
		if (streamWriter != null) {
			marshalXmlStreamWriter(graph, streamWriter);
		}
		else {
			XMLEventWriter eventWriter = StaxUtils.getXMLEventWriter(staxResult);
			if (eventWriter != null) {
				marshalXmlEventWriter(graph, eventWriter);
			}
			else {
				throw new IllegalArgumentException("StaxResult contains neither XMLStreamWriter nor XMLEventConsumer");
			}
		}
	}

	/**
	 * Template method for handling {@code SAXResult}s.
	 * <p>This implementation delegates to {@code marshalSaxHandlers}.
	 * @param graph the root of the object graph to marshal
	 * @param saxResult the {@code SAXResult}
	 * @throws XmlMappingException if the given object cannot be marshalled to the result
	 * @see #marshalSaxHandlers(Object, org.xml.sax.ContentHandler, org.xml.sax.ext.LexicalHandler)
	 */
	protected void marshalSaxResult(Object graph, SAXResult saxResult) throws XmlMappingException {
		ContentHandler contentHandler = saxResult.getHandler();
		Assert.notNull(contentHandler, "ContentHandler not set on SAXResult");
		LexicalHandler lexicalHandler = saxResult.getLexicalHandler();
		marshalSaxHandlers(graph, contentHandler, lexicalHandler);
	}

	/**
	 * Template method for handling {@code StreamResult}s.
	 * <p>This implementation delegates to {@code marshalOutputStream} or {@code marshalWriter},
	 * depending on what is contained in the {@code StreamResult}
	 * @param graph the root of the object graph to marshal
	 * @param streamResult the {@code StreamResult}
	 * @throws IOException if an I/O Exception occurs
	 * @throws XmlMappingException if the given object cannot be marshalled to the result
	 * @throws IllegalArgumentException if {@code streamResult} does neither
	 * contain an {@code OutputStream} nor a {@code Writer}
	 */
	protected void marshalStreamResult(Object graph, StreamResult streamResult)
			throws XmlMappingException, IOException {

		if (streamResult.getOutputStream() != null) {
			marshalOutputStream(graph, streamResult.getOutputStream());
		}
		else if (streamResult.getWriter() != null) {
			marshalWriter(graph, streamResult.getWriter());
		}
		else {
			throw new IllegalArgumentException("StreamResult contains neither OutputStream nor Writer");
		}
	}


	// Unmarshalling

	/**
	 * Unmarshals the given provided {@code javax.xml.transform.Source} into an object graph.
	 * <p>This implementation inspects the given result, and calls {@code unmarshalDomSource},
	 * {@code unmarshalSaxSource}, or {@code unmarshalStreamSource}.
	 * @param source the source to marshal from
	 * @return the object graph
	 * @throws IOException if an I/O Exception occurs
	 * @throws XmlMappingException if the given source cannot be mapped to an object
	 * @throws IllegalArgumentException if {@code source} is neither a {@code DOMSource},
	 * a {@code SAXSource}, nor a {@code StreamSource}
	 * @see #unmarshalDomSource(javax.xml.transform.dom.DOMSource)
	 * @see #unmarshalSaxSource(javax.xml.transform.sax.SAXSource)
	 * @see #unmarshalStreamSource(javax.xml.transform.stream.StreamSource)
	 */
	@Override
	public final Object unmarshal(Source source) throws IOException, XmlMappingException {
		if (source instanceof DOMSource domSource) {
			return unmarshalDomSource(domSource);
		}
		else if (StaxUtils.isStaxSource(source)) {
			return unmarshalStaxSource(source);
		}
		else if (source instanceof SAXSource saxSource) {
			return unmarshalSaxSource(saxSource);
		}
		else if (source instanceof StreamSource streamSource) {
			return unmarshalStreamSource(streamSource);
		}
		else {
			throw new IllegalArgumentException("Unknown Source type: " + source.getClass());
		}
	}

	/**
	 * Template method for handling {@code DOMSource}s.
	 * <p>This implementation delegates to {@code unmarshalDomNode}.
	 * If the given source is empty, an empty source {@code Document}
	 * will be created as a placeholder.
	 * @param domSource the {@code DOMSource}
	 * @return the object graph
	 * @throws XmlMappingException if the given source cannot be mapped to an object
	 * @throws IllegalArgumentException if the {@code domSource} is empty
	 * @see #unmarshalDomNode(org.w3c.dom.Node)
	 */
	protected Object unmarshalDomSource(DOMSource domSource) throws XmlMappingException {
		if (domSource.getNode() == null) {
			domSource.setNode(buildDocument());
		}
		try {
			return unmarshalDomNode(domSource.getNode());
		}
		catch (NullPointerException ex) {
			if (!isSupportDtd()) {
				throw new UnmarshallingFailureException("NPE while unmarshalling. " +
						"This can happen on JDK 1.6 due to the presence of DTD " +
						"declarations, which are disabled.", ex);
			}
			throw ex;
		}
	}

	/**
	 * Template method for handling {@code StaxSource}s.
	 * <p>This implementation delegates to {@code unmarshalXmlStreamReader} or
	 * {@code unmarshalXmlEventReader}.
	 * @param staxSource the {@code StaxSource}
	 * @return the object graph
	 * @throws XmlMappingException if the given source cannot be mapped to an object
	 */
	protected Object unmarshalStaxSource(Source staxSource) throws XmlMappingException {
		XMLStreamReader streamReader = StaxUtils.getXMLStreamReader(staxSource);
		if (streamReader != null) {
			return unmarshalXmlStreamReader(streamReader);
		}
		else {
			XMLEventReader eventReader = StaxUtils.getXMLEventReader(staxSource);
			if (eventReader != null) {
				return unmarshalXmlEventReader(eventReader);
			}
			else {
				throw new IllegalArgumentException("StaxSource contains neither XMLStreamReader nor XMLEventReader");
			}
		}
	}

	/**
	 * Template method for handling {@code SAXSource}s.
	 * <p>This implementation delegates to {@code unmarshalSaxReader}.
	 * @param saxSource the {@code SAXSource}
	 * @return the object graph
	 * @throws XmlMappingException if the given source cannot be mapped to an object
	 * @throws IOException if an I/O Exception occurs
	 * @see #unmarshalSaxReader(org.xml.sax.XMLReader, org.xml.sax.InputSource)
	 */
	protected Object unmarshalSaxSource(SAXSource saxSource) throws XmlMappingException, IOException {
		if (saxSource.getXMLReader() == null) {
			try {
				saxSource.setXMLReader(createXmlReader());
			}
			catch (SAXException | ParserConfigurationException ex) {
				throw new UnmarshallingFailureException("Could not create XMLReader for SAXSource", ex);
			}
		}
		if (saxSource.getInputSource() == null) {
			saxSource.setInputSource(new InputSource());
		}
		try {
			return unmarshalSaxReader(saxSource.getXMLReader(), saxSource.getInputSource());
		}
		catch (NullPointerException ex) {
			if (!isSupportDtd()) {
				throw new UnmarshallingFailureException("NPE while unmarshalling. " +
						"This can happen on JDK 1.6 due to the presence of DTD " +
						"declarations, which are disabled.");
			}
			throw ex;
		}
	}

	/**
	 * Template method for handling {@code StreamSource}s.
	 * <p>This implementation delegates to {@code unmarshalInputStream} or {@code unmarshalReader}.
	 * @param streamSource the {@code StreamSource}
	 * @return the object graph
	 * @throws IOException if an I/O exception occurs
	 * @throws XmlMappingException if the given source cannot be mapped to an object
	 */
	protected Object unmarshalStreamSource(StreamSource streamSource) throws XmlMappingException, IOException {
		if (streamSource.getInputStream() != null) {
			if (isProcessExternalEntities() && isSupportDtd()) {
				return unmarshalInputStream(streamSource.getInputStream());
			}
			else {
				InputSource inputSource = new InputSource(streamSource.getInputStream());
				inputSource.setEncoding(getDefaultEncoding());
				return unmarshalSaxSource(new SAXSource(inputSource));
			}
		}
		else if (streamSource.getReader() != null) {
			if (isProcessExternalEntities() && isSupportDtd()) {
				return unmarshalReader(streamSource.getReader());
			}
			else {
				return unmarshalSaxSource(new SAXSource(new InputSource(streamSource.getReader())));
			}
		}
		else {
			return unmarshalSaxSource(new SAXSource(new InputSource(streamSource.getSystemId())));
		}
	}


	// Abstract template methods

	/**
	 * Abstract template method for marshalling the given object graph to a DOM {@code Node}.
	 * <p>In practice, {@code node} is a {@code Document} node, a {@code DocumentFragment} node,
	 * or a {@code Element} node. In other words, a node that accepts children.
	 * @param graph the root of the object graph to marshal
	 * @param node the DOM node that will contain the result tree
	 * @throws XmlMappingException if the given object cannot be marshalled to the DOM node
	 * @see org.w3c.dom.Document
	 * @see org.w3c.dom.DocumentFragment
	 * @see org.w3c.dom.Element
	 */
	protected abstract void marshalDomNode(Object graph, Node node)
			throws XmlMappingException;

	/**
	 * Abstract template method for marshalling the given object to a StAX {@code XMLEventWriter}.
	 * @param graph the root of the object graph to marshal
	 * @param eventWriter the {@code XMLEventWriter} to write to
	 * @throws XmlMappingException if the given object cannot be marshalled to the DOM node
	 */
	protected abstract void marshalXmlEventWriter(Object graph, XMLEventWriter eventWriter)
			throws XmlMappingException;

	/**
	 * Abstract template method for marshalling the given object to a StAX {@code XMLStreamWriter}.
	 * @param graph the root of the object graph to marshal
	 * @param streamWriter the {@code XMLStreamWriter} to write to
	 * @throws XmlMappingException if the given object cannot be marshalled to the DOM node
	 */
	protected abstract void marshalXmlStreamWriter(Object graph, XMLStreamWriter streamWriter)
			throws XmlMappingException;

	/**
	 * Abstract template method for marshalling the given object graph to a SAX {@code ContentHandler}.
	 * @param graph the root of the object graph to marshal
	 * @param contentHandler the SAX {@code ContentHandler}
	 * @param lexicalHandler the SAX2 {@code LexicalHandler}. Can be {@code null}.
	 * @throws XmlMappingException if the given object cannot be marshalled to the handlers
	 */
	protected abstract void marshalSaxHandlers(
			Object graph, ContentHandler contentHandler, @Nullable LexicalHandler lexicalHandler)
			throws XmlMappingException;

	/**
	 * Abstract template method for marshalling the given object graph to a {@code OutputStream}.
	 * @param graph the root of the object graph to marshal
	 * @param outputStream the {@code OutputStream} to write to
	 * @throws XmlMappingException if the given object cannot be marshalled to the writer
	 * @throws IOException if an I/O exception occurs
	 */
	protected abstract void marshalOutputStream(Object graph, OutputStream outputStream)
			throws XmlMappingException, IOException;

	/**
	 * Abstract template method for marshalling the given object graph to a {@code Writer}.
	 * @param graph the root of the object graph to marshal
	 * @param writer the {@code Writer} to write to
	 * @throws XmlMappingException if the given object cannot be marshalled to the writer
	 * @throws IOException if an I/O exception occurs
	 */
	protected abstract void marshalWriter(Object graph, Writer writer)
			throws XmlMappingException, IOException;

	/**
	 * Abstract template method for unmarshalling from a given DOM {@code Node}.
	 * @param node the DOM node that contains the objects to be unmarshalled
	 * @return the object graph
	 * @throws XmlMappingException if the given DOM node cannot be mapped to an object
	 */
	protected abstract Object unmarshalDomNode(Node node) throws XmlMappingException;

	/**
	 * Abstract template method for unmarshalling from a given Stax {@code XMLEventReader}.
	 * @param eventReader the {@code XMLEventReader} to read from
	 * @return the object graph
	 * @throws XmlMappingException if the given event reader cannot be converted to an object
	 */
	protected abstract Object unmarshalXmlEventReader(XMLEventReader eventReader)
			throws XmlMappingException;

	/**
	 * Abstract template method for unmarshalling from a given Stax {@code XMLStreamReader}.
	 * @param streamReader the {@code XMLStreamReader} to read from
	 * @return the object graph
	 * @throws XmlMappingException if the given stream reader cannot be converted to an object
	 */
	protected abstract Object unmarshalXmlStreamReader(XMLStreamReader streamReader)
			throws XmlMappingException;

	/**
	 * Abstract template method for unmarshalling using a given SAX {@code XMLReader}
	 * and {@code InputSource}.
	 * @param xmlReader the SAX {@code XMLReader} to parse with
	 * @param inputSource the input source to parse from
	 * @return the object graph
	 * @throws XmlMappingException if the given reader and input source cannot be converted to an object
	 * @throws IOException if an I/O exception occurs
	 */
	protected abstract Object unmarshalSaxReader(XMLReader xmlReader, InputSource inputSource)
			throws XmlMappingException, IOException;

	/**
	 * Abstract template method for unmarshalling from a given {@code InputStream}.
	 * @param inputStream the {@code InputStreamStream} to read from
	 * @return the object graph
	 * @throws XmlMappingException if the given stream cannot be converted to an object
	 * @throws IOException if an I/O exception occurs
	 */
	protected abstract Object unmarshalInputStream(InputStream inputStream)
			throws XmlMappingException, IOException;

	/**
	 * Abstract template method for unmarshalling from a given {@code Reader}.
	 * @param reader the {@code Reader} to read from
	 * @return the object graph
	 * @throws XmlMappingException if the given reader cannot be converted to an object
	 * @throws IOException if an I/O exception occurs
	 */
	protected abstract Object unmarshalReader(Reader reader)
			throws XmlMappingException, IOException;

}
