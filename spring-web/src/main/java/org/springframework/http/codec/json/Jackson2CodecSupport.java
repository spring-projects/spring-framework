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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
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

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * Base class providing support methods for Jackson 2 encoding and decoding.
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


	protected final ObjectMapper mapper;


	/**
	 * Constructor with a Jackson {@link ObjectMapper} to use.
	 */
	protected Jackson2CodecSupport(ObjectMapper mapper) {
		Assert.notNull(mapper, "ObjectMapper must not be null");
		this.mapper = mapper;
	}


	protected boolean supportsMimeType(MimeType mimeType) {
		return mimeType == null ||
				JSON_MIME_TYPES.stream().anyMatch(m -> m.isCompatibleWith(mimeType));
	}

	/**
	 * Return the Jackson {@link JavaType} for the specified type and context class.
	 * <p>The default implementation returns {@code typeFactory.constructType(type, contextClass)},
	 * but this can be overridden in subclasses, to allow for custom generic collection handling.
	 * For instance:
	 * <pre class="code">
	 * protected JavaType getJavaType(Type type) {
	 *   if (type instanceof Class && List.class.isAssignableFrom((Class)type)) {
	 *     return TypeFactory.collectionType(ArrayList.class, MyBean.class);
	 *   } else {
	 *     return super.getJavaType(type);
	 *   }
	 * }
	 * </pre>
	 * @param type the generic type to return the Jackson JavaType for
	 * @param contextClass a context class for the target type, for example a class
	 * in which the target type appears in a method signature (can be {@code null})
	 * @return the Jackson JavaType
	 */
	protected JavaType getJavaType(Type type, Class<?> contextClass) {
		TypeFactory typeFactory = this.mapper.getTypeFactory();
		if (contextClass != null) {
			ResolvableType resolvedType = ResolvableType.forType(type);
			if (type instanceof TypeVariable) {
				ResolvableType resolvedTypeVariable = resolveVariable(
						(TypeVariable<?>) type, ResolvableType.forClass(contextClass));
				if (resolvedTypeVariable != ResolvableType.NONE) {
					return typeFactory.constructType(resolvedTypeVariable.resolve());
				}
			}
			else if (type instanceof ParameterizedType && resolvedType.hasUnresolvableGenerics()) {
				ParameterizedType parameterizedType = (ParameterizedType) type;
				Class<?>[] generics = new Class<?>[parameterizedType.getActualTypeArguments().length];
				Type[] typeArguments = parameterizedType.getActualTypeArguments();
				for (int i = 0; i < typeArguments.length; i++) {
					Type typeArgument = typeArguments[i];
					if (typeArgument instanceof TypeVariable) {
						ResolvableType resolvedTypeArgument = resolveVariable(
								(TypeVariable<?>) typeArgument, ResolvableType.forClass(contextClass));
						if (resolvedTypeArgument != ResolvableType.NONE) {
							generics[i] = resolvedTypeArgument.resolve();
						}
						else {
							generics[i] = ResolvableType.forType(typeArgument).resolve();
						}
					}
					else {
						generics[i] = ResolvableType.forType(typeArgument).resolve();
					}
				}
				return typeFactory.constructType(ResolvableType.
						forClassWithGenerics(resolvedType.getRawClass(), generics).getType());
			}
		}
		return typeFactory.constructType(type);
	}

	private ResolvableType resolveVariable(TypeVariable<?> typeVariable, ResolvableType contextType) {
		ResolvableType resolvedType;
		if (contextType.hasGenerics()) {
			resolvedType = ResolvableType.forType(typeVariable, contextType);
			if (resolvedType.resolve() != null) {
				return resolvedType;
			}
		}
		resolvedType = resolveVariable(typeVariable, contextType.getSuperType());
		if (resolvedType.resolve() != null) {
			return resolvedType;
		}
		for (ResolvableType ifc : contextType.getInterfaces()) {
			resolvedType = resolveVariable(typeVariable, ifc);
			if (resolvedType.resolve() != null) {
				return resolvedType;
			}
		}
		return ResolvableType.NONE;
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
		return Optional.ofNullable (type.getSource() instanceof MethodParameter ?
				(MethodParameter) type.getSource() : null);
	}

	protected abstract <A extends Annotation> A getAnnotation(MethodParameter parameter, Class<A> annotType);

}
