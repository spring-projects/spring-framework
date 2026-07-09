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

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.SmartHttpMessageConverter;

/**
 * A {@link RequestBodyAdvice} implementation that adds a {@code "contextClass"} hint for {@link Optional},
 * {@link HttpEntity} and {@link ServerSentEvent} container types.
 *
 * @author Sebastien Deleuze
 * @since 7.0.3
 */
public class ContextClassRequestBodyAdvice extends RequestBodyAdviceAdapter {

	@Override
	public boolean supports(MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
		if (!AbstractJacksonHttpMessageConverter.class.isAssignableFrom(converterType)) {
			return false;
		}
		Class<?> parameterType = parameter.getParameterType();
		return Optional.class.isAssignableFrom(parameterType) || HttpEntity.class.isAssignableFrom(parameterType) ||
				ServerSentEvent.class.isAssignableFrom(parameterType);
	}

	@Override
	public @Nullable Map<String, Object> determineReadHints(MethodParameter parameter, Type targetType,
			Class<? extends SmartHttpMessageConverter<?>> converterType) {

		return Collections.singletonMap("contextClass", parameter.getContainingClass());
	}
}
