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

package org.springframework.http.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonView;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonEncoding;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.PrettyPrinter;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.ser.FilterProvider;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonInputMessage;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.TypeUtils;

/**
 * Abstract base class for Jackson based and content type independent
 * {@link HttpMessageConverter} implementations.
 *
 * <p>The following hint entries are supported:
 * <ul>
 *     <li>A JSON view with a <code>com.fasterxml.jackson.annotation.JsonView</code>
 *         key and the class name of the JSON view as value.</li>
 *     <li>A filter provider with a <code>tools.jackson.databind.ser.FilterProvider</code>
 *         key and the filter provider class name as value.</li>
 * </ul>
 *
 * @author Sebastien Deleuze
 * @since 7.0
 * @param <T> the type of {@link ObjectMapper}
 * @see JacksonJsonHttpMessageConverter
 */
public abstract class AbstractJacksonHttpMessageConverter<T extends ObjectMapper> extends AbstractSmartHttpMessageConverter<Object> {

	private static final String JSON_VIEW_HINT = JsonView.class.getName();

	private static final String FILTER_PROVIDER_HINT = FilterProvider.class.getName();

	private static final Map<String, JsonEncoding> ENCODINGS;

	private static volatile @Nullable List<JacksonModule> modules = null;

	static {
		ENCODINGS = CollectionUtils.newHashMap(JsonEncoding.values().length);
		for (JsonEncoding encoding : JsonEncoding.values()) {
			ENCODINGS.put(encoding.getJavaName(), encoding);
		}
		ENCODINGS.put("US-ASCII", JsonEncoding.UTF8);
	}


	protected final T defaultMapper;

	private @Nullable Map<Class<?>, Map<MediaType, T>> mapperRegistrations;

	private final @Nullable PrettyPrinter ssePrettyPrinter;


	/**
	 * Construct a new instance with the provided {@link MapperBuilder builder}
	 * customized with the {@link tools.jackson.databind.JacksonModule}s found
	 * by {@link MapperBuilder#findModules(ClassLoader)}.
	 */
	private AbstractJacksonHttpMessageConverter(MapperBuilder<T, ?> builder) {
		this.defaultMapper = builder.addModules(initModules()).build();
		this.ssePrettyPrinter = initSsePrettyPrinter();
	}

	/**
	 * Construct a new instance with the provided {@link MapperBuilder builder}
	 * customized with the {@link tools.jackson.databind.JacksonModule}s found
	 * by {@link MapperBuilder#findModules(ClassLoader)} and {@link MediaType}.
	 */
	protected AbstractJacksonHttpMessageConverter(MapperBuilder<T, ?> builder, MediaType supportedMediaType) {
		this(builder);
		setSupportedMediaTypes(Collections.singletonList(supportedMediaType));
	}

	/**
	 * Construct a new instance with the provided {@link MapperBuilder builder}
	 * customized with the {@link tools.jackson.databind.JacksonModule}s found
	 * by {@link MapperBuilder#findModules(ClassLoader)} and {@link MediaType}s.
	 */
	protected AbstractJacksonHttpMessageConverter(MapperBuilder<T, ?> builder, MediaType... supportedMediaTypes) {
		this(builder);
		setSupportedMediaTypes(Arrays.asList(supportedMediaTypes));
	}

	/**
	 * Construct a new instance with the provided {@link ObjectMapper}.
	 */
	protected AbstractJacksonHttpMessageConverter(T mapper) {
		this.defaultMapper = mapper;
		this.ssePrettyPrinter = initSsePrettyPrinter();
	}

	/**
	 * Construct a new instance with the provided {@link ObjectMapper} and {@link MediaType}.
	 */
	protected AbstractJacksonHttpMessageConverter(T mapper, MediaType supportedMediaType) {
		this(mapper);
		setSupportedMediaTypes(Collections.singletonList(supportedMediaType));
	}

	/**
	 * Construct a new instance with the provided {@link ObjectMapper} and {@link MediaType}s.
	 */
	protected AbstractJacksonHttpMessageConverter(T mapper, MediaType... supportedMediaTypes) {
		this(mapper);
		setSupportedMediaTypes(Arrays.asList(supportedMediaTypes));
	}

	private List<JacksonModule> initModules() {
		if (modules == null) {
			modules = MapperBuilder.findModules(AbstractJacksonHttpMessageConverter.class.getClassLoader());

		}
		return Objects.requireNonNull(modules);
	}

	private PrettyPrinter initSsePrettyPrinter() {
		DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
		prettyPrinter.indentObjectsWith(new DefaultIndenter("  ", "\ndata:"));
		return prettyPrinter;
	}

	@Override
	public void setSupportedMediaTypes(List<MediaType> supportedMediaTypes) {
		super.setSupportedMediaTypes(supportedMediaTypes);
	}

	/**
	 * Return the main {@link ObjectMapper} in use.
	 */
	public T getMapper() {
		return this.defaultMapper;
	}

	/**
	 * Configure the {@link ObjectMapper} instances to use for the given
	 * {@link Class}. This is useful when you want to deviate from the
	 * {@link #getMapper() default} ObjectMapper or have the
	 * {@code ObjectMapper} vary by {@code MediaType}.
	 * <p><strong>Note:</strong> Use of this method effectively turns off use of
	 * the default {@link #getMapper() ObjectMapper} and
	 * {@link #setSupportedMediaTypes(List) supportedMediaTypes} for the given
	 * class. Therefore it is important for the mappings configured here to
	 * {@link MediaType#includes(MediaType) include} every MediaType that must
	 * be supported for the given class.
	 * @param clazz the type of Object to register ObjectMapper instances for
	 * @param registrar a consumer to populate or otherwise update the
	 * MediaType-to-ObjectMapper associations for the given Class
	 */
	public void registerMappersForType(Class<?> clazz, Consumer<Map<MediaType, T>> registrar) {
		if (this.mapperRegistrations == null) {
			this.mapperRegistrations = new LinkedHashMap<>();
		}
		Map<MediaType, T> registrations =
				this.mapperRegistrations.computeIfAbsent(clazz, c -> new LinkedHashMap<>());
		registrar.accept(registrations);
	}

	/**
	 * Return ObjectMapper registrations for the given class, if any.
	 * @param clazz the class to look up for registrations for
	 * @return a map with registered MediaType-to-ObjectMapper registrations,
	 * or empty if in case of no registrations for the given class.
	 */
	public Map<MediaType, T> getMappersForType(Class<?> clazz) {
		for (Map.Entry<Class<?>, Map<MediaType, T>> entry : getMapperRegistrations().entrySet()) {
			if (entry.getKey().isAssignableFrom(clazz)) {
				return entry.getValue();
			}
		}
		return Collections.emptyMap();
	}

	@Override
	public List<MediaType> getSupportedMediaTypes(Class<?> clazz) {
		List<MediaType> result = null;
		for (Map.Entry<Class<?>, Map<MediaType, T>> entry : getMapperRegistrations().entrySet()) {
			if (entry.getKey().isAssignableFrom(clazz)) {
				result = (result != null ? result : new ArrayList<>(entry.getValue().size()));
				result.addAll(entry.getValue().keySet());
			}
		}
		if (!CollectionUtils.isEmpty(result)) {
			return result;
		}
		return (ProblemDetail.class.isAssignableFrom(clazz) ?
				getMediaTypesForProblemDetail() : getSupportedMediaTypes());
	}

	private Map<Class<?>, Map<MediaType, T>> getMapperRegistrations() {
		return (this.mapperRegistrations != null ? this.mapperRegistrations : Collections.emptyMap());
	}

	/**
	 * Return the supported media type(s) for {@link ProblemDetail}.
	 * By default, an empty list, unless overridden in subclasses.
	 */
	protected List<MediaType> getMediaTypesForProblemDetail() {
		return Collections.emptyList();
	}


	@Override
	public boolean canRead(ResolvableType type, @Nullable MediaType mediaType) {
		if (!canRead(mediaType)) {
			return false;
		}
		Class<?> clazz = type.resolve();
		if (clazz == null) {
			return false;
		}
		return this.mapperRegistrations == null || selectMapper(clazz, mediaType) != null;
	}

	@Override
	@SuppressWarnings("removal")
	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
		if (!canWrite(mediaType)) {
			return false;
		}
		if (mediaType != null && mediaType.getCharset() != null) {
			Charset charset = mediaType.getCharset();
			if (!ENCODINGS.containsKey(charset.name())) {
				return false;
			}
		}
		if (MappingJacksonValue.class.isAssignableFrom(clazz)) {
			throw new UnsupportedOperationException("MappingJacksonValue is not supported, use hints instead");
		}
		return this.mapperRegistrations == null || selectMapper(clazz, mediaType) != null;
	}

	/**
	 * Select an ObjectMapper to use, either the main ObjectMapper or another
	 * if the handling for the given Class has been customized through
	 * {@link #registerMappersForType(Class, Consumer)}.
	 */
	private @Nullable T selectMapper(Class<?> targetType, @Nullable MediaType targetMediaType) {
		if (targetMediaType == null || CollectionUtils.isEmpty(this.mapperRegistrations)) {
			return this.defaultMapper;
		}
		for (Map.Entry<Class<?>, Map<MediaType, T>> typeEntry : getMapperRegistrations().entrySet()) {
			if (typeEntry.getKey().isAssignableFrom(targetType)) {
				for (Map.Entry<MediaType, T> mapperEntry : typeEntry.getValue().entrySet()) {
					if (mapperEntry.getKey().includes(targetMediaType)) {
						return mapperEntry.getValue();
					}
				}
				// No matching registrations
				return null;
			}
		}
		// No registrations
		return this.defaultMapper;
	}

	@Override
	public Object read(ResolvableType type, HttpInputMessage inputMessage, @Nullable Map<String, Object> hints)
			throws IOException, HttpMessageNotReadableException {

		Class<?> contextClass = (type.getSource() instanceof MethodParameter parameter ? parameter.getContainingClass() : null);
		JavaType javaType = getJavaType(type.getType(), contextClass);
		return readJavaType(javaType, inputMessage, hints);
	}

	@Override
	protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		JavaType javaType = getJavaType(clazz, null);
		return readJavaType(javaType, inputMessage, null);
	}

	@SuppressWarnings("removal")
	private Object readJavaType(JavaType javaType, HttpInputMessage inputMessage, @Nullable Map<String, Object> hints) throws IOException {
		MediaType contentType = inputMessage.getHeaders().getContentType();
		Charset charset = getCharset(contentType);

		T mapper = selectMapper(javaType.getRawClass(), contentType);
		Assert.state(mapper != null, () -> "No ObjectMapper for " + javaType);

		boolean isUnicode = ENCODINGS.containsKey(charset.name()) ||
				"UTF-16".equals(charset.name()) ||
				"UTF-32".equals(charset.name());
		try {
			InputStream inputStream = StreamUtils.nonClosing(inputMessage.getBody());
			if (inputMessage instanceof MappingJacksonInputMessage) {
				throw new UnsupportedOperationException("MappingJacksonInputMessage is not supported, use hints instead");
			}
			ObjectReader objectReader = mapper.readerFor(javaType);
			if (hints != null && hints.containsKey(JSON_VIEW_HINT)) {
				objectReader = objectReader.withView((Class<?>) hints.get(JSON_VIEW_HINT));
			}
			objectReader = customizeReader(objectReader, javaType);
			if (isUnicode) {
				return objectReader.readValue(inputStream);
			}
			else {
				Reader reader = new InputStreamReader(inputStream, charset);
				return objectReader.readValue(reader);
			}
		}
		catch (InvalidDefinitionException ex) {
			throw new HttpMessageConversionException("Type definition error: " + ex.getType(), ex);
		}
		catch (JacksonException ex) {
			throw new HttpMessageNotReadableException("JSON parse error: " + ex.getOriginalMessage(), ex, inputMessage);
		}
	}

	/**
	 * Subclasses can use this method to customize the {@link ObjectReader} used
	 * for reading values.
	 * @param reader the reader instance to customize
	 * @param javaType the type of element values to read
	 * @return the customized {@link ObjectReader}
	 */
	protected ObjectReader customizeReader(ObjectReader reader, JavaType javaType) {
		return reader;
	}

	/**
	 * Determine the charset to use for JSON input.
	 * <p>By default this is either the charset from the input {@code MediaType}
	 * or otherwise {@code UTF-8}. Can be overridden in subclasses.
	 * @param contentType the content type of the HTTP input message
	 * @return the charset to use
	 */
	protected Charset getCharset(@Nullable MediaType contentType) {
		if (contentType != null && contentType.getCharset() != null) {
			return contentType.getCharset();
		}
		else {
			return StandardCharsets.UTF_8;
		}
	}

	@Override
	protected void writeInternal(Object object, ResolvableType resolvableType, HttpOutputMessage outputMessage, @Nullable Map<String, Object> hints)
			throws IOException, HttpMessageNotWritableException {

		MediaType contentType = outputMessage.getHeaders().getContentType();
		JsonEncoding encoding = getJsonEncoding(contentType);

		Class<?> clazz = object.getClass();
		T mapper = selectMapper(clazz, contentType);
		Assert.state(mapper != null, () -> "No ObjectMapper for " + clazz.getName());

		OutputStream outputStream = StreamUtils.nonClosing(outputMessage.getBody());
		Class<?> jsonView = null;
		FilterProvider filters = null;
		JavaType javaType = null;

		Type type = resolvableType.getType();
		if (TypeUtils.isAssignable(type, object.getClass())) {
			javaType = getJavaType(type, null);
		}
		if (hints != null) {
			jsonView = (Class<?>) hints.get(JSON_VIEW_HINT);
			filters = (FilterProvider) hints.get(FILTER_PROVIDER_HINT);
		}

		ObjectWriter objectWriter = (jsonView != null ?
				mapper.writerWithView(jsonView) : mapper.writer());
		if (filters != null) {
			objectWriter = objectWriter.with(filters);
		}
		if (javaType != null && (javaType.isContainerType() || javaType.isTypeOrSubTypeOf(Optional.class))) {
			objectWriter = objectWriter.forType(javaType);
		}
		SerializationConfig config = objectWriter.getConfig();
		if (contentType != null && contentType.isCompatibleWith(MediaType.TEXT_EVENT_STREAM) &&
				config.isEnabled(SerializationFeature.INDENT_OUTPUT)) {
			objectWriter = objectWriter.with(this.ssePrettyPrinter);
		}
		objectWriter = customizeWriter(objectWriter, javaType, contentType);

		try (JsonGenerator generator = objectWriter.createGenerator(outputStream, encoding)) {
			writePrefix(generator, object);
			objectWriter.writeValue(generator, object);
			writeSuffix(generator, object);
			generator.flush();
		}
		catch (InvalidDefinitionException ex) {
			throw new HttpMessageConversionException("Type definition error: " + ex.getType(), ex);
		}
		catch (JacksonException ex) {
			throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getOriginalMessage(), ex);
		}
	}

	/**
	 * Subclasses can use this method to customize the {@link ObjectWriter} used
	 * for writing values.
	 * @param writer the writer instance to customize
	 * @param javaType the type of element values to write
	 * @param contentType the selected media type
	 * @return the customized {@link ObjectWriter}
	 */
	protected ObjectWriter customizeWriter(
			ObjectWriter writer, @Nullable JavaType javaType, @Nullable MediaType contentType) {

		return writer;
	}

	/**
	 * Write a prefix before the main content.
	 * @param generator the generator to use for writing content.
	 * @param object the object to write to the output message
	 */
	protected void writePrefix(JsonGenerator generator, Object object) {
	}

	/**
	 * Write a suffix after the main content.
	 * @param generator the generator to use for writing content.
	 * @param object the object to write to the output message
	 */
	protected void writeSuffix(JsonGenerator generator, Object object) {
	}

	/**
	 * Return the Jackson {@link JavaType} for the specified type and context class.
	 * @param type the generic type to return the Jackson JavaType for
	 * @param contextClass a context class for the target type, for example a class
	 * in which the target type appears in a method signature (can be {@code null})
	 * @return the Jackson JavaType
	 */
	protected JavaType getJavaType(Type type, @Nullable Class<?> contextClass) {
		return this.defaultMapper.constructType(GenericTypeResolver.resolveType(type, contextClass));
	}

	/**
	 * Determine the JSON encoding to use for the given content type.
	 * @param contentType the media type as requested by the caller
	 * @return the JSON encoding to use (never {@code null})
	 */
	protected JsonEncoding getJsonEncoding(@Nullable MediaType contentType) {
		if (contentType != null && contentType.getCharset() != null) {
			Charset charset = contentType.getCharset();
			JsonEncoding encoding = ENCODINGS.get(charset.name());
			if (encoding != null) {
				return encoding;
			}
		}
		return JsonEncoding.UTF8;
	}

	@Override
	protected boolean supportsRepeatableWrites(Object o) {
		return true;
	}

}
