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

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.reactive.HttpMessageWriter;
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
 * @since 5.0
 */
public class ResponseEntityResultHandler extends AbstractMessageWriterResultHandler
		implements HandlerResultHandler {

	/**
	 * Constructor with message converters and a {@code ConversionService} only
	 * and creating a {@link HeaderContentTypeResolver}, i.e. using Accept header
	 * to determine the requested content type.
	 *
	 * @param messageWriters writers for serializing to the response body stream
	 * @param conversionService for converting to Flux and Mono from other reactive types
	 */
	public ResponseEntityResultHandler(List<HttpMessageWriter<?>> messageWriters,
			ConversionService conversionService) {

		this(messageWriters, conversionService, new HeaderContentTypeResolver());
	}

	/**
	 * Constructor with message converters, a {@code ConversionService}, and a
	 * {@code RequestedContentTypeResolver}.
	 *
	 * @param messageWriters writers for serializing to the response body stream
	 * @param conversionService for converting other reactive types (e.g.
	 * rx.Observable, rx.Single, etc.) to Flux or Mono
	 * @param contentTypeResolver for resolving the requested content type
	 */
	public ResponseEntityResultHandler(List<HttpMessageWriter<?>> messageWriters,
			ConversionService conversionService, RequestedContentTypeResolver contentTypeResolver) {

		super(messageWriters, conversionService, contentTypeResolver);
		setOrder(0);
	}


	@Override
	public boolean supports(HandlerResult result) {
		ResolvableType returnType = result.getReturnType();
		if (isSupportedType(returnType)) {
			return true;
		}
		else if (getConversionService().canConvert(returnType.getRawClass(), Mono.class)) {
			ResolvableType genericType = result.getReturnType().getGeneric(0);
			return isSupportedType(genericType);
		}
		return false;
	}

	private boolean isSupportedType(ResolvableType returnType) {
		Class<?> clazz = returnType.getRawClass();
		return (HttpEntity.class.isAssignableFrom(clazz) && !RequestEntity.class.isAssignableFrom(clazz));
	}


	@Override
	public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {

		ResolvableType returnType = result.getReturnType();

		ResolvableType bodyType;
		MethodParameter bodyTypeParameter;

		Mono<?> returnValueMono;
		Optional<Object> optional = result.getReturnValue();

		if (optional.isPresent() && getConversionService().canConvert(returnType.getRawClass(), Mono.class)) {
			returnValueMono = getConversionService().convert(optional.get(), Mono.class);
			bodyType = returnType.getGeneric(0, 0);
			bodyTypeParameter = new MethodParameter(result.getReturnTypeSource());
			bodyTypeParameter.increaseNestingLevel();
			bodyTypeParameter.increaseNestingLevel();
		}
		else {
			returnValueMono = Mono.justOrEmpty(optional);
			bodyType = returnType.getGeneric(0);
			bodyTypeParameter = new MethodParameter(result.getReturnTypeSource());
			bodyTypeParameter.increaseNestingLevel();
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

			return writeBody(exchange, httpEntity.getBody(), bodyType, bodyTypeParameter);
		});
	}

}
