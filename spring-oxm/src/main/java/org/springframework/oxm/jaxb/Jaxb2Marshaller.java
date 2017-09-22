/*
 * Copyright 2002-2017 the original author or authors.
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

import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentMarshaller;
import javax.xml.bind.attachment.AttachmentUnmarshaller;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.oxm.GenericMarshaller;
import org.springframework.oxm.GenericUnmarshaller;
import org.springframework.oxm.MarshallingFailureException;
import org.springframework.oxm.UncategorizedMappingException;
import org.springframework.oxm.UnmarshallingFailureException;
import org.springframework.oxm.ValidationFailureException;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.mime.MimeContainer;
import org.springframework.oxm.mime.MimeMarshaller;
import org.springframework.oxm.mime.MimeUnmarshaller;
import org.springframework.oxm.support.SaxResourceUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.StaxUtils;

/**
 * Implementation of the {@code GenericMarshaller} interface for JAXB 2.2.
 *
 * <p>The typical usage will be to set either the "contextPath" or the "classesToBeBound"
 * property on this bean, possibly customize the marshaller and unmarshaller by setting
 * properties, schemas, adapters, and listeners, and to refer to it.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 3.0
 * @see #setContextPath(String)
 * @see #setClassesToBeBound(Class[])
 * @see #setJaxbContextProperties(Map)
 * @see #setMarshallerProperties(Map)
 * @see #setUnmarshallerProperties(Map)
 * @see #setSchema(Resource)
 * @see #setSchemas(Resource[])
 * @see #setMarshallerListener(javax.xml.bind.Marshaller.Listener)
 * @see #setUnmarshallerListener(javax.xml.bind.Unmarshaller.Listener)
 * @see #setAdapters(XmlAdapter[])
 */
public class Jaxb2Marshaller implements MimeMarshaller, MimeUnmarshaller, GenericMarshaller, GenericUnmarshaller,
		BeanClassLoaderAware, InitializingBean {

	private static final String CID = "cid:";


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private String contextPath;

	@Nullable
	private Class<?>[] classesToBeBound;

	@Nullable
	private String[] packagesToScan;

	@Nullable
	private Map<String, ?> jaxbContextProperties;

	@Nullable
	private Map<String, ?> marshallerProperties;

	@Nullable
	private Map<String, ?> unmarshallerProperties;

	@Nullable
	private Marshaller.Listener marshallerListener;

	@Nullable
	private Unmarshaller.Listener unmarshallerListener;

	@Nullable
	private ValidationEventHandler validationEventHandler;

	@Nullable
	private XmlAdapter<?, ?>[] adapters;

	@Nullable
	private Resource[] schemaResources;

	private String schemaLanguage = XMLConstants.W3C_XML_SCHEMA_NS_URI;

	@Nullable
	private LSResourceResolver schemaResourceResolver;

	private boolean lazyInit = false;

	private boolean mtomEnabled = false;

	private boolean supportJaxbElementClass = false;

	private boolean checkForXmlRootElement = true;

	@Nullable
	private Class<?> mappedClass;

	@Nullable
	private ClassLoader beanClassLoader;

	private final Object jaxbContextMonitor = new Object();

	@Nullable
	private volatile JAXBContext jaxbContext;

	@Nullable
	private Schema schema;

	private boolean supportDtd = false;

	private boolean processExternalEntities = false;


	/**
	 * Set multiple JAXB context paths. The given array of context paths gets
	 * converted to a colon-delimited string, as supported by JAXB.
	 */
	public void setContextPaths(String... contextPaths) {
		Assert.notEmpty(contextPaths, "'contextPaths' must not be empty");
		this.contextPath = StringUtils.arrayToDelimitedString(contextPaths, ":");
	}

	/**
	 * Set a JAXB context path.
	 * <p>Setting either this property, {@link #setClassesToBeBound "classesToBeBound"}
	 * or {@link #setPackagesToScan "packagesToScan"} is required.
	 */
	public void setContextPath(@Nullable String contextPath) {
		this.contextPath = contextPath;
	}

	/**
	 * Return the JAXB context path.
	 */
	@Nullable
	public String getContextPath() {
		return this.contextPath;
	}

	/**
	 * Set the list of Java classes to be recognized by a newly created JAXBContext.
	 * <p>Setting either this property, {@link #setContextPath "contextPath"}
	 * or {@link #setPackagesToScan "packagesToScan"} is required.
	 */
	public void setClassesToBeBound(@Nullable Class<?>... classesToBeBound) {
		this.classesToBeBound = classesToBeBound;
	}

	/**
	 * Return the list of Java classes to be recognized by a newly created JAXBContext.
	 */
	@Nullable
	public Class<?>[] getClassesToBeBound() {
		return this.classesToBeBound;
	}

	/**
	 * Set the packages to search for classes with JAXB2 annotations in the classpath.
	 * This is using a Spring-bases search and therefore analogous to Spring's component-scan
	 * feature ({@link org.springframework.context.annotation.ClassPathBeanDefinitionScanner}).
	 * <p>Setting either this property, {@link #setContextPath "contextPath"}
	 * or {@link #setClassesToBeBound "classesToBeBound"} is required.
	 */
	public void setPackagesToScan(@Nullable String... packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	/**
	 * Return the packages to search for JAXB2 annotations.
	 */
	@Nullable
	public String[] getPackagesToScan() {
		return this.packagesToScan;
	}

	/**
	 * Set the {@code JAXBContext} properties. These implementation-specific
	 * properties will be set on the underlying {@code JAXBContext}.
	 */
	public void setJaxbContextProperties(Map<String, ?> jaxbContextProperties) {
		this.jaxbContextProperties = jaxbContextProperties;
	}

	/**
	 * Set the JAXB {@code Marshaller} properties. These properties will be set on the
	 * underlying JAXB {@code Marshaller}, and allow for features such as indentation.
	 * @param properties the properties
	 * @see javax.xml.bind.Marshaller#setProperty(String, Object)
	 * @see javax.xml.bind.Marshaller#JAXB_ENCODING
	 * @see javax.xml.bind.Marshaller#JAXB_FORMATTED_OUTPUT
	 * @see javax.xml.bind.Marshaller#JAXB_NO_NAMESPACE_SCHEMA_LOCATION
	 * @see javax.xml.bind.Marshaller#JAXB_SCHEMA_LOCATION
	 */
	public void setMarshallerProperties(Map<String, ?> properties) {
		this.marshallerProperties = properties;
	}

	/**
	 * Set the JAXB {@code Unmarshaller} properties. These properties will be set on the
	 * underlying JAXB {@code Unmarshaller}.
	 * @param properties the properties
	 * @see javax.xml.bind.Unmarshaller#setProperty(String, Object)
	 */
	public void setUnmarshallerProperties(Map<String, ?> properties) {
		this.unmarshallerProperties = properties;
	}

	/**
	 * Specify the {@code Marshaller.Listener} to be registered with the JAXB {@code Marshaller}.
	 */
	public void setMarshallerListener(Marshaller.Listener marshallerListener) {
		this.marshallerListener = marshallerListener;
	}

	/**
	 * Set the {@code Unmarshaller.Listener} to be registered with the JAXB {@code Unmarshaller}.
	 */
	public void setUnmarshallerListener(Unmarshaller.Listener unmarshallerListener) {
		this.unmarshallerListener = unmarshallerListener;
	}

	/**
	 * Set the JAXB validation event handler. This event handler will be called by JAXB
	 * if any validation errors are encountered during calls to any of the marshal APIs.
	 */
	public void setValidationEventHandler(ValidationEventHandler validationEventHandler) {
		this.validationEventHandler = validationEventHandler;
	}

	/**
	 * Specify the {@code XmlAdapter}s to be registered with the JAXB {@code Marshaller}
	 * and {@code Unmarshaller}
	 */
	public void setAdapters(XmlAdapter<?, ?>... adapters) {
		this.adapters = adapters;
	}

	/**
	 * Set the schema resource to use for validation.
	 */
	public void setSchema(Resource schemaResource) {
		this.schemaResources = new Resource[] {schemaResource};
	}

	/**
	 * Set the schema resources to use for validation.
	 */
	public void setSchemas(Resource... schemaResources) {
		this.schemaResources = schemaResources;
	}

	/**
	 * Set the schema language. Default is the W3C XML Schema: {@code http://www.w3.org/2001/XMLSchema"}.
	 * @see XMLConstants#W3C_XML_SCHEMA_NS_URI
	 * @see XMLConstants#RELAXNG_NS_URI
	 */
	public void setSchemaLanguage(String schemaLanguage) {
		this.schemaLanguage = schemaLanguage;
	}

	/**
	 * Set the resource resolver, as used to load the schema resources.
	 * @see SchemaFactory#setResourceResolver(org.w3c.dom.ls.LSResourceResolver)
	 * @see #setSchema(Resource)
	 * @see #setSchemas(Resource[])
	 */
	public void setSchemaResourceResolver(LSResourceResolver schemaResourceResolver) {
		this.schemaResourceResolver = schemaResourceResolver;
	}

	/**
	 * Set whether to lazily initialize the {@link JAXBContext} for this marshaller.
	 * Default is {@code false} to initialize on startup; can be switched to {@code true}.
	 * <p>Early initialization just applies if {@link #afterPropertiesSet()} is called.
	 */
	public void setLazyInit(boolean lazyInit) {
		this.lazyInit = lazyInit;
	}

	/**
	 * Specify whether MTOM support should be enabled or not.
	 * Default is {@code false}: marshalling using XOP/MTOM not being enabled.
	 */
	public void setMtomEnabled(boolean mtomEnabled) {
		this.mtomEnabled = mtomEnabled;
	}

	/**
	 * Specify whether the {@link #supports(Class)} returns {@code true} for the {@link JAXBElement} class.
	 * <p>Default is {@code false}, meaning that {@code supports(Class)} always returns {@code false} for
	 * {@code JAXBElement} classes (though {@link #supports(Type)} can return {@code true}, since it can
	 * obtain the type parameters of {@code JAXBElement}).
	 * <p>This property is typically enabled in combination with usage of classes like
	 * {@link org.springframework.web.servlet.view.xml.MarshallingView MarshallingView},
	 * since the {@code ModelAndView} does not offer type parameter information at runtime.
	 * @see #supports(Class)
	 * @see #supports(Type)
	 */
	public void setSupportJaxbElementClass(boolean supportJaxbElementClass) {
		this.supportJaxbElementClass = supportJaxbElementClass;
	}

	/**
	 * Specify whether the {@link #supports(Class)} should check for
	 * {@link XmlRootElement @XmlRootElement} annotations.
	 * <p>Default is {@code true}, meaning that {@code supports(Class)} will check for
	 * this annotation. However, some JAXB implementations (i.e. EclipseLink MOXy) allow
	 * for defining the bindings in an external definition file, thus keeping the classes
	 * annotations free. Setting this property to {@code false} supports these
	 * JAXB implementations.
	 * @see #supports(Class)
	 * @see #supports(Type)
	 */
	public void setCheckForXmlRootElement(boolean checkForXmlRootElement) {
		this.checkForXmlRootElement = checkForXmlRootElement;
	}

	/**
	 * Specify a JAXB mapped class for partial unmarshalling.
	 * @see javax.xml.bind.Unmarshaller#unmarshal(javax.xml.transform.Source, Class)
	 */
	public void setMappedClass(Class<?> mappedClass) {
		this.mappedClass = mappedClass;
	}

	/**
	 * Indicates whether DTD parsing should be supported.
	 * <p>Default is {@code false} meaning that DTD is disabled.
	 */
	public void setSupportDtd(boolean supportDtd) {
		this.supportDtd = supportDtd;
	}

	/**
	 * Whether DTD parsing is supported.
	 */
	public boolean isSupportDtd() {
		return this.supportDtd;
	}

	/**
	 * Indicates whether external XML entities are processed when unmarshalling.
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
			setSupportDtd(true);
		}
	}

	/**
	 * Returns the configured value for whether XML external entities are allowed.
	 */
	public boolean isProcessExternalEntities() {
		return this.processExternalEntities;
	}


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		boolean hasContextPath = StringUtils.hasLength(this.contextPath);
		boolean hasClassesToBeBound = !ObjectUtils.isEmpty(this.classesToBeBound);
		boolean hasPackagesToScan = !ObjectUtils.isEmpty(this.packagesToScan);

		if (hasContextPath && (hasClassesToBeBound || hasPackagesToScan) ||
				(hasClassesToBeBound && hasPackagesToScan)) {
			throw new IllegalArgumentException("Specify either 'contextPath', 'classesToBeBound', " +
					"or 'packagesToScan'");
		}
		if (!hasContextPath && !hasClassesToBeBound && !hasPackagesToScan) {
			throw new IllegalArgumentException(
					"Setting either 'contextPath', 'classesToBeBound', " + "or 'packagesToScan' is required");
		}
		if (!this.lazyInit) {
			getJaxbContext();
		}
		if (!ObjectUtils.isEmpty(this.schemaResources)) {
			this.schema = loadSchema(this.schemaResources, this.schemaLanguage);
		}
	}

	/**
	 * Return the JAXBContext used by this marshaller, lazily building it if necessary.
	 */
	public JAXBContext getJaxbContext() {
		JAXBContext context = this.jaxbContext;
		if (context != null) {
			return context;
		}
		synchronized (this.jaxbContextMonitor) {
			context = this.jaxbContext;
			if (context == null) {
				try {
					if (StringUtils.hasLength(this.contextPath)) {
						context = createJaxbContextFromContextPath(this.contextPath);
					}
					else if (!ObjectUtils.isEmpty(this.classesToBeBound)) {
						context = createJaxbContextFromClasses(this.classesToBeBound);
					}
					else if (!ObjectUtils.isEmpty(this.packagesToScan)) {
						context = createJaxbContextFromPackages(this.packagesToScan);
					}
					else {
						context = JAXBContext.newInstance();
					}
					this.jaxbContext = context;
				}
				catch (JAXBException ex) {
					throw convertJaxbException(ex);
				}
			}
			return context;
		}
	}

	private JAXBContext createJaxbContextFromContextPath(String contextPath) throws JAXBException {
		if (logger.isInfoEnabled()) {
			logger.info("Creating JAXBContext with context path [" + this.contextPath + "]");
		}
		if (this.jaxbContextProperties != null) {
			if (this.beanClassLoader != null) {
				return JAXBContext.newInstance(contextPath, this.beanClassLoader, this.jaxbContextProperties);
			}
			else {
				// analogous to the JAXBContext.newInstance(String) implementation
				return JAXBContext.newInstance(contextPath, Thread.currentThread().getContextClassLoader(),
						this.jaxbContextProperties);
			}
		}
		else {
			if (this.beanClassLoader != null) {
				return JAXBContext.newInstance(contextPath, this.beanClassLoader);
			}
			else {
				return JAXBContext.newInstance(contextPath);
			}
		}
	}

	private JAXBContext createJaxbContextFromClasses(Class<?>... classesToBeBound) throws JAXBException {
		if (logger.isInfoEnabled()) {
			logger.info("Creating JAXBContext with classes to be bound [" +
					StringUtils.arrayToCommaDelimitedString(classesToBeBound) + "]");
		}
		if (this.jaxbContextProperties != null) {
			return JAXBContext.newInstance(classesToBeBound, this.jaxbContextProperties);
		}
		else {
			return JAXBContext.newInstance(classesToBeBound);
		}
	}

	private JAXBContext createJaxbContextFromPackages(String... packagesToScan) throws JAXBException {
		if (logger.isInfoEnabled()) {
			logger.info("Creating JAXBContext by scanning packages [" +
					StringUtils.arrayToCommaDelimitedString(packagesToScan) + "]");
		}
		ClassPathJaxb2TypeScanner scanner = new ClassPathJaxb2TypeScanner(this.beanClassLoader, packagesToScan);
		Class<?>[] jaxb2Classes = scanner.scanPackages();
		if (logger.isDebugEnabled()) {
			logger.debug("Found JAXB2 classes: [" + StringUtils.arrayToCommaDelimitedString(jaxb2Classes) + "]");
		}
		this.classesToBeBound = jaxb2Classes;
		if (this.jaxbContextProperties != null) {
			return JAXBContext.newInstance(jaxb2Classes, this.jaxbContextProperties);
		}
		else {
			return JAXBContext.newInstance(jaxb2Classes);
		}
	}

	@SuppressWarnings("deprecation")  // on JDK 9
	private Schema loadSchema(Resource[] resources, String schemaLanguage) throws IOException, SAXException {
		if (logger.isDebugEnabled()) {
			logger.debug("Setting validation schema to " +
					StringUtils.arrayToCommaDelimitedString(this.schemaResources));
		}
		Assert.notEmpty(resources, "No resources given");
		Assert.hasLength(schemaLanguage, "No schema language provided");
		Source[] schemaSources = new Source[resources.length];
		XMLReader xmlReader = org.xml.sax.helpers.XMLReaderFactory.createXMLReader();
		xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
		for (int i = 0; i < resources.length; i++) {
			Assert.notNull(resources[i], "Resource is null");
			Assert.isTrue(resources[i].exists(), "Resource " + resources[i] + " does not exist");
			InputSource inputSource = SaxResourceUtils.createInputSource(resources[i]);
			schemaSources[i] = new SAXSource(xmlReader, inputSource);
		}
		SchemaFactory schemaFactory = SchemaFactory.newInstance(schemaLanguage);
		if (this.schemaResourceResolver != null) {
			schemaFactory.setResourceResolver(this.schemaResourceResolver);
		}
		return schemaFactory.newSchema(schemaSources);
	}


	@Override
	public boolean supports(Class<?> clazz) {
		return ((this.supportJaxbElementClass && JAXBElement.class.isAssignableFrom(clazz)) ||
				supportsInternal(clazz, this.checkForXmlRootElement));
	}

	@Override
	public boolean supports(Type genericType) {
		if (genericType instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) genericType;
			if (JAXBElement.class == parameterizedType.getRawType() &&
					parameterizedType.getActualTypeArguments().length == 1) {
				Type typeArgument = parameterizedType.getActualTypeArguments()[0];
				if (typeArgument instanceof Class) {
					Class<?> classArgument = (Class<?>) typeArgument;
					return (((classArgument.isArray() && Byte.TYPE == classArgument.getComponentType())) ||
							isPrimitiveWrapper(classArgument) || isStandardClass(classArgument) ||
							supportsInternal(classArgument, false));
				}
				else if (typeArgument instanceof GenericArrayType) {
					GenericArrayType arrayType = (GenericArrayType) typeArgument;
					return (Byte.TYPE == arrayType.getGenericComponentType());
				}
			}
		}
		else if (genericType instanceof Class) {
			Class<?> clazz = (Class<?>) genericType;
			return supportsInternal(clazz, this.checkForXmlRootElement);
		}
		return false;
	}

	private boolean supportsInternal(Class<?> clazz, boolean checkForXmlRootElement) {
		if (checkForXmlRootElement && AnnotationUtils.findAnnotation(clazz, XmlRootElement.class) == null) {
			return false;
		}
		if (StringUtils.hasLength(this.contextPath)) {
			String packageName = ClassUtils.getPackageName(clazz);
			String[] contextPaths = StringUtils.tokenizeToStringArray(this.contextPath, ":");
			for (String contextPath : contextPaths) {
				if (contextPath.equals(packageName)) {
					return true;
				}
			}
			return false;
		}
		else if (!ObjectUtils.isEmpty(this.classesToBeBound)) {
			return Arrays.asList(this.classesToBeBound).contains(clazz);
		}
		return false;
	}

	/**
	 * Checks whether the given type is a primitive wrapper type.
	 * Compare section 8.5.1 of the JAXB2 spec.
	 */
	private boolean isPrimitiveWrapper(Class<?> clazz) {
		return (Boolean.class == clazz ||
				Byte.class == clazz ||
				Short.class == clazz ||
				Integer.class == clazz ||
				Long.class == clazz ||
				Float.class == clazz ||
				Double.class == clazz);
	}

	/**
	 * Checks whether the given type is a standard class.
	 * Compare section 8.5.2 of the JAXB2 spec.
	 */
	private boolean isStandardClass(Class<?> clazz) {
		return (String.class == clazz ||
				BigInteger.class.isAssignableFrom(clazz) ||
				BigDecimal.class.isAssignableFrom(clazz) ||
				Calendar.class.isAssignableFrom(clazz) ||
				Date.class.isAssignableFrom(clazz) ||
				QName.class.isAssignableFrom(clazz) ||
				URI.class == clazz ||
				XMLGregorianCalendar.class.isAssignableFrom(clazz) ||
				Duration.class.isAssignableFrom(clazz) ||
				Image.class == clazz ||
				DataHandler.class == clazz ||
				// Source and subclasses should be supported according to the JAXB2 spec, but aren't in the RI
				// Source.class.isAssignableFrom(clazz) ||
				UUID.class == clazz);

	}


	// Marshalling

	@Override
	public void marshal(Object graph, Result result) throws XmlMappingException {
		marshal(graph, result, null);
	}

	@Override
	public void marshal(Object graph, Result result, @Nullable MimeContainer mimeContainer) throws XmlMappingException {
		try {
			Marshaller marshaller = createMarshaller();
			if (this.mtomEnabled && mimeContainer != null) {
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

	/**
	 * Return a newly created JAXB marshaller. JAXB marshallers are not necessarily thread safe.
	 */
	protected Marshaller createMarshaller() {
		try {
			Marshaller marshaller = getJaxbContext().createMarshaller();
			initJaxbMarshaller(marshaller);
			return marshaller;
		}
		catch (JAXBException ex) {
			throw convertJaxbException(ex);
		}
	}

	/**
	 * Template method that can be overridden by concrete JAXB marshallers for custom initialization behavior.
	 * Gets called after creation of JAXB {@code Marshaller}, and after the respective properties have been set.
	 * <p>The default implementation sets the {@link #setMarshallerProperties(Map) defined properties}, the {@link
	 * #setValidationEventHandler(ValidationEventHandler) validation event handler}, the {@link #setSchemas(Resource[])
	 * schemas}, {@link #setMarshallerListener(javax.xml.bind.Marshaller.Listener) listener}, and
	 * {@link #setAdapters(XmlAdapter[]) adapters}.
	 */
	protected void initJaxbMarshaller(Marshaller marshaller) throws JAXBException {
		if (this.marshallerProperties != null) {
			for (String name : this.marshallerProperties.keySet()) {
				marshaller.setProperty(name, this.marshallerProperties.get(name));
			}
		}
		if (this.marshallerListener != null) {
			marshaller.setListener(this.marshallerListener);
		}
		if (this.validationEventHandler != null) {
			marshaller.setEventHandler(this.validationEventHandler);
		}
		if (this.adapters != null) {
			for (XmlAdapter<?, ?> adapter : this.adapters) {
				marshaller.setAdapter(adapter);
			}
		}
		if (this.schema != null) {
			marshaller.setSchema(this.schema);
		}
	}


	// Unmarshalling

	@Override
	public Object unmarshal(Source source) throws XmlMappingException {
		return unmarshal(source, null);
	}

	@Override
	public Object unmarshal(Source source, @Nullable MimeContainer mimeContainer) throws XmlMappingException {
		source = processSource(source);

		try {
			Unmarshaller unmarshaller = createUnmarshaller();
			if (this.mtomEnabled && mimeContainer != null) {
				unmarshaller.setAttachmentUnmarshaller(new Jaxb2AttachmentUnmarshaller(mimeContainer));
			}
			if (StaxUtils.isStaxSource(source)) {
				return unmarshalStaxSource(unmarshaller, source);
			}
			else if (this.mappedClass != null) {
				return unmarshaller.unmarshal(source, this.mappedClass).getValue();
			}
			else {
				return unmarshaller.unmarshal(source);
			}
		}
		catch (NullPointerException ex) {
			if (!isSupportDtd()) {
				throw new UnmarshallingFailureException("NPE while unmarshalling: " +
						"This can happen due to the presence of DTD declarations which are disabled.", ex);
			}
			throw ex;
		}
		catch (JAXBException ex) {
			throw convertJaxbException(ex);
		}
	}

	protected Object unmarshalStaxSource(Unmarshaller jaxbUnmarshaller, Source staxSource) throws JAXBException {
		XMLStreamReader streamReader = StaxUtils.getXMLStreamReader(staxSource);
		if (streamReader != null) {
			return (this.mappedClass != null ?
					jaxbUnmarshaller.unmarshal(streamReader, this.mappedClass).getValue() :
					jaxbUnmarshaller.unmarshal(streamReader));
		}
		else {
			XMLEventReader eventReader = StaxUtils.getXMLEventReader(staxSource);
			if (eventReader != null) {
				return (this.mappedClass != null ?
						jaxbUnmarshaller.unmarshal(eventReader, this.mappedClass).getValue() :
						jaxbUnmarshaller.unmarshal(eventReader));
			}
			else {
				throw new IllegalArgumentException("StaxSource contains neither XMLStreamReader nor XMLEventReader");
			}
		}
	}

	@SuppressWarnings("deprecation")  // on JDK 9
	private Source processSource(Source source) {
		if (StaxUtils.isStaxSource(source) || source instanceof DOMSource) {
			return source;
		}

		XMLReader xmlReader = null;
		InputSource inputSource = null;

		if (source instanceof SAXSource) {
			SAXSource saxSource = (SAXSource) source;
			xmlReader = saxSource.getXMLReader();
			inputSource = saxSource.getInputSource();
		}
		else if (source instanceof StreamSource) {
			StreamSource streamSource = (StreamSource) source;
			if (streamSource.getInputStream() != null) {
				inputSource = new InputSource(streamSource.getInputStream());
			}
			else if (streamSource.getReader() != null) {
				inputSource = new InputSource(streamSource.getReader());
			}
			else {
				inputSource = new InputSource(streamSource.getSystemId());
			}
		}

		try {
			if (xmlReader == null) {
				xmlReader = org.xml.sax.helpers.XMLReaderFactory.createXMLReader();
			}
			xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", !isSupportDtd());
			String name = "http://xml.org/sax/features/external-general-entities";
			xmlReader.setFeature(name, isProcessExternalEntities());
			if (!isProcessExternalEntities()) {
				xmlReader.setEntityResolver(NO_OP_ENTITY_RESOLVER);
			}
			return new SAXSource(xmlReader, inputSource);
		}
		catch (SAXException ex) {
			logger.warn("Processing of external entities could not be disabled", ex);
			return source;
		}
	}

	/**
	 * Return a newly created JAXB unmarshaller.
	 * Note: JAXB unmarshallers are not necessarily thread-safe.
	 */
	protected Unmarshaller createUnmarshaller() {
		try {
			Unmarshaller unmarshaller = getJaxbContext().createUnmarshaller();
			initJaxbUnmarshaller(unmarshaller);
			return unmarshaller;
		}
		catch (JAXBException ex) {
			throw convertJaxbException(ex);
		}
	}

	/**
	 * Template method that can be overridden by concrete JAXB marshallers for custom initialization behavior.
	 * Gets called after creation of JAXB {@code Marshaller}, and after the respective properties have been set.
	 * <p>The default implementation sets the {@link #setUnmarshallerProperties(Map) defined properties}, the {@link
	 * #setValidationEventHandler(ValidationEventHandler) validation event handler}, the {@link #setSchemas(Resource[])
	 * schemas}, {@link #setUnmarshallerListener(javax.xml.bind.Unmarshaller.Listener) listener}, and
	 * {@link #setAdapters(XmlAdapter[]) adapters}.
	 */
	protected void initJaxbUnmarshaller(Unmarshaller unmarshaller) throws JAXBException {
		if (this.unmarshallerProperties != null) {
			for (String name : this.unmarshallerProperties.keySet()) {
				unmarshaller.setProperty(name, this.unmarshallerProperties.get(name));
			}
		}
		if (this.unmarshallerListener != null) {
			unmarshaller.setListener(this.unmarshallerListener);
		}
		if (this.validationEventHandler != null) {
			unmarshaller.setEventHandler(this.validationEventHandler);
		}
		if (this.adapters != null) {
			for (XmlAdapter<?, ?> adapter : this.adapters) {
				unmarshaller.setAdapter(adapter);
			}
		}
		if (this.schema != null) {
			unmarshaller.setSchema(this.schema);
		}
	}

	/**
	 * Convert the given {@code JAXBException} to an appropriate exception from the
	 * {@code org.springframework.oxm} hierarchy.
	 * @param ex {@code JAXBException} that occurred
	 * @return the corresponding {@code XmlMappingException}
	 */
	protected XmlMappingException convertJaxbException(JAXBException ex) {
		if (ex instanceof ValidationException) {
			return new ValidationFailureException("JAXB validation exception", ex);
		}
		else if (ex instanceof MarshalException) {
			return new MarshallingFailureException("JAXB marshalling exception", ex);
		}
		else if (ex instanceof UnmarshalException) {
			return new UnmarshallingFailureException("JAXB unmarshalling exception", ex);
		}
		else {
			// fallback
			return new UncategorizedMappingException("Unknown JAXB exception", ex);
		}
	}


	private static class Jaxb2AttachmentMarshaller extends AttachmentMarshaller {

		private final MimeContainer mimeContainer;

		public Jaxb2AttachmentMarshaller(MimeContainer mimeContainer) {
			this.mimeContainer = mimeContainer;
		}

		@Override
		public String addMtomAttachment(byte[] data, int offset, int length, String mimeType,
				String elementNamespace, String elementLocalName) {
			ByteArrayDataSource dataSource = new ByteArrayDataSource(mimeType, data, offset, length);
			return addMtomAttachment(new DataHandler(dataSource), elementNamespace, elementLocalName);
		}

		@Override
		public String addMtomAttachment(DataHandler dataHandler, String elementNamespace, String elementLocalName) {
			String host = getHost(elementNamespace, dataHandler);
			String contentId = UUID.randomUUID() + "@" + host;
			this.mimeContainer.addAttachment("<" + contentId + ">", dataHandler);
			try {
				contentId = URLEncoder.encode(contentId, "UTF-8");
			}
			catch (UnsupportedEncodingException ex) {
				// ignore
			}
			return CID + contentId;
		}

		private String getHost(String elementNamespace, DataHandler dataHandler) {
			try {
				URI uri = new URI(elementNamespace);
				return uri.getHost();
			}
			catch (URISyntaxException ex) {
				// ignore
			}
			return dataHandler.getName();
		}

		@Override
		public String addSwaRefAttachment(DataHandler dataHandler) {
			String contentId = UUID.randomUUID() + "@" + dataHandler.getName();
			this.mimeContainer.addAttachment(contentId, dataHandler);
			return contentId;
		}

		@Override
		public boolean isXOPPackage() {
			return this.mimeContainer.convertToXopPackage();
		}
	}


	private static class Jaxb2AttachmentUnmarshaller extends AttachmentUnmarshaller {

		private final MimeContainer mimeContainer;

		public Jaxb2AttachmentUnmarshaller(MimeContainer mimeContainer) {
			this.mimeContainer = mimeContainer;
		}

		@Override
		public byte[] getAttachmentAsByteArray(String cid) {
			try {
				DataHandler dataHandler = getAttachmentAsDataHandler(cid);
				return FileCopyUtils.copyToByteArray(dataHandler.getInputStream());
			}
			catch (IOException ex) {
				throw new UnmarshallingFailureException("Couldn't read attachment", ex);
			}
		}

		@Override
		public DataHandler getAttachmentAsDataHandler(String contentId) {
			if (contentId.startsWith(CID)) {
				contentId = contentId.substring(CID.length());
				try {
					contentId = URLDecoder.decode(contentId, "UTF-8");
				}
				catch (UnsupportedEncodingException ex) {
					// ignore
				}
				contentId = '<' + contentId + '>';
			}
			DataHandler dataHandler = this.mimeContainer.getAttachment(contentId);
			if (dataHandler == null) {
				throw new IllegalArgumentException("No attachment found for " + contentId);
			}
			return dataHandler;
		}

		@Override
		public boolean isXOPPackage() {
			return this.mimeContainer.isXopPackage();
		}
	}


	/**
	 * DataSource that wraps around a byte array.
	 */
	private static class ByteArrayDataSource implements DataSource {

		private final byte[] data;

		private final String contentType;

		private final int offset;

		private final int length;

		public ByteArrayDataSource(String contentType, byte[] data, int offset, int length) {
			this.contentType = contentType;
			this.data = data;
			this.offset = offset;
			this.length = length;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(this.data, this.offset, this.length);
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getContentType() {
			return this.contentType;
		}

		@Override
		public String getName() {
			return "ByteArrayDataSource";
		}
	}


	private static final EntityResolver NO_OP_ENTITY_RESOLVER = new EntityResolver() {
		@Override
		public InputSource resolveEntity(String publicId, String systemId) {
			return new InputSource(new StringReader(""));
		}
	};

}
