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
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.MediaType;
import org.springframework.http.converter.reactive.HttpMessageConverter;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.result.ContentNegotiatingResultHandlerSupport;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;

/**
 * Abstract base class for result handlers that handle return values by writing
 * to the response with {@link HttpMessageConverter}.
 *
 * @author Rossen Stoyanchev
 */
public abstract class AbstractMessageConverterResultHandler extends ContentNegotiatingResultHandlerSupport {

	private final List<HttpMessageConverter<?>> messageConverters;


	/**
	 * Constructor with message converters, a {@code ConversionService}, and a
	 * {@code RequestedContentTypeResolver}.
	 *
	 * @param converters converters for writing the response body with
	 * @param conversionService for converting other reactive types (e.g.
	 * rx.Observable, rx.Single, etc.) to Flux or Mono
	 * @param contentTypeResolver for resolving the requested content type
	 */
	protected AbstractMessageConverterResultHandler(List<HttpMessageConverter<?>> converters,
			ConversionService conversionService, RequestedContentTypeResolver contentTypeResolver) {

		super(conversionService, contentTypeResolver);
		Assert.notEmpty(converters, "At least one message converter is required.");
		this.messageConverters = converters;
	}

	/**
	 * Return the configured message converters.
	 */
	public List<HttpMessageConverter<?>> getMessageConverters() {
		return this.messageConverters;
	}


	@SuppressWarnings("unchecked")
	protected Mono<Void> writeBody(ServerWebExchange exchange, Object body, ResolvableType bodyType) {

		boolean convertToFlux = getConversionService().canConvert(bodyType.getRawClass(), Flux.class);
		boolean convertToMono = getConversionService().canConvert(bodyType.getRawClass(), Mono.class);

		ResolvableType elementType = convertToFlux || convertToMono ? bodyType.getGeneric(0) : bodyType;

		Publisher<?> publisher;
		if (body == null) {
			publisher = Mono.empty();
		}
		else if (convertToMono) {
			publisher = getConversionService().convert(body, Mono.class);
		}
		else if (convertToFlux) {
			publisher = getConversionService().convert(body, Flux.class);
		}
		else {
			publisher = Mono.just(body);
		}

		if (Void.class.equals(elementType.getRawClass())) {
			return Mono.from((Publisher<Void>) publisher);
		}

		List<MediaType> producibleTypes = getProducibleMediaTypes(elementType);
		if (producibleTypes.isEmpty()) {
			return Mono.error(new IllegalStateException(
					"No converter for return value type: " + elementType));
		}

		MediaType bestMediaType = selectMediaType(exchange, producibleTypes);

		if (bestMediaType != null) {
			for (HttpMessageConverter<?> converter : getMessageConverters()) {
				if (converter.canWrite(elementType, bestMediaType)) {
					ServerHttpResponse response = exchange.getResponse();
					return converter.write((Publisher) publisher, elementType, bestMediaType, response);
				}
			}
		}

		return Mono.error(new NotAcceptableStatusException(producibleTypes));
	}

	private List<MediaType> getProducibleMediaTypes(ResolvableType elementType) {
		return getMessageConverters().stream()
				.filter(converter -> converter.canWrite(elementType, null))
				.flatMap(converter -> converter.getWritableMediaTypes().stream())
				.collect(Collectors.toList());
	}

}
