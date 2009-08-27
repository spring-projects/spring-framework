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

package org.springframework.oxm.castor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.ResolverException;
import org.exolab.castor.xml.UnmarshalHandler;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.ValidationException;
import org.exolab.castor.xml.XMLContext;
import org.exolab.castor.xml.XMLException;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.oxm.MarshallingFailureException;
import org.springframework.oxm.UncategorizedMappingException;
import org.springframework.oxm.UnmarshallingFailureException;
import org.springframework.oxm.ValidationFailureException;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.support.AbstractMarshaller;
import org.springframework.oxm.support.SaxResourceUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.util.xml.StaxUtils;

/**
 * Implementation of the <code>Marshaller</code> interface for Castor. By default, Castor does not require any further
 * configuration, though setting target classes, target packages or providing a mapping file can be used to have more control over the
 * behavior of Castor.
 *
 * <p>If a target class is specified using <code>setTargetClass</code>, the <code>CastorMarshaller</code> can only be
 * used to unmarshall XML that represents that specific class. If you want to unmarshall multiple classes, you have to
 * provide a mapping file using <code>setMappingLocations</code>.
 *
 * <p>Due to limitations of Castor's API, it is required to set the encoding used for writing to output streams. It
 * defaults to <code>UTF-8</code>.
 *
 * @author Arjen Poutsma
 * @see #setEncoding(String)
 * @see #setTargetClass(Class)
 * @see #setTargetPackages(String[])
 * @see #setMappingLocation(Resource)
 * @see #setMappingLocations(Resource[])
 * @since 3.0
 */
public class CastorMarshaller extends AbstractMarshaller implements InitializingBean {

	/**
	 * The default encoding used for stream access: UTF-8.
	 */
	public static final String DEFAULT_ENCODING = "UTF-8";


	private Resource[] mappingLocations;

	private String encoding = DEFAULT_ENCODING;

	private Class[] targetClasses;

	private String[] targetPackages;

	private boolean validating = false;

	private boolean whitespacePreserve = false;

	private boolean ignoreExtraAttributes = true;

	private boolean ignoreExtraElements = false;

	private Map<String, String> namespaceMappings;

	private XMLContext xmlContext;

	private boolean suppressNamespaces = false;

	private boolean suppressXsiType = false;


	/**
	 * Set the encoding to be used for stream access.
	 * @see #DEFAULT_ENCODING
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Set the locations of the Castor XML Mapping files.
	 */
	public void setMappingLocation(Resource mappingLocation) {
		this.mappingLocations = new Resource[] {mappingLocation};
	}

	/**
	 * Set the locations of the Castor XML Mapping files.
	 */
	public void setMappingLocations(Resource[] mappingLocations) {
		this.mappingLocations = mappingLocations;
	}

	/**
	 * Set the Castor target class. Alternative means of configuring
	 * <code>CastorMarshaller<code> for unmarshalling multiple classes include
	 * use of mapping files, and specifying packages with Castor descriptor classes.
	 */
	public void setTargetClass(Class targetClass) {
		this.targetClasses = new Class[]{targetClass};
	}

	/**
	 * Set the Castor target classes. Alternative means of configuring
	 * <code>CastorMarshaller<code> for unmarshalling multiple classes include
	 * use of mapping files, and specifying packages with Castor descriptor classes.
	 */
	public void setTargetClasses(Class[] targetClasses) {
		this.targetClasses = targetClasses;
	}

	/**
	 * Set the package names of packages with the Castor descriptor classes.
	 */
	public void setTargetPackages(String[] targetPackages) {
		this.targetPackages = targetPackages;
	}

	/**
	 * Set whether this marshaller should validate in- and outgoing documents.
	 * <p>Default is <code>false</code>.
	 * @see Marshaller#setValidation(boolean)
	 */
	public void setValidating(boolean validating) {
		this.validating = validating;
	}

	/**
	 * Set whether the Castor {@link Unmarshaller} should preserve "ignorable" whitespace.
	 * <p>Default is <code>false</code>.
	 * @see org.exolab.castor.xml.Unmarshaller#setWhitespacePreserve(boolean)
	 */
	public void setWhitespacePreserve(boolean whitespacePreserve) {
		this.whitespacePreserve = whitespacePreserve;
	}

	/**
	 * Set whether the Castor {@link Unmarshaller} should ignore attributes that do not match a specific field.
	 * <p>Default is <code>true</code>: extra attributes are ignored.
	 * @see org.exolab.castor.xml.Unmarshaller#setIgnoreExtraAttributes(boolean)
	 */
	public void setIgnoreExtraAttributes(boolean ignoreExtraAttributes) {
		this.ignoreExtraAttributes = ignoreExtraAttributes;
	}

	/**
	 * Set whether the Castor {@link Unmarshaller} should ignore elements that do not match a specific field.
	 * <p>Default is <code>false</code>, extra attributes are flagged as an error.
	 * @see org.exolab.castor.xml.Unmarshaller#setIgnoreExtraElements(boolean)
	 */
	public void setIgnoreExtraElements(boolean ignoreExtraElements) {
		this.ignoreExtraElements = ignoreExtraElements;
	}

	/**
	 * Set the namespace mappings. Property names are interpreted as namespace prefixes; values are namespace URIs.
	 * @see org.exolab.castor.xml.Marshaller#setNamespaceMapping(String, String)
	 */
	public void setNamespaceMappings(Map<String, String> namespaceMappings) {
		this.namespaceMappings = namespaceMappings;
	}

	/** Returns whether this marshaller should output namespaces. */
	public boolean isSuppressNamespaces() {
		return suppressNamespaces;
	}

	/**
	 * Sets whether this marshaller should output namespaces. The default is {@code false}, i.e. namespaces are
	 * written.
	 *
	 * @see org.exolab.castor.xml.Marshaller#setSuppressNamespaces(boolean)
	 */
	public void setSuppressNamespaces(boolean suppressNamespaces) {
		this.suppressNamespaces = suppressNamespaces;
	}

	/** Sets whether this marshaller should output the xsi:type attribute. */
	public boolean isSuppressXsiType() {
		return suppressXsiType;
	}

	/**
	 * Sets whether this marshaller should output the {@code xsi:type} attribute. The default is {@code false}, i.e. the
	 * {@code xsi:type} is written.
	 *
	 * @see org.exolab.castor.xml.Marshaller#setSuppressXSIType(boolean)
	 */
	public void setSuppressXsiType(boolean suppressXsiType) {
		this.suppressXsiType = suppressXsiType;
	}

	public final void afterPropertiesSet() throws CastorMappingException, IOException {
		if (logger.isInfoEnabled()) {
			if (!ObjectUtils.isEmpty(this.mappingLocations)) {
				logger.info(
						"Configured using [" + StringUtils.arrayToCommaDelimitedString(this.mappingLocations) + "]");
			}
			if (!ObjectUtils.isEmpty(this.targetClasses)) {
				logger.info("Configured for target classes " + StringUtils.arrayToCommaDelimitedString(targetClasses) +
						"]");
			}
			if (!ObjectUtils.isEmpty(this.targetPackages)) {
				logger.info(
						"Configured for target packages [" + StringUtils.arrayToCommaDelimitedString(targetPackages) +
								"]");
			}
			if (ObjectUtils.isEmpty(this.mappingLocations) && ObjectUtils.isEmpty(this.targetClasses) &&
					ObjectUtils.isEmpty(this.targetPackages)) {
				logger.info("Using default configuration");
			}
		}
		try {
			this.xmlContext = createXMLContext(this.mappingLocations, this.targetClasses, this.targetPackages);
		}
		catch (MappingException ex) {
			throw new CastorMappingException("Could not load Castor mapping", ex);
		}
		catch (ResolverException ex) {
			throw new CastorMappingException("Could not resolve Castor mapping", ex);
		}
	}

	/**
	 * Create the Castor <code>XMLContext</code>. Subclasses can override this to create a custom context.
	 * <p>
	 * The default implementation loads mapping files if defined, or the target class or packages if defined.
	 *
	 * @return the created resolver
	 * @throws MappingException when the mapping file cannot be loaded
	 * @throws IOException	  in case of I/O errors
	 * @see XMLContext#addMapping(org.exolab.castor.mapping.Mapping)
	 * @see XMLContext#addClass(Class)
	 */
	protected XMLContext createXMLContext(Resource[] mappingLocations, Class[] targetClasses, String[] targetPackages)
			throws MappingException, ResolverException, IOException {

		XMLContext context = new XMLContext();
		if (!ObjectUtils.isEmpty(mappingLocations)) {
			Mapping mapping = new Mapping();
			for (Resource mappingLocation : mappingLocations) {
				mapping.loadMapping(SaxResourceUtils.createInputSource(mappingLocation));
			}
			context.addMapping(mapping);
		}
		if (!ObjectUtils.isEmpty(targetClasses)) {
			context.addClasses(targetClasses);
		}
		if (!ObjectUtils.isEmpty(targetPackages)) {
			context.addPackages(targetPackages);
		}
		return context;
	}

	/**
	 * Returns <code>true</code> for all classes, i.e. Castor supports arbitrary classes.
	 */
	public boolean supports(Class<?> clazz) {
		return true;
	}

	// Marshalling

	@Override
	protected final void marshalDomNode(Object graph, Node node) throws XmlMappingException {
		marshalSaxHandlers(graph, DomUtils.createContentHandler(node), null);
	}

	@Override
	protected final void marshalSaxHandlers(Object graph, ContentHandler contentHandler, LexicalHandler lexicalHandler)
			throws XmlMappingException {
		Marshaller marshaller = xmlContext.createMarshaller();
		marshaller.setContentHandler(contentHandler);
		marshal(graph, marshaller);
	}

	@Override
	protected final void marshalOutputStream(Object graph, OutputStream outputStream)
			throws XmlMappingException, IOException {
		marshalWriter(graph, new OutputStreamWriter(outputStream, encoding));
	}

	@Override
	protected final void marshalWriter(Object graph, Writer writer) throws XmlMappingException, IOException {
		Marshaller marshaller = xmlContext.createMarshaller();
		marshaller.setWriter(writer);
		marshal(graph, marshaller);
	}

	@Override
	protected final void marshalXmlEventWriter(Object graph, XMLEventWriter eventWriter) throws XmlMappingException {
		marshalSaxHandlers(graph, StaxUtils.createContentHandler(eventWriter), null);
	}

	@Override
	protected final void marshalXmlStreamWriter(Object graph, XMLStreamWriter streamWriter) throws XmlMappingException {
		marshalSaxHandlers(graph, StaxUtils.createContentHandler(streamWriter), null);
	}

	private void marshal(Object graph, Marshaller marshaller) {
		try {
			customizeMarshaller(marshaller);
			marshaller.marshal(graph);
		}
		catch (XMLException ex) {
			throw convertCastorException(ex, true);
		}
	}

	/**
	 * Template method that allows for customizing of the given Castor {@link Marshaller}.
	 * <p>The default implementation invokes {@link Marshaller#setValidation(boolean)}
	 * with the property set on this marshaller, and calls {@link Marshaller#setNamespaceMapping(String, String)}
	 * with the {@linkplain #setNamespaceMappings(java.util.Map) namespace mappings}.
	 */
	protected void customizeMarshaller(Marshaller marshaller) {
		marshaller.setValidation(this.validating);
		marshaller.setSuppressNamespaces(isSuppressNamespaces());
		marshaller.setSuppressXSIType(isSuppressXsiType());
		if (this.namespaceMappings != null) {
			for (Map.Entry<String, String> entry : namespaceMappings.entrySet()) {
				marshaller.setNamespaceMapping(entry.getKey(), entry.getValue());
			}
		}
	}


	// Unmarshalling

	@Override
	protected final Object unmarshalDomNode(Node node) throws XmlMappingException {
		try {
			return createUnmarshaller().unmarshal(node);
		}
		catch (XMLException ex) {
			throw convertCastorException(ex, false);
		}
	}

	@Override
	protected final Object unmarshalInputStream(InputStream inputStream) throws XmlMappingException, IOException {
		try {
			return createUnmarshaller().unmarshal(new InputSource(inputStream));
		}
		catch (XMLException ex) {
			throw convertCastorException(ex, false);
		}
	}

	@Override
	protected final Object unmarshalReader(Reader reader) throws XmlMappingException, IOException {
		try {
			return createUnmarshaller().unmarshal(new InputSource(reader));
		}
		catch (XMLException ex) {
			throw convertCastorException(ex, false);
		}
	}

	@Override
	protected final Object unmarshalXmlEventReader(XMLEventReader eventReader) {
		XMLReader reader = StaxUtils.createXMLReader(eventReader);
		try {
			return unmarshalSaxReader(reader, new InputSource());
		}
		catch (IOException ex) {
			throw new UnmarshallingFailureException("Failed to read XML stream", ex);
		}
	}

	@Override
	protected final Object unmarshalSaxReader(XMLReader xmlReader, InputSource inputSource)
			throws XmlMappingException, IOException {

		UnmarshalHandler unmarshalHandler = createUnmarshaller().createHandler();
		try {
			ContentHandler contentHandler = Unmarshaller.getContentHandler(unmarshalHandler);
			xmlReader.setContentHandler(contentHandler);
			xmlReader.parse(inputSource);
			return unmarshalHandler.getObject();
		}
		catch (SAXException ex) {
			throw new UnmarshallingFailureException("SAX reader exception", ex);
		}
	}

	@Override
	protected final Object unmarshalXmlStreamReader(XMLStreamReader streamReader) {
		XMLReader reader = StaxUtils.createXMLReader(streamReader);
		try {
			return unmarshalSaxReader(reader, new InputSource());
		}
		catch (IOException ex) {
			throw new UnmarshallingFailureException("Failed to read XML stream", ex);
		}
	}

	private Unmarshaller createUnmarshaller() {
		Unmarshaller unmarshaller = this.xmlContext.createUnmarshaller();
		customizeUnmarshaller(unmarshaller);
		return unmarshaller;
	}

	/**
	 * Template method that allows for customizing of the given Castor
	 * {@link Unmarshaller}.
	 * <p>
	 * The default implementation invokes
	 * {@link Unmarshaller#setValidation(boolean)},
	 * {@link Unmarshaller#setWhitespacePreserve(boolean)},
	 * {@link Unmarshaller#setIgnoreExtraAttributes(boolean)}, and
	 * {@link Unmarshaller#setIgnoreExtraElements(boolean)} with the properties
	 * set on this marshaller.
	 */
	protected void customizeUnmarshaller(Unmarshaller unmarshaller) {
		unmarshaller.setValidation(this.validating);
		unmarshaller.setWhitespacePreserve(this.whitespacePreserve);
		unmarshaller.setIgnoreExtraAttributes(this.ignoreExtraAttributes);
		unmarshaller.setIgnoreExtraElements(this.ignoreExtraElements);
	}

	/**
	 * Convert the given <code>XMLException</code> to an appropriate exception
	 * from the <code>org.springframework.oxm</code> hierarchy.
	 * <p>
	 * A boolean flag is used to indicate whether this exception occurs during
	 * marshalling or unmarshalling, since Castor itself does not make this
	 * distinction in its exception hierarchy.
	 *
	 * @param ex		  Castor <code>XMLException</code> that occured
	 * @param marshalling indicates whether the exception occurs during
	 *                    marshalling (<code>true</code>), or unmarshalling (<code>false</code>)
	 * @return the corresponding <code>XmlMappingException</code>
	 */
	protected XmlMappingException convertCastorException(XMLException ex, boolean marshalling) {
		if (ex instanceof ValidationException) {
			return new ValidationFailureException("Castor validation exception", ex);
		}
		else if (ex instanceof MarshalException) {
			if (marshalling) {
				return new MarshallingFailureException("Castor marshalling exception", ex);
			}
			else {
				return new UnmarshallingFailureException("Castor unmarshalling exception", ex);
			}
		}
		else {
			// fallback
			return new UncategorizedMappingException("Unknown Castor exception", ex);
		}
	}

}
