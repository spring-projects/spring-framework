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

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import kotlin.reflect.KFunction;
import kotlin.reflect.KType;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractKotlinSerializationHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

/**
 * A {@link ResponseBodyAdvice} implementation that adds support for resolving
 * Kotlin {@link KType} from the return type and providing it as a hint with a
 * {@code "kotlin.reflect.KType"} key.
 *
 * @author Sebastien Deleuze
 * @since 7.0
 */
@SuppressWarnings("removal")
public class KotlinResponseBodyAdvice implements ResponseBodyAdvice<Object> {

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return AbstractKotlinSerializationHttpMessageConverter.class.isAssignableFrom(converterType);
	}

	@Override
	public @Nullable Object beforeBodyWrite(@Nullable Object body, MethodParameter returnType, MediaType selectedContentType,
			Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {

		return body;
	}

	@Override
	public @Nullable Map<String, Object> determineWriteHints(@Nullable Object body, MethodParameter returnType,
			MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType) {

		KFunction<?> function = ReflectJvmMapping.getKotlinFunction(Objects.requireNonNull(returnType.getMethod()));
		KType type = Objects.requireNonNull(function).getReturnType();
		if (HttpEntity.class.isAssignableFrom(returnType.getParameterType())) {
			return Collections.singletonMap(KType.class.getName(), Objects.requireNonNull(type.getArguments().get(0).getType()));
		}
		return Collections.singletonMap(KType.class.getName(), type);
	}

}
