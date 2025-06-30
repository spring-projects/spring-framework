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

package org.springframework.oxm.xstream;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;

import com.thoughtworks.xstream.MarshallingStrategy;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.ConverterMatcher;
import com.thoughtworks.xstream.converters.ConverterRegistry;
import com.thoughtworks.xstream.converters.DataHolder;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.core.ClassLoaderReference;
import com.thoughtworks.xstream.core.DefaultConverterLookup;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.naming.NameCoder;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.io.xml.DomReader;
import com.thoughtworks.xstream.io.xml.DomWriter;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.SaxWriter;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.io.xml.StaxReader;
import com.thoughtworks.xstream.io.xml.StaxWriter;
import com.thoughtworks.xstream.io.xml.XmlFriendlyNameCoder;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;
import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import com.thoughtworks.xstream.security.ForbiddenClassException;
import com.thoughtworks.xstream.security.TypePermission;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.oxm.MarshallingFailureException;
import org.springframework.oxm.UncategorizedMappingException;
import org.springframework.oxm.UnmarshallingFailureException;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.support.AbstractMarshaller;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.util.xml.StaxUtils;

/**
 * Implementation of the {@code Marshaller} interface for XStream.
 *
 * <p>By default, XStream does not require any further configuration and can (un)marshal
 * any class on the classpath. As such, it is <b>not recommended to use the
 * {@code XStreamMarshaller} to unmarshal XML from external sources</b> (i.e. the Web),
 * as this can result in <b>security vulnerabilities</b>. If you do use the
 * {@code XStreamMarshaller} to unmarshal external XML, set the
 * {@link #setSupportedClasses(Class[]) supportedClasses} and
 * {@link #setConverters(ConverterMatcher[]) converters} properties (possibly using
 * a {@link CatchAllConverter}) or override the {@link #customizeXStream(XStream)}
 * method to make sure it only accepts the classes you want it to support.
 *
 * <p>Due to XStream's API, it is required to set the encoding used for writing to
 * OutputStreams. It defaults to {@code UTF-8}.
 *
 * <p><b>NOTE:</b> XStream is an XML serialization library, not a data binding library.
 * Therefore, it has limited namespace support. As such, it is rather unsuitable for
 * usage within Web Services.
 *
 * <p>This marshaller requires XStream 1.4.7 or higher.
 *
 * <p>As of Spring Framework 6.0, the default {@link HierarchicalStreamDriver} is
 * a {@link DomDriver} that uses the configured {@linkplain #setEncoding(String)
 * encoding} and {@link #setNameCoder(NameCoder) NameCoder}. The driver can be
 * changed via {@link #setStreamDriver(HierarchicalStreamDriver)}.
 *
 * @author Peter Meijer
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
public class XStreamMarshaller extends AbstractMarshaller implements BeanClassLoaderAware, InitializingBean {

	/**
	 * The default encoding used for stream access: UTF-8.
	 */
	public static final String DEFAULT_ENCODING = "UTF-8";


	private @Nullable ReflectionProvider reflectionProvider;

	private @Nullable HierarchicalStreamDriver streamDriver;

	private @Nullable HierarchicalStreamDriver defaultDriver;

	private @Nullable Mapper mapper;

	private Class<? extends MapperWrapper> @Nullable [] mapperWrappers;

	private ConverterLookup converterLookup = new DefaultConverterLookup();

	private ConverterRegistry converterRegistry = (ConverterRegistry) this.converterLookup;

	private ConverterMatcher @Nullable [] converters;

	private TypePermission @Nullable [] typePermissions;

	private @Nullable MarshallingStrategy marshallingStrategy;

	private @Nullable Integer mode;

	private @Nullable Map<String, ?> aliases;

	private @Nullable Map<String, ?> aliasesByType;

	private @Nullable Map<String, String> fieldAliases;

	private Class<?> @Nullable [] useAttributeForTypes;

	private @Nullable Map<?, ?> useAttributeFor;

	private @Nullable Map<Class<?>, String> implicitCollections;

	private @Nullable Map<Class<?>, String> omittedFields;

	private Class<?> @Nullable [] annotatedClasses;

	private boolean autodetectAnnotations;

	private String encoding = DEFAULT_ENCODING;

	private NameCoder nameCoder = new XmlFriendlyNameCoder();

	private Class<?> @Nullable [] supportedClasses;

	private @Nullable ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private final SingletonSupplier<XStream> xstream = SingletonSupplier.of(this::buildXStream);


	/**
	 * Set a custom XStream {@link ReflectionProvider} to use.
	 * @since 4.0
	 */
	public void setReflectionProvider(ReflectionProvider reflectionProvider) {
		this.reflectionProvider = reflectionProvider;
	}

	/**
	 * Set an XStream {@link HierarchicalStreamDriver} to be used for readers and writers.
	 * <p>As of Spring 4.0, this stream driver will also be passed to the {@link XStream}
	 * constructor and therefore used by streaming-related native API methods themselves.
	 */
	public void setStreamDriver(HierarchicalStreamDriver streamDriver) {
		this.streamDriver = streamDriver;
		this.defaultDriver = streamDriver;
	}

	private HierarchicalStreamDriver getDefaultDriver() {
		if (this.defaultDriver == null) {
			this.defaultDriver = new DomDriver(this.encoding, this.nameCoder);
		}
		return this.defaultDriver;
	}

	/**
	 * Set a custom XStream {@link Mapper} to use.
	 * @since 4.0
	 */
	public void setMapper(Mapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * Set one or more custom XStream {@link MapperWrapper} classes.
	 * Each of those classes needs to have a constructor with a single argument
	 * of type {@link Mapper} or {@link MapperWrapper}.
	 * @since 4.0
	 */
	@SuppressWarnings("unchecked")
	public void setMapperWrappers(Class<? extends MapperWrapper>... mapperWrappers) {
		this.mapperWrappers = mapperWrappers;
	}

	/**
	 * Set a custom XStream {@link ConverterLookup} to use.
	 * Also used as {@link ConverterRegistry} if the given reference implements it as well.
	 * @since 4.0
	 * @see DefaultConverterLookup
	 */
	public void setConverterLookup(ConverterLookup converterLookup) {
		this.converterLookup = converterLookup;
		if (converterLookup instanceof ConverterRegistry registry) {
			this.converterRegistry = registry;
		}
	}

	/**
	 * Set a custom XStream {@link ConverterRegistry} to use.
	 * @since 4.0
	 * @see #setConverterLookup
	 * @see DefaultConverterLookup
	 */
	public void setConverterRegistry(ConverterRegistry converterRegistry) {
		this.converterRegistry = converterRegistry;
	}

	/**
	 * Set the {@code Converters} or {@code SingleValueConverters} to be registered
	 * with the {@code XStream} instance.
	 * @see Converter
	 * @see SingleValueConverter
	 */
	public void setConverters(ConverterMatcher... converters) {
		this.converters = converters;
	}

	/**
	 * Set XStream type permissions such as
	 * {@link com.thoughtworks.xstream.security.AnyTypePermission},
	 * {@link com.thoughtworks.xstream.security.ExplicitTypePermission} etc,
	 * as an alternative to overriding the {@link #customizeXStream} method.
	 * <p>Note: As of XStream 1.4.18, the default type permissions are
	 * restricted to well-known core JDK types. For any custom types,
	 * explicit type permissions need to be registered.
	 * @since 5.2.17
	 */
	public void setTypePermissions(TypePermission... typePermissions) {
		this.typePermissions = typePermissions;
	}

	/**
	 * Set a custom XStream {@link MarshallingStrategy} to use.
	 * @since 4.0
	 */
	public void setMarshallingStrategy(MarshallingStrategy marshallingStrategy) {
		this.marshallingStrategy = marshallingStrategy;
	}

	/**
	 * Set the XStream mode to use.
	 * @see XStream#ID_REFERENCES
	 * @see XStream#NO_REFERENCES
	 */
	public void setMode(int mode) {
		this.mode = mode;
	}

	/**
	 * Set the alias/type map, consisting of string aliases mapped to classes.
	 * <p>Keys are aliases; values are either {@code Class} instances, or String class names.
	 * @see XStream#alias(String, Class)
	 */
	public void setAliases(Map<String, ?> aliases) {
		this.aliases = aliases;
	}

	/**
	 * Set the <em>aliases by type</em> map, consisting of string aliases mapped to classes.
	 * <p>Any class that is assignable to this type will be aliased to the same name.
	 * Keys are aliases; values are either {@code Class} instances, or String class names.
	 * @see XStream#aliasType(String, Class)
	 */
	public void setAliasesByType(Map<String, ?> aliasesByType) {
		this.aliasesByType = aliasesByType;
	}

	/**
	 * Set the field alias/type map, consisting of field names.
	 * @see XStream#aliasField(String, Class, String)
	 */
	public void setFieldAliases(Map<String, String> fieldAliases) {
		this.fieldAliases = fieldAliases;
	}

	/**
	 * Set types to use XML attributes for.
	 * @see XStream#useAttributeFor(Class)
	 */
	public void setUseAttributeForTypes(Class<?>... useAttributeForTypes) {
		this.useAttributeForTypes = useAttributeForTypes;
	}

	/**
	 * Set the types to use XML attributes for. The given map can contain
	 * either {@code <String, Class>} pairs, in which case
	 * {@link XStream#useAttributeFor(String, Class)} is called.
	 * Alternatively, the map can contain {@code <Class, String>}
	 * or {@code <Class, List<String>>} pairs, which results
	 * in {@link XStream#useAttributeFor(Class, String)} calls.
	 */
	public void setUseAttributeFor(Map<?, ?> useAttributeFor) {
		this.useAttributeFor = useAttributeFor;
	}

	/**
	 * Specify implicit collection fields, as a Map consisting of {@code Class} instances
	 * mapped to comma separated collection field names.
	 * @see XStream#addImplicitCollection(Class, String)
	 */
	public void setImplicitCollections(Map<Class<?>, String> implicitCollections) {
		this.implicitCollections = implicitCollections;
	}

	/**
	 * Specify omitted fields, as a Map consisting of {@code Class} instances
	 * mapped to comma separated field names.
	 * @see XStream#omitField(Class, String)
	 */
	public void setOmittedFields(Map<Class<?>, String> omittedFields) {
		this.omittedFields = omittedFields;
	}

	/**
	 * Set annotated classes for which aliases will be read from class-level annotation metadata.
	 * @see XStream#processAnnotations(Class[])
	 */
	public void setAnnotatedClasses(Class<?>... annotatedClasses) {
		this.annotatedClasses = annotatedClasses;
	}

	/**
	 * Activate XStream's autodetection mode.
	 * <p><b>Note</b>: Autodetection implies that the XStream instance is being configured while
	 * it is processing the XML streams, and thus introduces a potential concurrency problem.
	 * @see XStream#autodetectAnnotations(boolean)
	 */
	public void setAutodetectAnnotations(boolean autodetectAnnotations) {
		this.autodetectAnnotations = autodetectAnnotations;
	}

	/**
	 * Set the encoding to be used for stream access.
	 * @see #DEFAULT_ENCODING
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	@Override
	protected String getDefaultEncoding() {
		return this.encoding;
	}

	/**
	 * Set a custom XStream {@link NameCoder} to use.
	 * The default is an {@link XmlFriendlyNameCoder}.
	 * @since 4.0.4
	 */
	public void setNameCoder(NameCoder nameCoder) {
		this.nameCoder = nameCoder;
	}

	/**
	 * Set the classes supported by this marshaller.
	 * <p>If this property is empty (the default), all classes are supported.
	 * @see #supports(Class)
	 */
	public void setSupportedClasses(Class<?>... supportedClasses) {
		this.supportedClasses = supportedClasses;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}


	@Override
	public void afterPropertiesSet() {
		// no-op due to use of SingletonSupplier for the XStream field
	}

	/**
	 * Build the native XStream delegate to be used by this marshaller,
	 * delegating to {@link #constructXStream}, {@link #configureXStream},
	 * and {@link #customizeXStream}.
	 */
	protected XStream buildXStream() {
		XStream xstream = constructXStream();
		configureXStream(xstream);
		customizeXStream(xstream);
		return xstream;
	}

	/**
	 * Construct an XStream instance, either using one of the
	 * standard constructors or creating a custom subclass.
	 * @return the {@code XStream} instance
	 */
	protected XStream constructXStream() {
		return new XStream(this.reflectionProvider, getDefaultDriver(), new ClassLoaderReference(this.beanClassLoader),
				this.mapper, this.converterLookup, this.converterRegistry) {
			@Override
			protected MapperWrapper wrapMapper(MapperWrapper next) {
				MapperWrapper mapperToWrap = next;
				if (mapperWrappers != null) {
					for (Class<? extends MapperWrapper> mapperWrapper : mapperWrappers) {
						Constructor<? extends MapperWrapper> ctor;
						try {
							ctor = mapperWrapper.getConstructor(Mapper.class);
						}
						catch (NoSuchMethodException ex) {
							try {
								ctor = mapperWrapper.getConstructor(MapperWrapper.class);
							}
							catch (NoSuchMethodException ex2) {
								throw new IllegalStateException("No appropriate MapperWrapper constructor found: " + mapperWrapper);
							}
						}
						try {
							mapperToWrap = ctor.newInstance(mapperToWrap);
						}
						catch (Throwable ex) {
							throw new IllegalStateException("Failed to construct MapperWrapper: " + mapperWrapper);
						}
					}
				}
				return mapperToWrap;
			}
		};
	}

	/**
	 * Configure the XStream instance with this marshaller's bean properties.
	 * @param xstream the {@code XStream} instance
	 */
	protected void configureXStream(XStream xstream) {
		if (this.converters != null) {
			for (int i = 0; i < this.converters.length; i++) {
				if (this.converters[i] instanceof Converter converter) {
					xstream.registerConverter(converter, i);
				}
				else if (this.converters[i] instanceof SingleValueConverter converter) {
					xstream.registerConverter(converter, i);
				}
				else {
					throw new IllegalArgumentException("Invalid ConverterMatcher [" + this.converters[i] + "]");
				}
			}
		}

		if (this.typePermissions != null) {
			for (TypePermission permission : this.typePermissions) {
				xstream.addPermission(permission);
			}
		}

		if (this.marshallingStrategy != null) {
			xstream.setMarshallingStrategy(this.marshallingStrategy);
		}
		if (this.mode != null) {
			xstream.setMode(this.mode);
		}

		try {
			if (this.aliases != null) {
				Map<String, Class<?>> classMap = toClassMap(this.aliases);
				classMap.forEach(xstream::alias);
			}
			if (this.aliasesByType != null) {
				Map<String, Class<?>> classMap = toClassMap(this.aliasesByType);
				classMap.forEach(xstream::aliasType);
			}
			if (this.fieldAliases != null) {
				for (Map.Entry<String, String> entry : this.fieldAliases.entrySet()) {
					String alias = entry.getValue();
					String field = entry.getKey();
					int idx = field.lastIndexOf('.');
					if (idx != -1) {
						String className = field.substring(0, idx);
						Class<?> clazz = ClassUtils.forName(className, this.beanClassLoader);
						String fieldName = field.substring(idx + 1);
						xstream.aliasField(alias, clazz, fieldName);
					}
					else {
						throw new IllegalArgumentException("Field name [" + field + "] does not contain '.'");
					}
				}
			}
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException("Failed to load specified alias class", ex);
		}

		if (this.useAttributeForTypes != null) {
			for (Class<?> type : this.useAttributeForTypes) {
				xstream.useAttributeFor(type);
			}
		}
		if (this.useAttributeFor != null) {
			for (Map.Entry<?, ?> entry : this.useAttributeFor.entrySet()) {
				if (entry.getKey() instanceof String key) {
					if (entry.getValue() instanceof Class<?> clazz) {
						xstream.useAttributeFor(key, clazz);
					}
					else {
						throw new IllegalArgumentException(
								"'useAttributesFor' takes Map<String, Class> when using a map key of type String");
					}
				}
				else if (entry.getKey() instanceof Class<?> key) {
					if (entry.getValue() instanceof String value) {
						xstream.useAttributeFor(key, value);
					}
					else if (entry.getValue() instanceof List<?> listValue) {
						for (Object element : listValue) {
							if (element instanceof String value) {
								xstream.useAttributeFor(key, value);
							}
						}
					}
					else {
						throw new IllegalArgumentException("'useAttributesFor' property takes either Map<Class, String> " +
								"or Map<Class, List<String>> when using a map key of type Class");
					}
				}
				else {
					throw new IllegalArgumentException(
							"'useAttributesFor' property takes either a map key of type String or Class");
				}
			}
		}

		if (this.implicitCollections != null) {
			this.implicitCollections.forEach((key, fields) -> {
				String[] collectionFields = StringUtils.commaDelimitedListToStringArray(fields);
				for (String collectionField : collectionFields) {
					xstream.addImplicitCollection(key, collectionField);
				}
			});
		}
		if (this.omittedFields != null) {
			this.omittedFields.forEach((key, value) -> {
				String[] fields = StringUtils.commaDelimitedListToStringArray(value);
				for (String field : fields) {
					xstream.omitField(key, field);
				}
			});
		}

		if (this.annotatedClasses != null) {
			xstream.processAnnotations(this.annotatedClasses);
		}
		if (this.autodetectAnnotations) {
			xstream.autodetectAnnotations(true);
		}
	}

	private Map<String, Class<?>> toClassMap(Map<String, ?> map) throws ClassNotFoundException {
		Map<String, Class<?>> result = CollectionUtils.newLinkedHashMap(map.size());
		for (Map.Entry<String, ?> entry : map.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			Class<?> type;
			if (value instanceof Class<?> clazz) {
				type = clazz;
			}
			else if (value instanceof String className) {
				type = ClassUtils.forName(className, this.beanClassLoader);
			}
			else {
				throw new IllegalArgumentException("Unknown value [" + value + "] - expected String or Class");
			}
			result.put(key, type);
		}
		return result;
	}

	/**
	 * Template to allow for customizing the given {@link XStream}.
	 * <p>The default implementation is empty.
	 * @param xstream the {@code XStream} instance
	 */
	protected void customizeXStream(XStream xstream) {
	}

	/**
	 * Return the native XStream delegate used by this marshaller.
	 * <p>The creation of the {@link XStream} instance returned by this method is
	 * thread safe.
	 * <p><b>NOTE: This method is marked as final.</b> It can be used to access
	 * the fully configured XStream for marshalling but not configuration purposes.
	 */
	public final XStream getXStream() {
		return this.xstream.obtain();
	}


	@Override
	public boolean supports(Class<?> clazz) {
		if (ObjectUtils.isEmpty(this.supportedClasses)) {
			return true;
		}
		else {
			for (Class<?> supportedClass : this.supportedClasses) {
				if (supportedClass.isAssignableFrom(clazz)) {
					return true;
				}
			}
			return false;
		}
	}


	// Marshalling

	@Override
	protected void marshalDomNode(Object graph, Node node) throws XmlMappingException {
		HierarchicalStreamWriter streamWriter;
		if (node instanceof Document document) {
			streamWriter = new DomWriter(document, this.nameCoder);
		}
		else if (node instanceof Element element) {
			streamWriter = new DomWriter(element, node.getOwnerDocument(), this.nameCoder);
		}
		else {
			throw new IllegalArgumentException("DOMResult contains neither Document nor Element");
		}
		doMarshal(graph, streamWriter, null);
	}

	@Override
	protected void marshalXmlEventWriter(Object graph, XMLEventWriter eventWriter) throws XmlMappingException {
		ContentHandler contentHandler = StaxUtils.createContentHandler(eventWriter);
		LexicalHandler lexicalHandler = (contentHandler instanceof LexicalHandler handler ? handler : null);
		marshalSaxHandlers(graph, contentHandler, lexicalHandler);
	}

	@Override
	protected void marshalXmlStreamWriter(Object graph, XMLStreamWriter streamWriter) throws XmlMappingException {
		try {
			StaxWriter writer;
			if (this.streamDriver instanceof StaxDriver staxDriver) {
				writer = staxDriver.createStaxWriter(streamWriter);
			}
			else {
				writer = new StaxWriter(new QNameMap(), streamWriter, this.nameCoder);
			}
			doMarshal(graph, writer, null);
		}
		catch (XMLStreamException ex) {
			throw convertXStreamException(ex, true);
		}
	}

	@Override
	protected void marshalSaxHandlers(Object graph, ContentHandler contentHandler, @Nullable LexicalHandler lexicalHandler)
			throws XmlMappingException {

		SaxWriter saxWriter = new SaxWriter(this.nameCoder);
		saxWriter.setContentHandler(contentHandler);
		doMarshal(graph, saxWriter, null);
	}

	@Override
	public void marshalOutputStream(Object graph, OutputStream outputStream) throws XmlMappingException, IOException {
		marshalOutputStream(graph, outputStream, null);
	}

	public void marshalOutputStream(Object graph, OutputStream outputStream, @Nullable DataHolder dataHolder)
			throws XmlMappingException, IOException {

		if (this.streamDriver != null) {
			doMarshal(graph, this.streamDriver.createWriter(outputStream), dataHolder);
		}
		else {
			marshalWriter(graph, new OutputStreamWriter(outputStream, this.encoding), dataHolder);
		}
	}

	@Override
	public void marshalWriter(Object graph, Writer writer) throws XmlMappingException, IOException {
		marshalWriter(graph, writer, null);
	}

	public void marshalWriter(Object graph, Writer writer, @Nullable DataHolder dataHolder)
			throws XmlMappingException, IOException {

		if (this.streamDriver != null) {
			doMarshal(graph, this.streamDriver.createWriter(writer), dataHolder);
		}
		else {
			doMarshal(graph, new CompactWriter(writer), dataHolder);
		}
	}

	/**
	 * Marshals the given graph to the given XStream HierarchicalStreamWriter.
	 * Converts exceptions using {@link #convertXStreamException}.
	 */
	private void doMarshal(Object graph, HierarchicalStreamWriter streamWriter, @Nullable DataHolder dataHolder) {
		try {
			getXStream().marshal(graph, streamWriter, dataHolder);
		}
		catch (Exception ex) {
			throw convertXStreamException(ex, true);
		}
		finally {
			try {
				streamWriter.flush();
			}
			catch (Exception ex) {
				logger.debug("Could not flush HierarchicalStreamWriter", ex);
			}
		}
	}


	// Unmarshalling

	@Override
	protected Object unmarshalStreamSource(StreamSource streamSource) throws XmlMappingException, IOException {
		if (streamSource.getInputStream() != null) {
			return unmarshalInputStream(streamSource.getInputStream());
		}
		else if (streamSource.getReader() != null) {
			return unmarshalReader(streamSource.getReader());
		}
		else {
			throw new IllegalArgumentException("StreamSource contains neither InputStream nor Reader");
		}
	}

	@Override
	protected Object unmarshalDomNode(Node node) throws XmlMappingException {
		HierarchicalStreamReader streamReader;
		if (node instanceof Document document) {
			streamReader = new DomReader(document, this.nameCoder);
		}
		else if (node instanceof Element element) {
			streamReader = new DomReader(element, this.nameCoder);
		}
		else {
			throw new IllegalArgumentException("DOMSource contains neither Document nor Element");
		}
		return doUnmarshal(streamReader, null);
	}

	@Override
	protected Object unmarshalXmlEventReader(XMLEventReader eventReader) throws XmlMappingException {
		try {
			XMLStreamReader streamReader = StaxUtils.createEventStreamReader(eventReader);
			return unmarshalXmlStreamReader(streamReader);
		}
		catch (XMLStreamException ex) {
			throw convertXStreamException(ex, false);
		}
	}

	@Override
	protected Object unmarshalXmlStreamReader(XMLStreamReader streamReader) throws XmlMappingException {
		return doUnmarshal(new StaxReader(new QNameMap(), streamReader, this.nameCoder), null);
	}

	@Override
	protected Object unmarshalSaxReader(XMLReader xmlReader, InputSource inputSource)
			throws XmlMappingException, IOException {

		throw new UnsupportedOperationException(
				"XStreamMarshaller does not support unmarshalling using SAX XMLReaders");
	}

	@Override
	public Object unmarshalInputStream(InputStream inputStream) throws XmlMappingException, IOException {
		return unmarshalInputStream(inputStream, null);
	}

	public Object unmarshalInputStream(InputStream inputStream, @Nullable DataHolder dataHolder) throws XmlMappingException, IOException {
		if (this.streamDriver != null) {
			return doUnmarshal(this.streamDriver.createReader(inputStream), dataHolder);
		}
		else {
			return unmarshalReader(new InputStreamReader(inputStream, this.encoding), dataHolder);
		}
	}

	@Override
	public Object unmarshalReader(Reader reader) throws XmlMappingException, IOException {
		return unmarshalReader(reader, null);
	}

	public Object unmarshalReader(Reader reader, @Nullable DataHolder dataHolder) throws XmlMappingException, IOException {
		return doUnmarshal(getDefaultDriver().createReader(reader), dataHolder);
	}

	/**
	 * Unmarshals the given graph to the given XStream HierarchicalStreamWriter.
	 * Converts exceptions using {@link #convertXStreamException}.
	 */
	private Object doUnmarshal(HierarchicalStreamReader streamReader, @Nullable DataHolder dataHolder) {
		try {
			return getXStream().unmarshal(streamReader, null, dataHolder);
		}
		catch (Exception ex) {
			throw convertXStreamException(ex, false);
		}
	}


	/**
	 * Convert the given XStream exception to an appropriate exception from the
	 * {@code org.springframework.oxm} hierarchy.
	 * <p>A boolean flag is used to indicate whether this exception occurs during marshalling or
	 * unmarshalling, since XStream itself does not make this distinction in its exception hierarchy.
	 * @param ex the XStream exception that occurred
	 * @param marshalling indicates whether the exception occurs during marshalling ({@code true}),
	 * or unmarshalling ({@code false})
	 * @return the corresponding {@code XmlMappingException}
	 */
	protected XmlMappingException convertXStreamException(Exception ex, boolean marshalling) {
		if (ex instanceof StreamException || ex instanceof CannotResolveClassException ||
				ex instanceof ForbiddenClassException || ex instanceof ConversionException) {
			if (marshalling) {
				return new MarshallingFailureException("XStream marshalling exception", ex);
			}
			else {
				return new UnmarshallingFailureException("XStream unmarshalling exception", ex);
			}
		}
		else {
			// fallback
			return new UncategorizedMappingException("Unknown XStream exception", ex);
		}
	}

}
