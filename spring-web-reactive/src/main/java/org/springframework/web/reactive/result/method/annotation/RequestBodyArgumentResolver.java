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

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.MediaType;
import org.springframework.http.converter.reactive.HttpMessageConverter;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolves method arguments annotated with {@code @RequestBody} by reading and
 * decoding the body of the request through a compatible
 * {@code HttpMessageConverter}.
 *
 * @author Sebastien Deleuze
 * @author Stephane Maldini
 * @author Rossen Stoyanchev
 */
public class RequestBodyArgumentResolver implements HandlerMethodArgumentResolver {

	private final List<HttpMessageConverter<?>> messageConverters;

	private final ConversionService conversionService;


	/**
	 * Constructor with message converters and a ConversionService.
	 * @param converters converters for reading the request body with
	 * @param service for converting to other reactive types from Flux and Mono
	 */
	public RequestBodyArgumentResolver(List<HttpMessageConverter<?>> converters,
			ConversionService service) {

		Assert.notEmpty(converters, "At least one message converter is required.");
		Assert.notNull(service, "'conversionService' is required.");
		this.messageConverters = converters;
		this.conversionService = service;
	}


	/**
	 * Return the configured message converters.
	 */
	public List<HttpMessageConverter<?>> getMessageConverters() {
		return this.messageConverters;
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(RequestBody.class);
	}

	@Override
	public Mono<Object> resolveArgument(MethodParameter parameter, ModelMap model,
			ServerWebExchange exchange) {

		ResolvableType type = ResolvableType.forMethodParameter(parameter);
		ResolvableType elementType = type.hasGenerics() ? type.getGeneric(0) : type;

		MediaType mediaType = exchange.getRequest().getHeaders().getContentType();
		if (mediaType == null) {
			mediaType = MediaType.APPLICATION_OCTET_STREAM;
		}

		Flux<?> elementFlux = exchange.getRequest().getBody();

		HttpMessageConverter<?> converter = getMessageConverter(elementType, mediaType);
		if (converter != null) {
			elementFlux = converter.read(elementType, exchange.getRequest());
		}

		if (type.getRawClass() == Flux.class) {
			return Mono.just(elementFlux);
		}
		else if (type.getRawClass() == Mono.class) {
			return Mono.just(Mono.from(elementFlux));
		}
		else if (this.conversionService.canConvert(Publisher.class, type.getRawClass())) {
			Object target = this.conversionService.convert(elementFlux, type.getRawClass());
			return Mono.just(target);
		}

		// TODO Currently manage only "Foo" parameter, not "List<Foo>" parameters, Stéphane is going to add toIterable/toIterator to Flux to support that use case
		return elementFlux.next().map(o -> o);
	}

	private HttpMessageConverter<?> getMessageConverter(ResolvableType type, MediaType mediaType) {
		for (HttpMessageConverter<?> messageConverter : this.messageConverters) {
			if (messageConverter.canRead(type, mediaType)) {
				return messageConverter;
			}
		}
		return null;
	}

}
