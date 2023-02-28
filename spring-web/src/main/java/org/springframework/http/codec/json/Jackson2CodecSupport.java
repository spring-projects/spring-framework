/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.http.codec.json;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Hints;
import org.springframework.http.HttpLogging;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import org.springframework.util.ObjectUtils;

/**
 * Base class providing support methods for Jackson 2.x encoding and decoding.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class Jackson2CodecSupport {

	/**
	 * The key for the hint to specify a "JSON View" for encoding or decoding
	 * with the value expected to be a {@link Class}.
	 * @see <a href="https://www.baeldung.com/jackson-json-view-annotation">Jackson JSON Views</a>
	 */
	public static final String JSON_VIEW_HINT = Jackson2CodecSupport.class.getName() + ".jsonView";

	/**
	 * The key for the hint to access the actual ResolvableType passed into
	 * {@link org.springframework.http.codec.HttpMessageReader#read(ResolvableType, ResolvableType, ServerHttpRequest, ServerHttpResponse, Map)}
	 * (server-side only). Currently set when the method argument has generics because
	 * in case of reactive types, use of {@code ResolvableType.getGeneric()} means no
	 * MethodParameter source and no knowledge of the containing class.
	 */
	static final String ACTUAL_TYPE_HINT = Jackson2CodecSupport.class.getName() + ".actualType";

	private static final String JSON_VIEW_HINT_ERROR =
			"@JsonView only supported for write hints with exactly 1 class argument: ";

	private static final List<MimeType> defaultMimeTypes = List.of(
			MediaType.APPLICATION_JSON,
			new MediaType("application", "*+json"),
			MediaType.APPLICATION_NDJSON);


	protected final Log logger = HttpLogging.forLogName(getClass());

	private ObjectMapper defaultObjectMapper;

	@Nullable
	private Map<Class<?>, Map<MimeType, ObjectMapper>> objectMapperRegistrations;

	private final List<MimeType> mimeTypes;


	/**
	 * Constructor with a Jackson {@link ObjectMapper} to use.
	 */
	protected Jackson2CodecSupport(ObjectMapper objectMapper, MimeType... mimeTypes) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		this.defaultObjectMapper = objectMapper;
		this.mimeTypes = (!ObjectUtils.isEmpty(mimeTypes) ? List.of(mimeTypes) : defaultMimeTypes);
	}


	/**
	 * Configure the default ObjectMapper instance to use.
	 * @param objectMapper the ObjectMapper instance
	 * @since 5.3.4
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		this.defaultObjectMapper = objectMapper;
	}

	/**
	 * Return the {@link #setObjectMapper configured} default ObjectMapper.
	 */
	public ObjectMapper getObjectMapper() {
		return this.defaultObjectMapper;
	}

	/**
	 * Configure the {@link ObjectMapper} instances to use for the given
	 * {@link Class}. This is useful when you want to deviate from the
	 * {@link #getObjectMapper() default} ObjectMapper or have the
	 * {@code ObjectMapper} vary by {@code MediaType}.
	 * <p><strong>Note:</strong> Use of this method effectively turns off use of
	 * the default {@link #getObjectMapper() ObjectMapper} and supported
	 * {@link #getMimeTypes() MimeTypes} for the given class. Therefore it is
	 * important for the mappings configured here to
	 * {@link MediaType#includes(MediaType) include} every MediaType that must
	 * be supported for the given class.
	 * @param clazz the type of Object to register ObjectMapper instances for
	 * @param registrar a consumer to populate or otherwise update the
	 * MediaType-to-ObjectMapper associations for the given Class
	 * @since 5.3.4
	 */
	public void registerObjectMappersForType(Class<?> clazz, Consumer<Map<MimeType, ObjectMapper>> registrar) {
		if (this.objectMapperRegistrations == null) {
			this.objectMapperRegistrations = new LinkedHashMap<>();
		}
		Map<MimeType, ObjectMapper> registrations =
				this.objectMapperRegistrations.computeIfAbsent(clazz, c -> new LinkedHashMap<>());
		registrar.accept(registrations);
	}

	/**
	 * Return ObjectMapper registrations for the given class, if any.
	 * @param clazz the class to look up for registrations for
	 * @return a map with registered MediaType-to-ObjectMapper registrations,
	 * or empty if in case of no registrations for the given class.
	 * @since 5.3.4
	 */
	@Nullable
	public Map<MimeType, ObjectMapper> getObjectMappersForType(Class<?> clazz) {
		for (Map.Entry<Class<?>, Map<MimeType, ObjectMapper>> entry : getObjectMapperRegistrations().entrySet()) {
			if (entry.getKey().isAssignableFrom(clazz)) {
				return entry.getValue();
			}
		}
		return Collections.emptyMap();
	}

	protected Map<Class<?>, Map<MimeType, ObjectMapper>> getObjectMapperRegistrations() {
		return (this.objectMapperRegistrations != null ? this.objectMapperRegistrations : Collections.emptyMap());
	}

	/**
	 * Subclasses should expose this as "decodable" or "encodable" mime types.
	 */
	protected List<MimeType> getMimeTypes() {
		return this.mimeTypes;
	}

	protected List<MimeType> getMimeTypes(ResolvableType elementType) {
		Class<?> elementClass = elementType.toClass();
		List<MimeType> result = null;
		for (Map.Entry<Class<?>, Map<MimeType, ObjectMapper>> entry : getObjectMapperRegistrations().entrySet()) {
			if (entry.getKey().isAssignableFrom(elementClass)) {
				result = (result != null ? result : new ArrayList<>(entry.getValue().size()));
				result.addAll(entry.getValue().keySet());
			}
		}
		if (!CollectionUtils.isEmpty(result)) {
			return result;
		}
		return (ProblemDetail.class.isAssignableFrom(elementClass) ? getMediaTypesForProblemDetail() : getMimeTypes());
	}

	/**
	 * Return the supported media type(s) for {@link ProblemDetail}.
	 * By default, an empty list, unless overridden in subclasses.
	 * @since 6.0.5
	 */
	protected List<MimeType> getMediaTypesForProblemDetail() {
		return Collections.emptyList();
	}

	protected boolean supportsMimeType(@Nullable MimeType mimeType) {
		if (mimeType == null) {
			return true;
		}
		for (MimeType supportedMimeType : this.mimeTypes) {
			if (supportedMimeType.isCompatibleWith(mimeType)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine whether to log the given exception coming from a
	 * {@link ObjectMapper#canDeserialize} / {@link ObjectMapper#canSerialize} check.
	 * @param type the class that Jackson tested for (de-)serializability
	 * @param cause the Jackson-thrown exception to evaluate
	 * (typically a {@link JsonMappingException})
	 * @since 5.3.1
	 */
	protected void logWarningIfNecessary(Type type, @Nullable Throwable cause) {
		if (cause == null) {
			return;
		}
		if (logger.isDebugEnabled()) {
			String msg = "Failed to evaluate Jackson " + (type instanceof JavaType ? "de" : "") +
					"serialization for type [" + type + "]";
			logger.debug(msg, cause);
		}
	}

	protected JavaType getJavaType(Type type, @Nullable Class<?> contextClass) {
		return this.defaultObjectMapper.constructType(GenericTypeResolver.resolveType(type, contextClass));
	}

	protected Map<String, Object> getHints(ResolvableType resolvableType) {
		MethodParameter param = getParameter(resolvableType);
		if (param != null) {
			Map<String, Object> hints = null;
			if (resolvableType.hasGenerics()) {
				hints = new HashMap<>(2);
				hints.put(ACTUAL_TYPE_HINT, resolvableType);
			}
			JsonView annotation = getAnnotation(param, JsonView.class);
			if (annotation != null) {
				Class<?>[] classes = annotation.value();
				Assert.isTrue(classes.length == 1, () -> JSON_VIEW_HINT_ERROR + param);
				hints = (hints != null ? hints : new HashMap<>(1));
				hints.put(JSON_VIEW_HINT, classes[0]);
			}
			if (hints != null) {
				return hints;
			}
		}
		return Hints.none();
	}

	@Nullable
	protected MethodParameter getParameter(ResolvableType type) {
		return (type.getSource() instanceof MethodParameter methodParameter ? methodParameter : null);
	}

	@Nullable
	protected abstract <A extends Annotation> A getAnnotation(MethodParameter parameter, Class<A> annotType);

	/**
	 * Select an ObjectMapper to use, either the main ObjectMapper or another
	 * if the handling for the given Class has been customized through
	 * {@link #registerObjectMappersForType(Class, Consumer)}.
	 * @since 5.3.4
	 */
	@Nullable
	protected ObjectMapper selectObjectMapper(ResolvableType targetType, @Nullable MimeType targetMimeType) {
		if (targetMimeType == null || CollectionUtils.isEmpty(this.objectMapperRegistrations)) {
			return this.defaultObjectMapper;
		}
		Class<?> targetClass = targetType.toClass();
		for (Map.Entry<Class<?>, Map<MimeType, ObjectMapper>> typeEntry : getObjectMapperRegistrations().entrySet()) {
			if (typeEntry.getKey().isAssignableFrom(targetClass)) {
				for (Map.Entry<MimeType, ObjectMapper> objectMapperEntry : typeEntry.getValue().entrySet()) {
					if (objectMapperEntry.getKey().includes(targetMimeType)) {
						return objectMapperEntry.getValue();
					}
				}
				// No matching registrations
				return null;
			}
		}
		// No registrations
		return this.defaultObjectMapper;
	}

}
