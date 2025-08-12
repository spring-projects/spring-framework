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

package org.springframework.http.codec;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonView;
import org.apache.commons.logging.Log;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.ser.FilterProvider;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Hints;
import org.springframework.http.HttpLogging;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;

/**
 * Base class providing support methods for Jackson 3.x encoding and decoding.
 *
 * @author Sebastien Deleuze
 * @since 7.0
 * @param <T> the type of {@link ObjectMapper}
 */
public abstract class JacksonCodecSupport<T extends ObjectMapper> {

	/**
	 * The key for the hint to specify a "JSON View" for encoding or decoding
	 * with the value expected to be a {@link Class}.
	 */
	public static final String JSON_VIEW_HINT = JsonView.class.getName();

	/**
	 * The key for the hint to specify a {@link FilterProvider}.
	 */
	public static final String FILTER_PROVIDER_HINT = FilterProvider.class.getName();

	/**
	 * The key for the hint to access the actual ResolvableType passed into
	 * {@link org.springframework.http.codec.HttpMessageReader#read(ResolvableType, ResolvableType, ServerHttpRequest, ServerHttpResponse, Map)}
	 * (server-side only). Currently set when the method argument has generics because
	 * in case of reactive types, use of {@code ResolvableType.getGeneric()} means no
	 * MethodParameter source and no knowledge of the containing class.
	 */
	static final String ACTUAL_TYPE_HINT = JacksonCodecSupport.class.getName() + ".actualType";

	private static final String JSON_VIEW_HINT_ERROR =
			"@JsonView only supported for write hints with exactly 1 class argument: ";


	protected final Log logger = HttpLogging.forLogName(getClass());

	private final T defaultMapper;

	protected @Nullable Map<Class<?>, Map<MimeType, T>> mapperRegistrations;

	private final List<MimeType> mimeTypes;

	private static volatile @Nullable List<JacksonModule> modules = null;

	/**
	 * Construct a new instance with the provided {@link MapperBuilder builder}
	 * customized with the {@link tools.jackson.databind.JacksonModule}s found
	 * by {@link MapperBuilder#findModules(ClassLoader)} and {@link MimeType}s.
	 */
	protected JacksonCodecSupport(MapperBuilder<T, ?> builder, MimeType... mimeTypes) {
		Assert.notNull(builder, "MapperBuilder must not be null");
		Assert.notEmpty(mimeTypes, "MimeTypes must not be empty");
		this.defaultMapper = builder.addModules(initModules()).build();
		this.mimeTypes = List.of(mimeTypes);
	}

	/**
	 * Construct a new instance with the provided {@link ObjectMapper}
	 * customized with the {@link tools.jackson.databind.JacksonModule}s found
	 * by {@link MapperBuilder#findModules(ClassLoader)} and {@link MimeType}s.
	 */
	protected JacksonCodecSupport(T mapper, MimeType... mimeTypes) {
		Assert.notNull(mapper, "ObjectMapper must not be null");
		Assert.notEmpty(mimeTypes, "MimeTypes must not be empty");
		this.defaultMapper = mapper;
		this.mimeTypes = List.of(mimeTypes);
	}

	private List<JacksonModule> initModules() {
		if (modules == null) {
			modules = MapperBuilder.findModules(JacksonCodecSupport.class.getClassLoader());

		}
		return Objects.requireNonNull(modules);
	}

	/**
	 * Return the {@link ObjectMapper configured} default mapper.
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
	 * the default {@link #getMapper() ObjectMapper} and supported
	 * {@link #getMimeTypes() MimeTypes} for the given class. Therefore it is
	 * important for the mappings configured here to
	 * {@link MediaType#includes(MediaType) include} every MediaType that must
	 * be supported for the given class.
	 * @param clazz the type of Object to register ObjectMapper instances for
	 * @param registrar a consumer to populate or otherwise update the
	 * MediaType-to-ObjectMapper associations for the given Class
	 */
	public void registerMappersForType(Class<?> clazz, Consumer<Map<MimeType, T>> registrar) {
		if (this.mapperRegistrations == null) {
			this.mapperRegistrations = new LinkedHashMap<>();
		}
		Map<MimeType, T> registrations =
				this.mapperRegistrations.computeIfAbsent(clazz, c -> new LinkedHashMap<>());
		registrar.accept(registrations);
	}

	/**
	 * Return ObjectMapper registrations for the given class, if any.
	 * @param clazz the class to look up for registrations for
	 * @return a map with registered MediaType-to-ObjectMapper registrations,
	 * or empty if in case of no registrations for the given class.
	 */
	public @Nullable Map<MimeType, T> getMappersForType(Class<?> clazz) {
		for (Map.Entry<Class<?>, Map<MimeType, T>> entry : getMapperRegistrations().entrySet()) {
			if (entry.getKey().isAssignableFrom(clazz)) {
				return entry.getValue();
			}
		}
		return Collections.emptyMap();
	}

	protected Map<Class<?>, Map<MimeType, T>> getMapperRegistrations() {
		return (this.mapperRegistrations != null ? this.mapperRegistrations : Collections.emptyMap());
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
		for (Map.Entry<Class<?>, Map<MimeType, T>> entry : getMapperRegistrations().entrySet()) {
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

	protected JavaType getJavaType(Type type, @Nullable Class<?> contextClass) {
		return this.defaultMapper.constructType(GenericTypeResolver.resolveType(type, contextClass));
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

	protected @Nullable MethodParameter getParameter(ResolvableType type) {
		return (type.getSource() instanceof MethodParameter methodParameter ? methodParameter : null);
	}

	protected abstract <A extends Annotation> @Nullable A getAnnotation(MethodParameter parameter, Class<A> annotType);

	/**
	 * Select an ObjectMapper to use, either the main ObjectMapper or another
	 * if the handling for the given Class has been customized through
	 * {@link #registerMappersForType(Class, Consumer)}.
	 */
	protected @Nullable T selectMapper(ResolvableType targetType, @Nullable MimeType targetMimeType) {
		if (targetMimeType == null || CollectionUtils.isEmpty(this.mapperRegistrations)) {
			return this.defaultMapper;
		}
		Class<?> targetClass = targetType.toClass();
		for (Map.Entry<Class<?>, Map<MimeType, T>> typeEntry : getMapperRegistrations().entrySet()) {
			if (typeEntry.getKey().isAssignableFrom(targetClass)) {
				for (Map.Entry<MimeType, T> mapperEntry : typeEntry.getValue().entrySet()) {
					if (mapperEntry.getKey().includes(targetMimeType)) {
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

}
