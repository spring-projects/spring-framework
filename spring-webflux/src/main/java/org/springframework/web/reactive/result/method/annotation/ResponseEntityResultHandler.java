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
package org.springframework.web.reactive.result.method.annotation;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerHttpMessageWriter;
import org.springframework.util.Assert;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Handles {@link HttpEntity} and {@link ResponseEntity} return values.
 *
 * <p>By default the order for this result handler is set to 0. It is generally
 * safe to place it early in the order as it looks for a concrete return type.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ResponseEntityResultHandler extends AbstractMessageWriterResultHandler
		implements HandlerResultHandler {

	private static final List<HttpMethod> SAFE_METHODS = Arrays.asList(HttpMethod.GET, HttpMethod.HEAD);


	/**
	 * Basic constructor with a default {@link ReactiveAdapterRegistry}.
	 * @param writers writers for serializing to the response body
	 * @param resolver to determine the requested content type
	 */
	public ResponseEntityResultHandler(List<ServerHttpMessageWriter<?>> writers,
			RequestedContentTypeResolver resolver) {

		this(writers, resolver, new ReactiveAdapterRegistry());
	}

	/**
	 * Constructor with an {@link ReactiveAdapterRegistry} instance.
	 * @param writers writers for serializing to the response body
	 * @param resolver to determine the requested content type
	 * @param registry for adaptation to reactive types
	 */
	public ResponseEntityResultHandler(List<ServerHttpMessageWriter<?>> writers,
			RequestedContentTypeResolver resolver, ReactiveAdapterRegistry registry) {

		super(writers, resolver, registry);
		setOrder(0);
	}


	@Override
	public boolean supports(HandlerResult result) {
		if (isSupportedType(result.getReturnType())) {
			return true;
		}
		ReactiveAdapter adapter = getAdapter(result);
		return adapter != null && !adapter.isNoValue() &&
				isSupportedType(result.getReturnType().getGeneric(0));
	}

	private boolean isSupportedType(ResolvableType type) {
		Class<?> clazz = type.getRawClass();
		return (HttpEntity.class.isAssignableFrom(clazz) && !RequestEntity.class.isAssignableFrom(clazz));
	}


	@Override
	public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {

		Mono<?> returnValueMono;
		MethodParameter bodyParameter;
		ReactiveAdapter adapter = getAdapter(result);

		if (adapter != null) {
			Assert.isTrue(!adapter.isMultiValue(), "Only a single ResponseEntity supported");
			returnValueMono = Mono.from(adapter.toPublisher(result.getReturnValue()));
			bodyParameter = result.getReturnTypeSource().nested().nested();
		}
		else {
			returnValueMono = Mono.justOrEmpty(result.getReturnValue());
			bodyParameter = result.getReturnTypeSource().nested();
		}

		return returnValueMono.then(returnValue -> {
			Assert.isInstanceOf(HttpEntity.class, returnValue, "HttpEntity expected");
			HttpEntity<?> httpEntity = (HttpEntity<?>) returnValue;

			if (httpEntity instanceof ResponseEntity) {
				ResponseEntity<?> responseEntity = (ResponseEntity<?>) httpEntity;
				exchange.getResponse().setStatusCode(responseEntity.getStatusCode());
			}

			HttpHeaders entityHeaders = httpEntity.getHeaders();
			HttpHeaders responseHeaders = exchange.getResponse().getHeaders();

			if (!entityHeaders.isEmpty()) {
				entityHeaders.entrySet().stream()
						.filter(entry -> !responseHeaders.containsKey(entry.getKey()))
						.forEach(entry -> responseHeaders.put(entry.getKey(), entry.getValue()));
			}
			if(httpEntity.getBody() == null) {
				return exchange.getResponse().setComplete();
			}

			String etag = entityHeaders.getETag();
			Instant lastModified = Instant.ofEpochMilli(entityHeaders.getLastModified());
			HttpMethod httpMethod = exchange.getRequest().getMethod();
			if (SAFE_METHODS.contains(httpMethod) && exchange.checkNotModified(etag, lastModified)) {
				return exchange.getResponse().setComplete();
			}

			return writeBody(httpEntity.getBody(), bodyParameter, exchange);
		});
	}

}
