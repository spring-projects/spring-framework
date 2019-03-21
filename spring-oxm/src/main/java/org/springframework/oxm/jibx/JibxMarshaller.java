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

package org.springframework.oxm.jibx;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.IXMLReader;
import org.jibx.runtime.IXMLWriter;
import org.jibx.runtime.JiBXException;
import org.jibx.runtime.ValidationException;
import org.jibx.runtime.impl.MarshallingContext;
import org.jibx.runtime.impl.StAXReaderWrapper;
import org.jibx.runtime.impl.StAXWriter;
import org.jibx.runtime.impl.UnmarshallingContext;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.oxm.MarshallingFailureException;
import org.springframework.oxm.UnmarshallingFailureException;
import org.springframework.oxm.ValidationFailureException;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.support.AbstractMarshaller;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.StaxUtils;

/**
 * Implementation of the {@code Marshaller} and {@code Unmarshaller} interfaces for JiBX.
 *
 * <p>The typical usage will be to set the {@code targetClass} and optionally the
 * {@code bindingName} property on this bean.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see org.jibx.runtime.IMarshallingContext
 * @see org.jibx.runtime.IUnmarshallingContext
 */
public class JibxMarshaller extends AbstractMarshaller implements InitializingBean {

	private static final String DEFAULT_BINDING_NAME = "binding";


	@Nullable
	private Class<?> targetClass;

	@Nullable
	private String targetPackage;

	@Nullable
	private String bindingName;

	private int indent = -1;

	private String encoding = "UTF-8";

	@Nullable
	private Boolean standalone;

	@Nullable
	private String docTypeRootElementName;

	@Nullable
	private String docTypeSystemId;

	@Nullable
	private String docTypePublicId;

	@Nullable
	private String docTypeInternalSubset;

	@Nullable
	private IBindingFactory bindingFactory;

	private final TransformerFactory transformerFactory = TransformerFactory.newInstance();


	/**
	 * Set the target class for this instance. Setting either this property or the
	 * {@link #setTargetPackage(String) targetPackage} property is required.
	 * <p>If this property is set, {@link #setTargetPackage(String) targetPackage} is ignored.
	 */
	public void setTargetClass(Class<?> targetClass) {
		this.targetClass = targetClass;
	}

	/**
	 * Set the target package for this instance. Setting either this property or the
	 * {@link #setTargetClass(Class) targetClass} property is required.
	 * <p>If {@link #setTargetClass(Class) targetClass} is set, this property is ignored.
	 */
	public void setTargetPackage(String targetPackage) {
		this.targetPackage = targetPackage;
	}

	/**
	 * Set the optional binding name for this instance.
	 */
	public void setBindingName(String bindingName) {
		this.bindingName = bindingName;
	}

	/**
	 * Set the number of nesting indent spaces. Default is {@code -1}, i.e. no indentation.
	 */
	public void setIndent(int indent) {
		this.indent = indent;
	}

	/**
	 * Set the document encoding using for marshalling. Default is UTF-8.
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	@Override
	protected String getDefaultEncoding() {
		return this.encoding;
	}

	/**
	 * Set the document standalone flag for marshalling. By default, this flag is not present.
	 */
	public void setStandalone(Boolean standalone) {
		this.standalone = standalone;
	}

	/**
	 * Set the root element name for the DTD declaration written when marshalling.
	 * By default, this is {@code null} (i.e. no DTD declaration is written).
	 * <p>If set to a value, the system ID or public ID also need to be set.
	 * @see #setDocTypeSystemId(String)
	 * @see #setDocTypePublicId(String)
	 */
	public void setDocTypeRootElementName(String docTypeRootElementName) {
		this.docTypeRootElementName = docTypeRootElementName;
	}

	/**
	 * Set the system id for the DTD declaration written when marshalling.
	 * By default, this is {@code null}. Only used when the root element also has been set.
	 * <p>Set either this property or {@code docTypePublicId}, not both.
	 * @see #setDocTypeRootElementName(String)
	 */
	public void setDocTypeSystemId(String docTypeSystemId) {
		this.docTypeSystemId = docTypeSystemId;
	}

	/**
	 * Set the public id for the DTD declaration written when marshalling.
	 * By default, this is {@code null}. Only used when the root element also has been set.
	 * <p>Set either this property or {@code docTypeSystemId}, not both.
	 * @see #setDocTypeRootElementName(String)
	 */
	public void setDocTypePublicId(String docTypePublicId) {
		this.docTypePublicId = docTypePublicId;
	}

	/**
	 * Set the internal subset Id for the DTD declaration written when marshalling.
	 * By default, this is {@code null}. Only used when the root element also has been set.
	 * @see #setDocTypeRootElementName(String)
	 */
	public void setDocTypeInternalSubset(String docTypeInternalSubset) {
		this.docTypeInternalSubset = docTypeInternalSubset;
	}


	@Override
	public void afterPropertiesSet() throws JiBXException {
		if (this.targetClass != null) {
			if (StringUtils.hasLength(this.bindingName)) {
				if (logger.isInfoEnabled()) {
					logger.info("Configured for target class [" + this.targetClass +
							"] using binding [" + this.bindingName + "]");
				}
				this.bindingFactory = BindingDirectory.getFactory(this.bindingName, this.targetClass);
			}
			else {
				if (logger.isInfoEnabled()) {
					logger.info("Configured for target class [" + this.targetClass + "]");
				}
				this.bindingFactory = BindingDirectory.getFactory(this.targetClass);
			}
		}
		else if (this.targetPackage != null) {
			if (!StringUtils.hasLength(this.bindingName)) {
				this.bindingName = DEFAULT_BINDING_NAME;
			}
			if (logger.isInfoEnabled()) {
				logger.info("Configured for target package [" + this.targetPackage +
						"] using binding [" + this.bindingName + "]");
			}
			this.bindingFactory = BindingDirectory.getFactory(this.bindingName, this.targetPackage);
		}
		else {
			throw new IllegalArgumentException("Either 'targetClass' or 'targetPackage' is required");
		}
	}


	@Override
	public boolean supports(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		if (this.targetClass != null) {
			return (this.targetClass == clazz);
		}
		Assert.state(this.bindingFactory != null, "JibxMarshaller not initialized");
		String[] mappedClasses = this.bindingFactory.getMappedClasses();
		String className = clazz.getName();
		for (String mappedClass : mappedClasses) {
			if (className.equals(mappedClass)) {
				return true;
			}
		}
		return false;
	}


	// Supported marshalling

	@Override
	protected void marshalOutputStream(Object graph, OutputStream outputStream)
			throws XmlMappingException, IOException {
		try {
			IMarshallingContext marshallingContext = createMarshallingContext();
			marshallingContext.startDocument(this.encoding, this.standalone, outputStream);
			marshalDocument(marshallingContext, graph);
		}
		catch (JiBXException ex) {
			throw convertJibxException(ex, true);
		}
	}

	@Override
	protected void marshalWriter(Object graph, Writer writer) throws XmlMappingException, IOException {
		try {
			IMarshallingContext marshallingContext = createMarshallingContext();
			marshallingContext.startDocument(this.encoding, this.standalone, writer);
			marshalDocument(marshallingContext, graph);
		}
		catch (JiBXException ex) {
			throw convertJibxException(ex, true);
		}
	}

	private void marshalDocument(IMarshallingContext marshallingContext, Object graph) throws IOException, JiBXException {
		if (StringUtils.hasLength(this.docTypeRootElementName)) {
			IXMLWriter xmlWriter = marshallingContext.getXmlWriter();
			xmlWriter.writeDocType(this.docTypeRootElementName, this.docTypeSystemId,
					this.docTypePublicId, this.docTypeInternalSubset);
		}
		marshallingContext.marshalDocument(graph);
	}


	// Unsupported marshalling

	@Override
	protected void marshalDomNode(Object graph, Node node) throws XmlMappingException {
		try {
			// JiBX does not support DOM natively, so we write to a buffer first, and transform that to the Node
			Result result = new DOMResult(node);
			transformAndMarshal(graph, result);
		}
		catch (IOException ex) {
			throw new MarshallingFailureException("JiBX marshalling exception", ex);
		}
	}

	@Override
	protected void marshalXmlEventWriter(Object graph, XMLEventWriter eventWriter) {
		XMLStreamWriter streamWriter = StaxUtils.createEventStreamWriter(eventWriter);
		marshalXmlStreamWriter(graph, streamWriter);
	}

	@Override
	protected void marshalXmlStreamWriter(Object graph, XMLStreamWriter streamWriter) throws XmlMappingException {
		try {
			MarshallingContext marshallingContext = (MarshallingContext) createMarshallingContext();
			IXMLWriter xmlWriter = new StAXWriter(marshallingContext.getNamespaces(), streamWriter);
			marshallingContext.setXmlWriter(xmlWriter);
			marshallingContext.marshalDocument(graph);
		}
		catch (JiBXException ex) {
			throw convertJibxException(ex, false);
		}
	}

	@Override
	protected void marshalSaxHandlers(Object graph, ContentHandler contentHandler, @Nullable LexicalHandler lexicalHandler)
			throws XmlMappingException {
		try {
			// JiBX does not support SAX natively, so we write to a buffer first, and transform that to the handlers
			SAXResult saxResult = new SAXResult(contentHandler);
			saxResult.setLexicalHandler(lexicalHandler);
			transformAndMarshal(graph, saxResult);
		}
		catch (IOException ex) {
			throw new MarshallingFailureException("JiBX marshalling exception", ex);
		}
	}

	private void transformAndMarshal(Object graph, Result result) throws IOException {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
			marshalOutputStream(graph, os);
			ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
			Transformer transformer = this.transformerFactory.newTransformer();
			transformer.transform(new StreamSource(is), result);
		}
		catch (TransformerException ex) {
			throw new MarshallingFailureException(
					"Could not transform to [" + ClassUtils.getShortName(result.getClass()) + "]", ex);
		}

	}


	// Unmarshalling

	@Override
	protected Object unmarshalXmlEventReader(XMLEventReader eventReader) {
		try {
			XMLStreamReader streamReader = StaxUtils.createEventStreamReader(eventReader);
			return unmarshalXmlStreamReader(streamReader);
		}
		catch (XMLStreamException ex) {
			return new UnmarshallingFailureException("JiBX unmarshalling exception", ex);
		}
	}

	@Override
	protected Object unmarshalXmlStreamReader(XMLStreamReader streamReader) {
		try {
			UnmarshallingContext unmarshallingContext = (UnmarshallingContext) createUnmarshallingContext();
			IXMLReader xmlReader = new StAXReaderWrapper(streamReader, null, true);
			unmarshallingContext.setDocument(xmlReader);
			return unmarshallingContext.unmarshalElement();
		}
		catch (JiBXException ex) {
			throw convertJibxException(ex, false);
		}
	}

	@Override
	protected Object unmarshalInputStream(InputStream inputStream) throws XmlMappingException, IOException {
		try {
			IUnmarshallingContext unmarshallingContext = createUnmarshallingContext();
			return unmarshallingContext.unmarshalDocument(inputStream, this.encoding);
		}
		catch (JiBXException ex) {
			throw convertJibxException(ex, false);
		}
	}

	@Override
	protected Object unmarshalReader(Reader reader) throws XmlMappingException, IOException {
		try {
			IUnmarshallingContext unmarshallingContext = createUnmarshallingContext();
			return unmarshallingContext.unmarshalDocument(reader);
		}
		catch (JiBXException ex) {
			throw convertJibxException(ex, false);
		}
	}


	// Unsupported Unmarshalling

	@Override
	protected Object unmarshalDomNode(Node node) throws XmlMappingException {
		try {
			return transformAndUnmarshal(new DOMSource(node), null);
		}
		catch (IOException ex) {
			throw new UnmarshallingFailureException("JiBX unmarshalling exception", ex);
		}
	}

	@Override
	protected Object unmarshalSaxReader(XMLReader xmlReader, InputSource inputSource)
			throws XmlMappingException, IOException {

		return transformAndUnmarshal(new SAXSource(xmlReader, inputSource), inputSource.getEncoding());
	}

	private Object transformAndUnmarshal(Source source, @Nullable String encoding) throws IOException {
		try {
			Transformer transformer = this.transformerFactory.newTransformer();
			if (encoding != null) {
				transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
			}
			ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
			transformer.transform(source, new StreamResult(os));
			ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
			return unmarshalInputStream(is);
		}
		catch (TransformerException ex) {
			throw new MarshallingFailureException(
					"Could not transform from [" + ClassUtils.getShortName(source.getClass()) + "]", ex);
		}
	}


	/**
	 * Create a new {@code IMarshallingContext}, configured with the correct indentation.
	 * @return the created marshalling context
	 * @throws JiBXException in case of errors
	 */
	protected IMarshallingContext createMarshallingContext() throws JiBXException {
		Assert.state(this.bindingFactory != null, "JibxMarshaller not initialized");
		IMarshallingContext marshallingContext = this.bindingFactory.createMarshallingContext();
		marshallingContext.setIndent(this.indent);
		return marshallingContext;
	}

	/**
	 * Create a new {@code IUnmarshallingContext}.
	 * @return the created unmarshalling context
	 * @throws JiBXException in case of errors
	 */
	protected IUnmarshallingContext createUnmarshallingContext() throws JiBXException {
		Assert.state(this.bindingFactory != null, "JibxMarshaller not initialized");
		return this.bindingFactory.createUnmarshallingContext();
	}

	/**
	 * Convert the given {@code JiBXException} to an appropriate exception from the
	 * {@code org.springframework.oxm} hierarchy.
	 * <p>A boolean flag is used to indicate whether this exception occurs during marshalling or
	 * unmarshalling, since JiBX itself does not make this distinction in its exception hierarchy.
	 * @param ex {@code JiBXException} that occurred
	 * @param marshalling indicates whether the exception occurs during marshalling ({@code true}),
	 * or unmarshalling ({@code false})
	 * @return the corresponding {@code XmlMappingException}
	 */
	public XmlMappingException convertJibxException(JiBXException ex, boolean marshalling) {
		if (ex instanceof ValidationException) {
			return new ValidationFailureException("JiBX validation exception", ex);
		}
		else {
			if (marshalling) {
				return new MarshallingFailureException("JiBX marshalling exception", ex);
			}
			else {
				return new UnmarshallingFailureException("JiBX unmarshalling exception", ex);
			}
		}
	}

}
