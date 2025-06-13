/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.oxm.jaxb;

import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.XMLConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
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

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.MarshalException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.ValidationEventHandler;
import jakarta.xml.bind.ValidationException;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.attachment.AttachmentMarshaller;
import jakarta.xml.bind.attachment.AttachmentUnmarshaller;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.Resource;
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
import org.springframework.util.ResourceUtils;
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
 * @author Sam Brannen
 * @since 3.0
 * @see #setContextPath
 * @see #setClassesToBeBound
 * @see #setJaxbContextProperties
 * @see #setMarshallerProperties
 * @see #setUnmarshallerProperties
 * @see #setSchema
 * @see #setSchemas
 * @see #setMarshallerListener
 * @see #setUnmarshallerListener
 * @see #setAdapters
 */
public class Jaxb2Marshaller implements MimeMarshaller, MimeUnmarshaller, GenericMarshaller, GenericUnmarshaller,
		BeanClassLoaderAware, InitializingBean {

	private static final String CID = "cid:";

	private static final EntityResolver NO_OP_ENTITY_RESOLVER =
			(publicId, systemId) -> new InputSource(new StringReader(""));


	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	private @Nullable String contextPath;

	private Class<?> @Nullable [] classesToBeBound;

	private String @Nullable [] packagesToScan;

	private @Nullable Map<String, ?> jaxbContextProperties;

	private @Nullable Map<String, ?> marshallerProperties;

	private @Nullable Map<String, ?> unmarshallerProperties;

	private Marshaller.@Nullable Listener marshallerListener;

	private Unmarshaller.@Nullable Listener unmarshallerListener;

	private @Nullable ValidationEventHandler validationEventHandler;

	private XmlAdapter<?, ?> @Nullable [] adapters;

	private Resource @Nullable [] schemaResources;

	private String schemaLanguage = XMLConstants.W3C_XML_SCHEMA_NS_URI;

	private @Nullable LSResourceResolver schemaResourceResolver;

	private boolean lazyInit = false;

	private boolean mtomEnabled = false;

	private boolean supportJaxbElementClass = false;

	private boolean checkForXmlRootElement = true;

	private @Nullable Class<?> mappedClass;

	private @Nullable ClassLoader beanClassLoader;

	private final Lock jaxbContextLock = new ReentrantLock();

	private volatile @Nullable JAXBContext jaxbContext;

	private @Nullable Schema schema;

	private boolean supportDtd = false;

	private boolean processExternalEntities = false;

	private volatile @Nullable SAXParserFactory schemaParserFactory;

	private volatile @Nullable SAXParserFactory sourceParserFactory;


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
	public @Nullable String getContextPath() {
		return this.contextPath;
	}

	/**
	 * Set the list of Java classes to be recognized by a newly created JAXBContext.
	 * <p>Setting either this property, {@link #setContextPath "contextPath"}
	 * or {@link #setPackagesToScan "packagesToScan"} is required.
	 */
	public void setClassesToBeBound(Class<?> @Nullable ... classesToBeBound) {
		this.classesToBeBound = classesToBeBound;
	}

	/**
	 * Return the list of Java classes to be recognized by a newly created JAXBContext.
	 */
	public Class<?> @Nullable [] getClassesToBeBound() {
		return this.classesToBeBound;
	}

	/**
	 * Set the packages to search for classes with JAXB2 annotations in the classpath.
	 * This is using a Spring-bases search and therefore analogous to Spring's component-scan
	 * feature ({@link org.springframework.context.annotation.ClassPathBeanDefinitionScanner}).
	 * <p>Setting either this property, {@link #setContextPath "contextPath"} or
	 * {@link #setClassesToBeBound "classesToBeBound"} is required.
	 */
	public void setPackagesToScan(String @Nullable ... packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	/**
	 * Return the packages to search for JAXB2 annotations.
	 */
	public String @Nullable [] getPackagesToScan() {
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
	 * Set the JAXB {@code Marshaller} properties.
	 * <p>These properties will be set on the underlying JAXB {@code Marshaller},
	 * and allow for features such as indentation.
	 * @param properties the properties
	 * @see jakarta.xml.bind.Marshaller#setProperty(String, Object)
	 * @see jakarta.xml.bind.Marshaller#JAXB_ENCODING
	 * @see jakarta.xml.bind.Marshaller#JAXB_FORMATTED_OUTPUT
	 * @see jakarta.xml.bind.Marshaller#JAXB_NO_NAMESPACE_SCHEMA_LOCATION
	 * @see jakarta.xml.bind.Marshaller#JAXB_SCHEMA_LOCATION
	 */
	public void setMarshallerProperties(Map<String, ?> properties) {
		this.marshallerProperties = properties;
	}

	/**
	 * Set the JAXB {@code Unmarshaller} properties.
	 * <p>These properties will be set on the underlying JAXB {@code Unmarshaller}.
	 * @param properties the properties
	 * @see jakarta.xml.bind.Unmarshaller#setProperty(String, Object)
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
	 * and {@code Unmarshaller}.
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
	 * Set the schema language.
	 * Default is the W3C XML Schema: {@code http://www.w3.org/2001/XMLSchema"}.
	 * @see XMLConstants#W3C_XML_SCHEMA_NS_URI
	 * @see XMLConstants#RELAXNG_NS_URI
	 */
	public void setSchemaLanguage(String schemaLanguage) {
		this.schemaLanguage = schemaLanguage;
	}

	/**
	 * Set the resource resolver, as used to load the schema resources.
	 * @see SchemaFactory#setResourceResolver(org.w3c.dom.ls.LSResourceResolver)
	 * @see #setSchema
	 * @see #setSchemas
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
	 * Specify whether the {@link #supports(Class)} returns {@code true} for the
	 * {@link JAXBElement} class.
	 * <p>Default is {@code false}, meaning that {@code supports(Class)} always returns
	 * {@code false} for {@code JAXBElement} classes (though {@link #supports(Type)} can
	 * return {@code true}, since it can obtain the type parameters of {@code JAXBElement}).
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
	 * @see jakarta.xml.bind.Unmarshaller#unmarshal(javax.xml.transform.Source, Class)
	 */
	public void setMappedClass(Class<?> mappedClass) {
		this.mappedClass = mappedClass;
	}

	/**
	 * Indicate whether DTD parsing should be supported.
	 * <p>Default is {@code false} meaning that DTD is disabled.
	 */
	public void setSupportDtd(boolean supportDtd) {
		this.supportDtd = supportDtd;
		this.sourceParserFactory = null;
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
	 * <p><strong>Note:</strong> setting this option to {@code true} also automatically
	 * sets {@link #setSupportDtd} to {@code true}.
	 */
	public void setProcessExternalEntities(boolean processExternalEntities) {
		this.processExternalEntities = processExternalEntities;
		if (processExternalEntities) {
			this.supportDtd = true;
		}
		this.sourceParserFactory = null;
	}

	/**
	 * Return whether XML external entities are allowed.
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

		this.jaxbContextLock.lock();
		try {
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
		finally {
			this.jaxbContextLock.unlock();
		}
	}

	private JAXBContext createJaxbContextFromContextPath(String contextPath) throws JAXBException {
		if (logger.isDebugEnabled()) {
			logger.debug("Creating JAXBContext with context path [" + this.contextPath + "]");
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
		if (logger.isDebugEnabled()) {
			logger.debug("Creating JAXBContext with classes to be bound [" +
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
		if (logger.isDebugEnabled()) {
			logger.debug("Creating JAXBContext by scanning packages [" +
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

	private Schema loadSchema(Resource[] resources, String schemaLanguage) throws IOException, SAXException, ParserConfigurationException {
		if (logger.isDebugEnabled()) {
			logger.debug("Setting validation schema to " +
					StringUtils.arrayToCommaDelimitedString(this.schemaResources));
		}
		Assert.notEmpty(resources, "No resources given");
		Assert.hasLength(schemaLanguage, "No schema language provided");
		Source[] schemaSources = new Source[resources.length];

		// This parser is used to read the schema resources provided by the application.
		// The parser used for reading the source is protected against XXE attacks.
		// See "processSource(Source source)".
		SAXParserFactory saxParserFactory = this.schemaParserFactory;
		if (saxParserFactory == null) {
			saxParserFactory = SAXParserFactory.newInstance();
			saxParserFactory.setNamespaceAware(true);
			saxParserFactory.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
			this.schemaParserFactory = saxParserFactory;
		}
		SAXParser saxParser = saxParserFactory.newSAXParser();
		XMLReader xmlReader = saxParser.getXMLReader();

		for (int i = 0; i < resources.length; i++) {
			Resource resource = resources[i];
			Assert.isTrue(resource != null && resource.exists(), () -> "Resource does not exist: " + resource);
			InputSource inputSource = SaxResourceUtils.createInputSource(resource);
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
		return (this.supportJaxbElementClass && JAXBElement.class.isAssignableFrom(clazz)) ||
				supportsInternal(clazz, this.checkForXmlRootElement);
	}

	@Override
	public boolean supports(Type genericType) {
		if (genericType instanceof ParameterizedType parameterizedType) {
			if (JAXBElement.class == parameterizedType.getRawType() &&
					parameterizedType.getActualTypeArguments().length == 1) {
				Type typeArgument = parameterizedType.getActualTypeArguments()[0];
				if (typeArgument instanceof Class<?> classArgument) {
					return ((byte.class == classArgument.componentType()) ||
							isPrimitiveWrapper(classArgument) || isStandardClass(classArgument) ||
							supportsInternal(classArgument, false));
				}
				else if (typeArgument instanceof GenericArrayType arrayType) {
					return (byte.class == arrayType.getGenericComponentType());
				}
			}
		}
		else if (genericType instanceof Class<?> clazz) {
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

	/**
	 * Return a newly created JAXB marshaller.
	 * <p>Note: JAXB marshallers are not necessarily thread-safe.
	 * This method is public as of 5.2.
	 * @since 5.2
	 * @see #createUnmarshaller()
	 */
	public Marshaller createMarshaller() {
		try {
			Marshaller marshaller = getJaxbContext().createMarshaller();
			initJaxbMarshaller(marshaller);
			return marshaller;
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
	 * Template method that can be overridden by concrete JAXB marshallers
	 * for custom initialization behavior. Gets called after creation of JAXB
	 * {@code Marshaller}, and after the respective properties have been set.
	 * <p>The default implementation sets the
	 * {@link #setMarshallerProperties defined properties}, the
	 * {@link #setValidationEventHandler validation event handler}, the
	 * {@link #setSchemas schemas}, {@link #setMarshallerListener listener},
	 * and {@link #setAdapters adapters}.
	 */
	protected void initJaxbMarshaller(Marshaller marshaller) throws JAXBException {
		if (this.marshallerProperties != null) {
			for (Map.Entry<String, ?> entry : this.marshallerProperties.entrySet()) {
				marshaller.setProperty(entry.getKey(), entry.getValue());
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

	/**
	 * Return a newly created JAXB unmarshaller.
	 * <p>Note: JAXB unmarshallers are not necessarily thread-safe.
	 * This method is public as of 5.2.
	 * @since 5.2
	 * @see #createMarshaller()
	 */
	public Unmarshaller createUnmarshaller() {
		try {
			Unmarshaller unmarshaller = getJaxbContext().createUnmarshaller();
			initJaxbUnmarshaller(unmarshaller);
			return unmarshaller;
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

	private Source processSource(Source source) {
		if (StaxUtils.isStaxSource(source) || source instanceof DOMSource) {
			return source;
		}

		XMLReader xmlReader = null;
		InputSource inputSource = null;

		if (source instanceof SAXSource saxSource) {
			xmlReader = saxSource.getXMLReader();
			inputSource = saxSource.getInputSource();
		}
		else if (source instanceof StreamSource streamSource) {
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
			// By default, Spring will prevent the processing of external entities.
			// This is a mitigation against XXE attacks.
			if (xmlReader == null) {
				SAXParserFactory saxParserFactory = this.sourceParserFactory;
				if (saxParserFactory == null) {
					saxParserFactory = SAXParserFactory.newInstance();
					saxParserFactory.setNamespaceAware(true);
					saxParserFactory.setFeature(
							"http://apache.org/xml/features/disallow-doctype-decl", !isSupportDtd());
					saxParserFactory.setFeature(
							"http://xml.org/sax/features/external-general-entities", isProcessExternalEntities());
					this.sourceParserFactory = saxParserFactory;
				}
				SAXParser saxParser = saxParserFactory.newSAXParser();
				xmlReader = saxParser.getXMLReader();
			}
			if (!isProcessExternalEntities()) {
				xmlReader.setEntityResolver(NO_OP_ENTITY_RESOLVER);
			}
			return new SAXSource(xmlReader, inputSource);
		}
		catch (SAXException | ParserConfigurationException ex) {
			logger.info("Processing of external entities could not be disabled", ex);
			return source;
		}
	}

	/**
	 * Template method that can be overridden by concrete JAXB marshallers
	 * for custom initialization behavior. Gets called after creation of JAXB
	 * {@code Marshaller}, and after the respective properties have been set.
	 * <p>The default implementation sets the
	 * {@link #setUnmarshallerProperties defined properties}, the
	 * {@link #setValidationEventHandler validation event handler}, the
	 * {@link #setSchemas schemas}, {@link #setUnmarshallerListener listener},
	 * and {@link #setAdapters adapters}.
	 */
	protected void initJaxbUnmarshaller(Unmarshaller unmarshaller) throws JAXBException {
		if (this.unmarshallerProperties != null) {
			for (Map.Entry<String, ?> entry : this.unmarshallerProperties.entrySet()) {
				unmarshaller.setProperty(entry.getKey(), entry.getValue());
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
	 * Convert the given {@code JAXBException} to an appropriate exception
	 * from the {@code org.springframework.oxm} hierarchy.
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
			contentId = URLEncoder.encode(contentId, StandardCharsets.UTF_8);
			return CID + contentId;
		}

		private String getHost(String elementNamespace, DataHandler dataHandler) {
			try {
				URI uri = ResourceUtils.toURI(elementNamespace);
				return uri.getHost();
			}
			catch (URISyntaxException ignored) {
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
				throw new UnmarshallingFailureException("Could not read attachment", ex);
			}
		}

		@Override
		public DataHandler getAttachmentAsDataHandler(String contentId) {
			if (contentId.startsWith(CID)) {
				contentId = contentId.substring(CID.length());
				contentId = URLDecoder.decode(contentId, StandardCharsets.UTF_8);
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
		public InputStream getInputStream() {
			return new ByteArrayInputStream(this.data, this.offset, this.length);
		}

		@Override
		public OutputStream getOutputStream() {
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

}
