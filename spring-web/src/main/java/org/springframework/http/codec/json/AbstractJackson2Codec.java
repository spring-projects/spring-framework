/*
 * Copyright 2002-2016 the original author or authors.
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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * Abstract base class for Jackson based decoder/encoder implementations.
 *
 * @author Sebastien Deleuze
 */
public class AbstractJackson2Codec {

	/**
	 * Hint key to use with a {@link Class} value specifying the JSON View to use to serialize
	 * or deserialize an object.
	 * @see <a href="http://wiki.fasterxml.com/JacksonJsonViews">Jackson JSON Views</a>
	 */
	public static final String JSON_VIEW_HINT = AbstractJackson2Codec.class.getName() + ".jsonView";

	protected static final List<MimeType> JSON_MIME_TYPES = Arrays.asList(
				new MimeType("application", "json", StandardCharsets.UTF_8),
				new MimeType("application", "*+json", StandardCharsets.UTF_8));


	protected final ObjectMapper mapper;


	/**
	 * Create a new Jackson codec for the given mapper.
	 * @param mapper the Jackson ObjectMapper to use
	 */
	protected AbstractJackson2Codec(ObjectMapper mapper) {
		Assert.notNull(mapper, "ObjectMapper must not be null");
		this.mapper = mapper;
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

}
