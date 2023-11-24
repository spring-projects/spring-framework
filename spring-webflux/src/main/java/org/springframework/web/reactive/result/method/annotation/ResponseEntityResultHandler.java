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

package org.springframework.web.reactive.result.method.annotation;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import reactor.core.publisher.Mono;

import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.ErrorResponse;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Handles return values of type {@link HttpEntity}, {@link ResponseEntity},
 * {@link HttpHeaders}, {@link ErrorResponse}, and {@link ProblemDetail}.
 *
 * <p>By default the order for this result handler is set to 0. It is generally
 * safe to place it early in the order as it looks for a concrete return type.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ResponseEntityResultHandler extends AbstractMessageWriterResultHandler implements HandlerResultHandler {

	private static final Set<HttpMethod> SAFE_METHODS = Set.of(HttpMethod.GET, HttpMethod.HEAD);


	/**
	 * Basic constructor with a default {@link ReactiveAdapterRegistry}.
	 * @param writers the writers for serializing to the response body
	 * @param resolver to determine the requested content type
	 */
	public ResponseEntityResultHandler(List<HttpMessageWriter<?>> writers,
			RequestedContentTypeResolver resolver) {

		this(writers, resolver, ReactiveAdapterRegistry.getSharedInstance());
	}

	/**
	 * Constructor with an {@link ReactiveAdapterRegistry} instance.
	 * @param writers the writers for serializing to the response body
	 * @param resolver to determine the requested content type
	 * @param registry for adaptation to reactive types
	 */
	public ResponseEntityResultHandler(List<HttpMessageWriter<?>> writers,
			RequestedContentTypeResolver resolver, ReactiveAdapterRegistry registry) {

		super(writers, resolver, registry);
		setOrder(0);
	}


	@Override
	public boolean supports(HandlerResult result) {
		Class<?> valueType = resolveReturnValueType(result);
		if (isSupportedType(valueType)) {
			return true;
		}
		ReactiveAdapter adapter = getAdapter(result);
		return adapter != null && !adapter.isNoValue() &&
				isSupportedType(result.getReturnType().getGeneric().toClass());
	}

	@Nullable
	private static Class<?> resolveReturnValueType(HandlerResult result) {
		Class<?> valueType = result.getReturnType().toClass();
		Object value = result.getReturnValue();
		if (valueType == Object.class && value != null) {
			valueType = value.getClass();
		}
		return valueType;
	}

	private boolean isSupportedType(@Nullable Class<?> type) {
		if (type == null) {
			return false;
		}
		return ((HttpEntity.class.isAssignableFrom(type) && !RequestEntity.class.isAssignableFrom(type)) ||
				ErrorResponse.class.isAssignableFrom(type) || ProblemDetail.class.isAssignableFrom(type) ||
				HttpHeaders.class.isAssignableFrom(type));
	}


	@Override
	@SuppressWarnings("ConstantConditions")
	public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {

		Mono<?> returnValueMono;
		MethodParameter bodyParameter;
		ReactiveAdapter adapter = getAdapter(result);
		MethodParameter actualParameter = result.getReturnTypeSource();

		if (adapter != null) {
			Assert.isTrue(!adapter.isMultiValue(), "Only a single ResponseEntity supported");
			returnValueMono = Mono.from(adapter.toPublisher(result.getReturnValue()));
			boolean isContinuation = (KotlinDetector.isSuspendingFunction(actualParameter.getMethod()) &&
					!COROUTINES_FLOW_CLASS_NAME.equals(actualParameter.getParameterType().getName()));
			bodyParameter = (isContinuation ? actualParameter.nested() : actualParameter.nested().nested());
		}
		else {
			returnValueMono = Mono.justOrEmpty(result.getReturnValue());
			bodyParameter = actualParameter.nested();
		}

		return returnValueMono.flatMap(returnValue -> {
			HttpEntity<?> httpEntity;
			if (returnValue instanceof HttpEntity<?> he) {
				httpEntity = he;
			}
			else if (returnValue instanceof ErrorResponse response) {
				httpEntity = new ResponseEntity<>(response.getBody(), response.getHeaders(), response.getStatusCode());
			}
			else if (returnValue instanceof ProblemDetail detail) {
				httpEntity = ResponseEntity.of(detail).build();
			}
			else if (returnValue instanceof HttpHeaders headers) {
				httpEntity = new ResponseEntity<>(headers, HttpStatus.OK);
			}
			else {
				return Mono.error(() -> new IllegalArgumentException(
						"HttpEntity or HttpHeaders expected but got: " + returnValue.getClass()));
			}

			if (httpEntity.getBody() instanceof ProblemDetail detail) {
				if (detail.getInstance() == null) {
					URI path = URI.create(exchange.getRequest().getPath().value());
					detail.setInstance(path);
				}
				if (logger.isWarnEnabled() && httpEntity instanceof ResponseEntity<?> responseEntity) {
					if (responseEntity.getStatusCode().value() != detail.getStatus()) {
						logger.warn(actualParameter.getExecutable().toGenericString() +
								" returned ResponseEntity: " + responseEntity + ", but its status" +
								" doesn't match the ProblemDetail status: " + detail.getStatus());
					}
				}
			}

			if (httpEntity instanceof ResponseEntity<?> responseEntity) {
				exchange.getResponse().setStatusCode(responseEntity.getStatusCode());
			}

			HttpHeaders entityHeaders = httpEntity.getHeaders();
			HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
			if (!entityHeaders.isEmpty()) {
				responseHeaders.putAll(entityHeaders);
			}

			if (httpEntity.getBody() == null || returnValue instanceof HttpHeaders) {
				return exchange.getResponse().setComplete();
			}

			String etag = entityHeaders.getETag();
			Instant lastModified = Instant.ofEpochMilli(entityHeaders.getLastModified());
			HttpMethod httpMethod = exchange.getRequest().getMethod();
			return ((SAFE_METHODS.contains(httpMethod) && exchange.checkNotModified(etag, lastModified)) ?
					exchange.getResponse().setComplete() :
					writeBody(httpEntity.getBody(), bodyParameter, actualParameter, exchange));
		});
	}

}
