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

package org.springframework.web.reactive.method.annotation;

import java.util.List;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.WebServerExchange;

/**
 * @author Sebastien Deleuze
 * @author Stephane Maldini
 */
public class RequestBodyArgumentResolver implements HandlerMethodArgumentResolver {

	private final List<Decoder<?>> decoders;

	private final ConversionService conversionService;


	public RequestBodyArgumentResolver(List<Decoder<?>> decoders, ConversionService service) {
		Assert.notEmpty(decoders, "At least one decoder is required.");
		Assert.notNull(service, "'conversionService' is required.");
		this.decoders = decoders;
		this.conversionService = service;
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(RequestBody.class);
	}

	@Override
	public Mono<Object> resolveArgument(MethodParameter parameter, WebServerExchange exchange) {
		MediaType mediaType = exchange.getRequest().getHeaders().getContentType();
		if (mediaType == null) {
			mediaType = MediaType.APPLICATION_OCTET_STREAM;
		}
		ResolvableType type = ResolvableType.forMethodParameter(parameter);
		Flux<DataBuffer> body = exchange.getRequest().getBody();
		Flux<?> elementFlux = body;
		ResolvableType elementType = type.hasGenerics() ? type.getGeneric(0) : type;

		Decoder<?> decoder = resolveDecoder(elementType, mediaType);
		if (decoder != null) {
			elementFlux = decoder.decode(body, elementType, mediaType);
		}

		if (this.conversionService.canConvert(Publisher.class, type.getRawClass())) {
			return Mono.just(this.conversionService.convert(elementFlux, type.getRawClass()));
		}
		else if (type.getRawClass() == Flux.class) {
			return Mono.just(elementFlux);
		}
		else if (type.getRawClass() == Mono.class) {
			return Mono.just(Mono.from(elementFlux));
		}

		// TODO Currently manage only "Foo" parameter, not "List<Foo>" parameters, Stéphane is going to add toIterable/toIterator to Flux to support that use case
		return elementFlux.next().map(o -> o);
	}

	private Decoder<?> resolveDecoder(ResolvableType type, MediaType mediaType, Object... hints) {
		for (Decoder<?> decoder : this.decoders) {
			if (decoder.canDecode(type, mediaType, hints)) {
				return decoder;
			}
		}
		return null;
	}

}
