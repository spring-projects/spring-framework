/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.reactive.web.dispatch.method.annotation;

import java.nio.ByteBuffer;
import java.util.List;

import org.reactivestreams.Publisher;
import reactor.Publishers;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.ReactiveServerHttpRequest;
import org.springframework.reactive.codec.decoder.Decoder;
import org.springframework.reactive.web.dispatch.method.HandlerMethodArgumentResolver;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author Sebastien Deleuze
 * @author Stephane Maldini
 */
public class RequestBodyArgumentResolver implements HandlerMethodArgumentResolver {

	private final List<Decoder<?>> deserializers;

	private final ConversionService conversionService;


	public RequestBodyArgumentResolver(List<Decoder<?>> deserializers, ConversionService service) {
		Assert.notEmpty(deserializers, "At least one deserializer is required.");
		Assert.notNull(service, "'conversionService' is required.");
		this.deserializers = deserializers;
		this.conversionService = service;
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(RequestBody.class);
	}

	@Override
	public Publisher<Object> resolveArgument(MethodParameter parameter, ReactiveServerHttpRequest request) {
		MediaType mediaType = resolveMediaType(request);
		ResolvableType type = ResolvableType.forMethodParameter(parameter);
		Publisher<ByteBuffer> inputStream = request.getBody();
		Publisher<?> elementStream = inputStream;
		ResolvableType elementType = type.hasGenerics() ? type.getGeneric(0) : type;
		Decoder<?> deserializer = resolveDeserializer(elementType, mediaType);
		if (deserializer != null) {
			elementStream = deserializer.decode(inputStream, elementType, mediaType);
		}
		if (this.conversionService.canConvert(Publisher.class, type.getRawClass())) {
			return Publishers.just(this.conversionService.convert(elementStream, type.getRawClass()));
		}
		return Publishers.map(elementStream, element -> element);
	}

	private MediaType resolveMediaType(ReactiveServerHttpRequest request) {
		String acceptHeader = request.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
		List<MediaType> mediaTypes = MediaType.parseMediaTypes(acceptHeader);
		MediaType.sortBySpecificityAndQuality(mediaTypes);
		return ( mediaTypes.size() > 0 ? mediaTypes.get(0) : MediaType.TEXT_PLAIN);
	}

	private Decoder<?> resolveDeserializer(ResolvableType type, MediaType mediaType, Object... hints) {
		for (Decoder<?> deserializer : this.deserializers) {
			if (deserializer.canDecode(type, mediaType, hints)) {
				return deserializer;
			}
		}
		return null;
	}

}
