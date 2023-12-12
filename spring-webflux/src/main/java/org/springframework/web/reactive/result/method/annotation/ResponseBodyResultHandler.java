/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@code HandlerResultHandler} that handles return values from methods annotated
 * with {@code @ResponseBody} writing to the body of the request or response with
 * an {@link HttpMessageWriter}.
 *
 * <p>By default the order for this result handler is set to 100. As it detects
 * the presence of {@code @ResponseBody} it should be ordered after result
 * handlers that look for a specific return type. Note however that this handler
 * does recognize and explicitly ignores the {@code ResponseEntity} return type.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @since 5.0
 */
public class ResponseBodyResultHandler extends AbstractMessageWriterResultHandler implements HandlerResultHandler {

	/**
	 * Basic constructor with a default {@link ReactiveAdapterRegistry}.
	 * @param writers the writers for serializing to the response body
	 * @param resolver to determine the requested content type
	 */
	public ResponseBodyResultHandler(List<HttpMessageWriter<?>> writers, RequestedContentTypeResolver resolver) {
		this(writers, resolver, ReactiveAdapterRegistry.getSharedInstance());
	}

	/**
	 * Constructor with an {@link ReactiveAdapterRegistry} instance.
	 * @param writers the writers for serializing to the response body
	 * @param resolver to determine the requested content type
	 * @param registry for adaptation to reactive types
	 */
	public ResponseBodyResultHandler(List<HttpMessageWriter<?>> writers,
			RequestedContentTypeResolver resolver, ReactiveAdapterRegistry registry) {

		this(writers, resolver, registry, Collections.emptyList());
	}

	/**
	 * Variant of
	 * {@link #ResponseBodyResultHandler(List, RequestedContentTypeResolver, ReactiveAdapterRegistry)}
	 * with additional list of {@link ErrorResponse.Interceptor}s for return
	 * value handling.
	 * @since 6.2
	 */
	public ResponseBodyResultHandler(List<HttpMessageWriter<?>> writers,
			RequestedContentTypeResolver resolver, ReactiveAdapterRegistry registry,
			List<ErrorResponse.Interceptor> interceptors) {

		super(writers, resolver, registry, interceptors);
		setOrder(100);
	}


	@Override
	public boolean supports(HandlerResult result) {
		MethodParameter returnType = result.getReturnTypeSource();
		Class<?> containingClass = returnType.getContainingClass();
		return (AnnotatedElementUtils.hasAnnotation(containingClass, ResponseBody.class) ||
				returnType.hasMethodAnnotation(ResponseBody.class));
	}

	@Override
	public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
		Object body = result.getReturnValue();
		MethodParameter bodyTypeParameter = result.getReturnTypeSource();
		if (body instanceof ProblemDetail detail) {
			exchange.getResponse().setStatusCode(HttpStatusCode.valueOf(detail.getStatus()));
			if (detail.getInstance() == null) {
				URI path = URI.create(exchange.getRequest().getPath().value());
				detail.setInstance(path);
			}
			invokeErrorResponseInterceptors(detail, null);
		}
		return writeBody(body, bodyTypeParameter, exchange);
	}

}
