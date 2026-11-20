/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.mvc.method.annotation;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

/**
 * A convenient base class for {@code ResponseBodyAdvice} implementations
 * that customize the response before JSON serialization with
 * {@link AbstractJacksonHttpMessageConverter}'s and
 * {@link AbstractJackson2HttpMessageConverter}'s concrete subclasses.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 4.1
 */
@SuppressWarnings("removal")
public abstract class AbstractMappingJacksonResponseBodyAdvice implements ResponseBodyAdvice<Object> {

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return AbstractJacksonHttpMessageConverter.class.isAssignableFrom(converterType) ||
				AbstractJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
	}

	@Override
	public final @Nullable Object beforeBodyWrite(@Nullable Object body, MethodParameter returnType,
			MediaType contentType, Class<? extends HttpMessageConverter<?>> converterType,
			ServerHttpRequest request, ServerHttpResponse response) {

		if (body == null) {
			return null;
		}
		if (AbstractJacksonHttpMessageConverter.class.isAssignableFrom(converterType)) {
			return body;
		}
		MappingJacksonValue container = getOrCreateContainer(body);
		beforeBodyWriteInternal(container, contentType, returnType, request, response);
		return container;
	}

	/**
	 * Wrap the body in a {@link MappingJacksonValue} value container (for providing
	 * additional serialization instructions) or simply cast it if already wrapped.
	 */
	protected MappingJacksonValue getOrCreateContainer(Object body) {
		return (body instanceof MappingJacksonValue mjv ? mjv : new MappingJacksonValue(body));
	}

	/**
	 * Invoked only if the converter type is {@code MappingJackson2HttpMessageConverter}.
	 */
	protected abstract void beforeBodyWriteInternal(MappingJacksonValue bodyContainer, MediaType contentType,
			MethodParameter returnType, ServerHttpRequest request, ServerHttpResponse response);

}
