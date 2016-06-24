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

import java.util.List;
import java.util.Optional;

import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.reactive.HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.reactive.accept.HeaderContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Handles {@link HttpEntity} and {@link ResponseEntity} return values.
 *
 * <p>By default the order for this result handler is set to 0. It is generally
 * safe to place it early in the order as it looks for a concrete return type.
 *
 * @author Rossen Stoyanchev
 */
public class ResponseEntityResultHandler extends AbstractMessageConverterResultHandler
		implements HandlerResultHandler {

	/**
	 * Constructor with message converters and a {@code ConversionService} only
	 * and creating a {@link HeaderContentTypeResolver}, i.e. using Accept header
	 * to determine the requested content type.
	 *
	 * @param converters converters for writing the response body with
	 * @param conversionService for converting to Flux and Mono from other reactive types
	 */
	public ResponseEntityResultHandler(List<HttpMessageConverter<?>> converters,
			ConversionService conversionService) {

		this(converters, conversionService, new HeaderContentTypeResolver());
	}

	/**
	 * Constructor with message converters, a {@code ConversionService}, and a
	 * {@code RequestedContentTypeResolver}.
	 *
	 * @param converters converters for writing the response body with
	 * @param conversionService for converting other reactive types (e.g.
	 * rx.Observable, rx.Single, etc.) to Flux or Mono
	 * @param contentTypeResolver for resolving the requested content type
	 */
	public ResponseEntityResultHandler(List<HttpMessageConverter<?>> converters,
			ConversionService conversionService, RequestedContentTypeResolver contentTypeResolver) {

		super(converters, conversionService, contentTypeResolver);
		setOrder(0);
	}


	@Override
	public boolean supports(HandlerResult result) {
		ResolvableType returnType = result.getReturnValueType();
		return (HttpEntity.class.isAssignableFrom(returnType.getRawClass()) &&
				!RequestEntity.class.isAssignableFrom(returnType.getRawClass()));
	}


	@Override
	public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {

		Object body = null;

		Optional<Object> optional = result.getReturnValue();
		if (optional.isPresent()) {
			Assert.isInstanceOf(HttpEntity.class, optional.get());
			HttpEntity<?> httpEntity = (HttpEntity<?>) optional.get();

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

			body = httpEntity.getBody();
		}

		ResolvableType bodyType = result.getReturnValueType().getGeneric(0);
		return writeBody(exchange, body, bodyType);
	}

}
