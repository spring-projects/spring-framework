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

package org.springframework.http.codec.json;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * Base class providing support methods for Jackson 2.9 encoding and decoding.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class Jackson2CodecSupport {

	/**
	 * The key for the hint to specify a "JSON View" for encoding or decoding
	 * with the value expected to be a {@link Class}.
	 * @see <a href="http://wiki.fasterxml.com/JacksonJsonViews">Jackson JSON Views</a>
	 */
	public static final String JSON_VIEW_HINT = Jackson2CodecSupport.class.getName() + ".jsonView";

	private static final String JSON_VIEW_HINT_ERROR =
			"@JsonView only supported for write hints with exactly 1 class argument: ";

	protected static final List<MimeType> JSON_MIME_TYPES = Arrays.asList(
				new MimeType("application", "json", StandardCharsets.UTF_8),
				new MimeType("application", "*+json", StandardCharsets.UTF_8));


	protected final ObjectMapper objectMapper;


	/**
	 * Constructor with a Jackson {@link ObjectMapper} to use.
	 */
	protected Jackson2CodecSupport(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		this.objectMapper = objectMapper;
	}


	protected boolean supportsMimeType(MimeType mimeType) {
		return (mimeType == null ||
				JSON_MIME_TYPES.stream().anyMatch(m -> m.isCompatibleWith(mimeType)));
	}

	protected JavaType getJavaType(Type type, Class<?> contextClass) {
		TypeFactory typeFactory = this.objectMapper.getTypeFactory();
		return typeFactory.constructType(GenericTypeResolver.resolveType(type, contextClass));
	}

	protected Map<String, Object> getHints(ResolvableType actualType) {
		return getParameter(actualType)
				.flatMap(parameter -> Optional.ofNullable(getAnnotation(parameter, JsonView.class))
						.map(annotation -> {
							Class<?>[] classes = annotation.value();
							Assert.isTrue(classes.length == 1, JSON_VIEW_HINT_ERROR + parameter);
							return Collections.<String, Object>singletonMap(JSON_VIEW_HINT, classes[0]);
						}))
				.orElse(Collections.emptyMap());
	}

	protected Optional<MethodParameter> getParameter(ResolvableType type) {
		return Optional.ofNullable(type.getSource() instanceof MethodParameter ?
				(MethodParameter) type.getSource() : null);
	}

	protected abstract <A extends Annotation> A getAnnotation(MethodParameter parameter, Class<A> annotType);

}
