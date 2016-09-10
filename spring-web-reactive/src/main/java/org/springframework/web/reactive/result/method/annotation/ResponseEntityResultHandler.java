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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
import org.springframework.http.codec.HttpMessageWriter;
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
	 * Constructor with {@link HttpMessageWriter}s and a
	 * {@code RequestedContentTypeResolver}.
	 *
	 * @param messageWriters writers for serializing to the response body stream
	 * @param contentTypeResolver for resolving the requested content type
	 */
	public ResponseEntityResultHandler(List<HttpMessageWriter<?>> messageWriters,
			RequestedContentTypeResolver contentTypeResolver) {

		this(messageWriters, contentTypeResolver, new ReactiveAdapterRegistry(), Collections.emptyList());
	}

	/**
	 * Constructor with an additional {@link ReactiveAdapterRegistry}.
	 *
	 * @param messageWriters writers for serializing to the response body stream
	 * @param contentTypeResolver for resolving the requested content type
	 * @param adapterRegistry for adapting other reactive types (e.g. rx.Observable,
	 * rx.Single, etc.) to Flux or Mono
	 */
	public ResponseEntityResultHandler(List<HttpMessageWriter<?>> messageWriters,
			RequestedContentTypeResolver contentTypeResolver,
			ReactiveAdapterRegistry adapterRegistry) {

		this(messageWriters, contentTypeResolver, adapterRegistry, Collections.emptyList());
	}

	/**
	 * Constructor with additional {@link ReactiveAdapterRegistry} and list of {@link ResponseBodyAdvice}.
	 *
	 * @param messageWriters writers for serializing to the response body stream
	 * @param contentTypeResolver for resolving the requested content type
	 * @param adapterRegistry for adapting other reactive types (e.g. rx.Observable,
	 * rx.Single, etc.) to Flux or Mono
	 * @param bodyAdvice body advice to customize the response
	 */
	public ResponseEntityResultHandler(List<HttpMessageWriter<?>> messageWriters,
			RequestedContentTypeResolver contentTypeResolver,
			ReactiveAdapterRegistry adapterRegistry, List<ResponseBodyAdvice> bodyAdvice) {

		super(messageWriters, contentTypeResolver, adapterRegistry, bodyAdvice);
		setOrder(0);
	}


	@Override
	public boolean supports(HandlerResult result) {
		Class<?> returnType = result.getReturnType().getRawClass();
		if (isSupportedType(returnType)) {
			return true;
		}
		else {
			ReactiveAdapter adapter = getAdapterRegistry().getAdapterFrom(returnType, result.getReturnValue());
			if (adapter != null &&
					!adapter.getDescriptor().isMultiValue() &&
					!adapter.getDescriptor().isNoValue()) {

				ResolvableType genericType = result.getReturnType().getGeneric(0);
				return isSupportedType(genericType.getRawClass());
			}
		}
		return false;
	}

	private boolean isSupportedType(Class<?> clazz) {
		return (HttpEntity.class.isAssignableFrom(clazz) && !RequestEntity.class.isAssignableFrom(clazz));
	}


	@Override
	public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {

		ResolvableType returnType = result.getReturnType();
		MethodParameter bodyType;

		Mono<?> returnValueMono;
		Optional<Object> optionalValue = result.getReturnValue();

		Class<?> rawClass = returnType.getRawClass();
		ReactiveAdapter adapter = getAdapterRegistry().getAdapterFrom(rawClass, optionalValue);

		if (adapter != null) {
			returnValueMono = adapter.toMono(optionalValue);
			bodyType = new MethodParameter(result.getReturnTypeSource());
			bodyType.increaseNestingLevel();
			bodyType.increaseNestingLevel();
		}
		else {
			returnValueMono = Mono.justOrEmpty(optionalValue);
			bodyType = new MethodParameter(result.getReturnTypeSource());
			bodyType.increaseNestingLevel();
		}

		return returnValueMono.then(returnValue -> {

			Assert.isInstanceOf(HttpEntity.class, returnValue);
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
				exchange.getResponse().setComplete();
				return Mono.empty();
			}

			String etag = entityHeaders.getETag();
			Instant lastModified = Instant.ofEpochMilli(entityHeaders.getLastModified());
			HttpMethod httpMethod = exchange.getRequest().getMethod();
			if (SAFE_METHODS.contains(httpMethod) && exchange.checkNotModified(etag, lastModified)) {
				exchange.getResponse().setComplete();
				return Mono.empty();
			}

			return writeBody(httpEntity.getBody(), bodyType, exchange);
		});
	}

}
