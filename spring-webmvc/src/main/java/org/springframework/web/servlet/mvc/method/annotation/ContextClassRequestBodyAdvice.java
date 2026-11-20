/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
