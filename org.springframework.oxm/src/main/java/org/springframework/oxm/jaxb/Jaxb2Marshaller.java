/*
 * Copyright 2006 the original author or authors.
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

package org.springframework.oxm.jaxb;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentMarshaller;
import javax.xml.bind.attachment.AttachmentUnmarshaller;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.mime.MimeContainer;
import org.springframework.oxm.mime.MimeMarshaller;
import org.springframework.oxm.mime.MimeUnmarshaller;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.SaxUtils;
import org.springframework.util.xml.StaxUtils;

/**
 * Implementation of the <code>Marshaller</code> interface for JAXB 2.0.
 *
 * <p>The typical usage will be to set either the <code>contextPath</code> or the <code>classesToBeBound</code> property
 * on this bean, possibly customize the marshaller and unmarshaller by setting properties, schemas, adapters, and
 * listeners, and to refer to it.
 *
 * @author Arjen Poutsma
 * @see #setContextPath(String)
 * @see #setClassesToBeBound(Class[])
 * @see #setJaxbContextProperties(Map)
 * @see #setMarshallerProperties(Map)
 * @see #setUnmarshallerProperties(Map)
 * @see #setSchema(Resource)
 * @see #setSchemas(Resource[])
 * @see #setMarshallerListener(Marshaller.Listener)
 * @see #setUnmarshallerListener(javax.xml.bind.Unmarshaller.Listener)
 * @see #setAdapters(XmlAdapter[])
 * @since 3.0
 */
public class Jaxb2Marshaller implements MimeMarshaller, MimeUnmarshaller, BeanClassLoaderAware, InitializingBean {

	private static final String CID = "cid:";

	/**
	 * Logger available to subclasses.
	 */
	private static final Log logger = LogFactory.getLog(Jaxb2Marshaller.class);

	private String contextPath;

	private Map<String, Object> marshallerProperties;

	private Map<String, Object> unmarshallerProperties;

	private JAXBContext jaxbContext;

	private ValidationEventHandler validationEventHandler;

	private ClassLoader classLoader;

	private Resource[] schemaResources;

	private String schemaLanguage = XMLConstants.W3C_XML_SCHEMA_NS_URI;

	private Marshaller.Listener marshallerListener;

	private Unmarshaller.Listener unmarshallerListener;

	private XmlAdapter[] adapters;

	private Schema schema;

	private Class[] classesToBeBound;

	private Map<String, ?> jaxbContextProperties;

	private boolean mtomEnabled = false;

	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * Sets the <code>XmlAdapter</code>s to be registered with the JAXB <code>Marshaller</code> and
	 * <code>Unmarshaller</code>
	 */
	public void setAdapters(XmlAdapter[] adapters) {
		this.adapters = adapters;
	}

	/**
	 * Sets the JAXB Context path.
	 */
	public void setContextPath(String contextPath) {
		Assert.hasText(contextPath, "'contextPath' must not be null");
		this.contextPath = contextPath;
	}

	/**
	 * Sets multiple JAXB Context paths. The given array of context paths is converted to a colon-delimited string, as
	 * supported by JAXB.
	 */
	public void setContextPaths(String[] contextPaths) {
		Assert.notEmpty(contextPaths, "'contextPaths' must not be empty");
		this.contextPath = StringUtils.arrayToDelimitedString(contextPaths, ":");
	}

	/**
	 * Sets the list of java classes to be recognized by a newly created JAXBContext. Setting this property or
	 * <code>contextPath</code> is required.
	 *
	 * @see #setContextPath(String)
	 */
	public void setClassesToBeBound(Class[] classesToBeBound) {
		this.classesToBeBound = classesToBeBound;
	}

	/**
	 * Sets the <code>JAXBContext</code> properties. These implementation-specific properties will be set on the
	 * <code>JAXBContext</code>.
	 */
	public void setJaxbContextProperties(Map<String, ?> jaxbContextProperties) {
		this.jaxbContextProperties = jaxbContextProperties;
	}

	/**
	 * Sets the <code>Marshaller.Listener</code> to be registered with the JAXB <code>Marshaller</code>.
	 */
	public void setMarshallerListener(Marshaller.Listener marshallerListener) {
		this.marshallerListener = marshallerListener;
	}

	/**
	 * Indicates whether MTOM support should be enabled or not. Default is <code>false</code>, marshalling using XOP/MTOM
	 * is not enabled.
	 */
	public void setMtomEnabled(boolean mtomEnabled) {
		this.mtomEnabled = mtomEnabled;
	}

	/**
	 * Sets the schema language. Default is the W3C XML Schema: <code>http://www.w3.org/2001/XMLSchema"</code>.
	 *
	 * @see XMLConstants#W3C_XML_SCHEMA_NS_URI
	 * @see XMLConstants#RELAXNG_NS_URI
	 */
	public void setSchemaLanguage(String schemaLanguage) {
		this.schemaLanguage = schemaLanguage;
	}

	/**
	 * Sets the schema resource to use for validation.
	 */
	public void setSchema(Resource schemaResource) {
		schemaResources = new Resource[]{schemaResource};
	}

	/**
	 * Sets the schema resources to use for validation.
	 */
	public void setSchemas(Resource[] schemaResources) {
		this.schemaResources = schemaResources;
	}

	/**
	 * Sets the <code>Unmarshaller.Listener</code> to be registered with the JAXB <code>Unmarshaller</code>.
	 */
	public void setUnmarshallerListener(Unmarshaller.Listener unmarshallerListener) {
		this.unmarshallerListener = unmarshallerListener;
	}

	/**
	 * Sets the JAXB <code>Marshaller</code> properties. These properties will be set on the underlying JAXB
	 * <code>Marshaller</code>, and allow for features such as indentation.
	 *
	 * @param properties the properties
	 * @see Marshaller#setProperty(String,Object)
	 * @see Marshaller#JAXB_ENCODING
	 * @see Marshaller#JAXB_FORMATTED_OUTPUT
	 * @see Marshaller#JAXB_NO_NAMESPACE_SCHEMA_LOCATION
	 * @see Marshaller#JAXB_SCHEMA_LOCATION
	 */
	public void setMarshallerProperties(Map<String, Object> properties) {
		this.marshallerProperties = properties;
	}

	/**
	 * Sets the JAXB <code>Unmarshaller</code> properties. These properties will be set on the underlying JAXB
	 * <code>Unmarshaller</code>.
	 *
	 * @param properties the properties
	 * @see javax.xml.bind.Unmarshaller#setProperty(String,Object)
	 */
	public void setUnmarshallerProperties(Map<String, Object> properties) {
		this.unmarshallerProperties = properties;
	}

	/**
	 * Sets the JAXB validation event handler. This event handler will be called by JAXB if any validation errors are
	 * encountered during calls to any of the marshal API's.
	 *
	 * @param validationEventHandler the event handler
	 */
	public void setValidationEventHandler(ValidationEventHandler validationEventHandler) {
		this.validationEventHandler = validationEventHandler;
	}

	public final void afterPropertiesSet() throws Exception {
		if (StringUtils.hasLength(contextPath) && !ObjectUtils.isEmpty(classesToBeBound)) {
			throw new IllegalArgumentException("specify either contextPath or classesToBeBound property; not both");
		}
		try {
			jaxbContext = createJaxbContext();
		}
		catch (JAXBException ex) {
			throw convertJaxbException(ex);
		}
	}

	/*
	 * JAXBContext
	 */

	protected JAXBContext createJaxbContext() throws Exception {
		if (!ObjectUtils.isEmpty(schemaResources)) {
			if (logger.isDebugEnabled()) {
				logger.debug(
						"Setting validation schema to " + StringUtils.arrayToCommaDelimitedString(schemaResources));
			}
			schema = loadSchema(schemaResources, schemaLanguage);
		}
		if (StringUtils.hasLength(contextPath)) {
			return createJaxbContextFromContextPath();
		}
		else if (!ObjectUtils.isEmpty(classesToBeBound)) {
			return createJaxbContextFromClasses();
		}
		else {
			throw new IllegalArgumentException("setting either contextPath or classesToBeBound is required");
		}
	}

	private JAXBContext createJaxbContextFromContextPath() throws JAXBException {
		if (logger.isInfoEnabled()) {
			logger.info("Creating JAXBContext with context path [" + contextPath + "]");
		}
		if (jaxbContextProperties != null) {
			if (classLoader != null) {
				return JAXBContext.newInstance(contextPath, classLoader, jaxbContextProperties);
			}
			else {
				return JAXBContext.newInstance(contextPath, ClassUtils.getDefaultClassLoader(), jaxbContextProperties);
			}
		}
		else {
			if (classLoader != null) {
				return JAXBContext.newInstance(contextPath, classLoader);
			}
			else {
				return JAXBContext.newInstance(contextPath);
			}
		}
	}

	private JAXBContext createJaxbContextFromClasses() throws JAXBException {
		if (logger.isInfoEnabled()) {
			logger.info("Creating JAXBContext with classes to be bound [" +
					StringUtils.arrayToCommaDelimitedString(classesToBeBound) + "]");
		}
		if (jaxbContextProperties != null) {
			return JAXBContext.newInstance(classesToBeBound, jaxbContextProperties);
		}
		else {
			return JAXBContext.newInstance(classesToBeBound);
		}
	}

	private Schema loadSchema(Resource[] resources, String schemaLanguage) throws IOException, SAXException {
		Assert.notEmpty(resources, "No resources given");
		Assert.hasLength(schemaLanguage, "No schema language provided");
		Source[] schemaSources = new Source[resources.length];
		XMLReader xmlReader = XMLReaderFactory.createXMLReader();
		xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
		for (int i = 0; i < resources.length; i++) {
			Assert.notNull(resources[i], "Resource is null");
			Assert.isTrue(resources[i].exists(), "Resource " + resources[i] + " does not exist");
			InputSource inputSource = SaxUtils.createInputSource(resources[i]);
			schemaSources[i] = new SAXSource(xmlReader, inputSource);
		}
		SchemaFactory schemaFactory = SchemaFactory.newInstance(schemaLanguage);
		return schemaFactory.newSchema(schemaSources);
	}

	public boolean supports(Class<?> clazz) {
		if (JAXBElement.class.isAssignableFrom(clazz)) {
			return true;
		}
		else if (clazz.getAnnotation(XmlRootElement.class) != null) {
			return true;
		}
		if (StringUtils.hasLength(contextPath)) {
			String packageName = ClassUtils.getPackageName(clazz);
			String[] contextPaths = StringUtils.tokenizeToStringArray(contextPath, ":");
			for (String contextPath : contextPaths) {
				if (contextPath.equals(packageName)) {
					return true;
				}
			}
			return false;
		}
		else if (!ObjectUtils.isEmpty(classesToBeBound)) {
			return Arrays.asList(classesToBeBound).contains(clazz);
		}
		return false;
	}

	/*
	 * Marshalling
	 */

	/**
	 * Returns a newly created JAXB marshaller. JAXB marshallers are not necessarily thread safe.
	 */
	private Marshaller createMarshaller() {
		try {
			Marshaller marshaller = jaxbContext.createMarshaller();
			initJaxbMarshaller(marshaller);
			return marshaller;
		}
		catch (JAXBException ex) {
			throw convertJaxbException(ex);
		}
	}

	/**
	 * Template method that can be overridden by concrete JAXB marshallers for custom initialization behavior. Gets called
	 * after creation of JAXB <code>Marshaller</code>, and after the respective properties have been set.
	 *
	 * <p>Default implementation sets the {@link #setMarshallerProperties(Map) defined properties}, the {@link
	 * #setValidationEventHandler(ValidationEventHandler) validation event handler}, the {@link #setSchemas(Resource[])
	 * schemas}, {@link #setMarshallerListener(Marshaller.Listener) listener}, and {@link #setAdapters(XmlAdapter[])
	 * adapters}.
	 */
	protected void initJaxbMarshaller(Marshaller marshaller) throws JAXBException {
		if (marshallerProperties != null) {
			for (String name : marshallerProperties.keySet()) {
				marshaller.setProperty(name, marshallerProperties.get(name));
			}
		}
		if (validationEventHandler != null) {
			marshaller.setEventHandler(validationEventHandler);
		}
		if (schema != null) {
			marshaller.setSchema(schema);
		}
		if (marshallerListener != null) {
			marshaller.setListener(marshallerListener);
		}
		if (adapters != null) {
			for (XmlAdapter adapter : adapters) {
				marshaller.setAdapter(adapter);
			}
		}
	}

	public void marshal(Object graph, Result result) throws XmlMappingException {
		marshal(graph, result, null);
	}

	public void marshal(Object graph, Result result, MimeContainer mimeContainer) throws XmlMappingException {
		try {
			Marshaller marshaller = createMarshaller();
			if (mtomEnabled && mimeContainer != null) {
				marshaller.setAttachmentMarshaller(new Jaxb2AttachmentMarshaller(mimeContainer));
			}
			if (StaxUtils.isStaxResult(result)) {
				marshalStaxResult(marshaller, graph, result);
			}
			else {
				marshaller.marshal(graph, result);
			}
		}
		catch (JAXBException ex) {
			throw convertJaxbException(ex);
		}
	}

	private void marshalStaxResult(Marshaller jaxbMarshaller, Object graph, Result staxResult) throws JAXBException {
		XMLStreamWriter streamWriter = StaxUtils.getXMLStreamWriter(staxResult);
		if (streamWriter != null) {
			jaxbMarshaller.marshal(graph, streamWriter);
		}
		else {
			XMLEventWriter eventWriter = StaxUtils.getXMLEventWriter(staxResult);
			if (eventWriter != null) {
				jaxbMarshaller.marshal(graph, eventWriter);
			}
			else {
				throw new IllegalArgumentException("StAX Result contains neither XMLStreamWriter nor XMLEventConsumer");
			}
		}
	}

	/*
	 * Unmarshalling
	 */

	/**
	 * Returns a newly created JAXB unmarshaller. JAXB unmarshallers are not necessarily thread safe.
	 */
	private Unmarshaller createUnmarshaller() {
		try {
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			initJaxbUnmarshaller(unmarshaller);
			return unmarshaller;
		}
		catch (JAXBException ex) {
			throw convertJaxbException(ex);
		}
	}

	/**
	 * Template method that can be overridden by concrete JAXB marshallers for custom initialization behavior. Gets called
	 * after creation of JAXB <code>Marshaller</code>, and after the respective properties have been set.
	 *
	 * <p>Default implementation sets the {@link #setUnmarshallerProperties(Map) defined properties}, the {@link
	 * #setValidationEventHandler(ValidationEventHandler) validation event handler}, the {@link #setSchemas(Resource[])
	 * schemas}, {@link #setUnmarshallerListener(Unmarshaller.Listener) listener}, and {@link #setAdapters(XmlAdapter[])
	 * adapters}.
	 */
	protected void initJaxbUnmarshaller(Unmarshaller unmarshaller) throws JAXBException {
		if (unmarshallerProperties != null) {
			for (String name : unmarshallerProperties.keySet()) {
				unmarshaller.setProperty(name, unmarshallerProperties.get(name));
			}
		}
		if (validationEventHandler != null) {
			unmarshaller.setEventHandler(validationEventHandler);
		}
		if (schema != null) {
			unmarshaller.setSchema(schema);
		}
		if (unmarshallerListener != null) {
			unmarshaller.setListener(unmarshallerListener);
		}
		if (adapters != null) {
			for (XmlAdapter adapter : adapters) {
				unmarshaller.setAdapter(adapter);
			}
		}
	}

	public Object unmarshal(Source source) throws XmlMappingException {
		return unmarshal(source, null);
	}

	public Object unmarshal(Source source, MimeContainer mimeContainer) throws XmlMappingException {
		try {
			Unmarshaller unmarshaller = createUnmarshaller();
			if (mtomEnabled && mimeContainer != null) {
				unmarshaller.setAttachmentUnmarshaller(new Jaxb2AttachmentUnmarshaller(mimeContainer));
			}
			if (StaxUtils.isStaxSource(source)) {
				return unmarshalStaxSource(unmarshaller, source);
			}
			else {
				return unmarshaller.unmarshal(source);
			}
		}
		catch (JAXBException ex) {
			throw convertJaxbException(ex);
		}
	}

	private Object unmarshalStaxSource(Unmarshaller jaxbUnmarshaller, Source staxSource) throws JAXBException {
		XMLStreamReader streamReader = StaxUtils.getXMLStreamReader(staxSource);
		if (streamReader != null) {
			return jaxbUnmarshaller.unmarshal(streamReader);
		}
		else {
			XMLEventReader eventReader = StaxUtils.getXMLEventReader(staxSource);
			if (eventReader != null) {
				return jaxbUnmarshaller.unmarshal(eventReader);
			}
			else {
				throw new IllegalArgumentException("StaxSource contains neither XMLStreamReader nor XMLEventReader");
			}
		}
	}

	/**
	 * Convert the given <code>JAXBException</code> to an appropriate exception from the
	 * <code>org.springframework.oxm</code> hierarchy. <p/> The default implementation delegates to <code>JaxbUtils</code>.
	 * Can be overridden in subclasses.
	 *
	 * @param ex <code>JAXBException</code> that occured
	 * @return the corresponding <code>XmlMappingException</code> instance
	 * @see org.springframework.oxm.jaxb.JaxbUtils#convertJaxbException
	 */
	protected XmlMappingException convertJaxbException(JAXBException ex) {
		return JaxbUtils.convertJaxbException(ex);
	}

	/*
	   * Inner classes
	   */

	private static class Jaxb2AttachmentMarshaller extends AttachmentMarshaller {

		private final MimeContainer mimeContainer;

		private Jaxb2AttachmentMarshaller(MimeContainer mimeContainer) {
			this.mimeContainer = mimeContainer;
		}

		@Override
		public String addMtomAttachment(byte[] data,
				int offset,
				int length,
				String mimeType,
				String elementNamespace,
				String elementLocalName) {
			ByteArrayDataSource dataSource = new ByteArrayDataSource(mimeType, data, offset, length);
			return addMtomAttachment(new DataHandler(dataSource), elementNamespace, elementLocalName);
		}

		@Override
		public String addMtomAttachment(DataHandler dataHandler, String elementNamespace, String elementLocalName) {
			String host = getHost(elementNamespace, dataHandler);
			String contentId = UUID.randomUUID() + "@" + host;
			mimeContainer.addAttachment("<" + contentId + ">", dataHandler);
			try {
				contentId = URLEncoder.encode(contentId, "UTF-8");
			}
			catch (UnsupportedEncodingException e) {
				// ignore
			}
			return CID + contentId;
		}

		private String getHost(String elementNamespace, DataHandler dataHandler) {
			try {
				URI uri = new URI(elementNamespace);
				return uri.getHost();
			}
			catch (URISyntaxException e) {
				// ignore
			}
			return dataHandler.getName();
		}

		@Override
		public String addSwaRefAttachment(DataHandler dataHandler) {
			String contentId = UUID.randomUUID() + "@" + dataHandler.getName();
			mimeContainer.addAttachment(contentId, dataHandler);
			return contentId;
		}

		@Override
		public boolean isXOPPackage() {
			return mimeContainer.convertToXopPackage();
		}
	}

	private static class Jaxb2AttachmentUnmarshaller extends AttachmentUnmarshaller {

		private final MimeContainer mimeContainer;

		private Jaxb2AttachmentUnmarshaller(MimeContainer mimeContainer) {
			this.mimeContainer = mimeContainer;
		}

		@Override
		public byte[] getAttachmentAsByteArray(String cid) {
			try {
				DataHandler dataHandler = getAttachmentAsDataHandler(cid);
				return FileCopyUtils.copyToByteArray(dataHandler.getInputStream());
			}
			catch (IOException ex) {
				throw new JaxbUnmarshallingFailureException(ex);
			}
		}

		@Override
		public DataHandler getAttachmentAsDataHandler(String contentId) {
			if (contentId.startsWith(CID)) {
				contentId = contentId.substring(CID.length());
				try {
					contentId = URLDecoder.decode(contentId, "UTF-8");
				}
				catch (UnsupportedEncodingException e) {
					// ignore
				}
				contentId = '<' + contentId + '>';
			}
			return mimeContainer.getAttachment(contentId);
		}

		@Override
		public boolean isXOPPackage() {
			return mimeContainer.isXopPackage();
		}
	}

	/**
	 * DataSource that wraps around a byte array.
	 */
	private static class ByteArrayDataSource implements DataSource {

		private byte[] data;

		private String contentType;

		private int offset;

		private int length;

		private ByteArrayDataSource(String contentType, byte[] data, int offset, int length) {
			this.contentType = contentType;
			this.data = data;
			this.offset = offset;
			this.length = length;
		}

		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(data, offset, length);
		}

		public OutputStream getOutputStream() throws IOException {
			throw new UnsupportedOperationException();
		}

		public String getContentType() {
			return contentType;
		}

		public String getName() {
			return "ByteArrayDataSource";
		}
	}

}

