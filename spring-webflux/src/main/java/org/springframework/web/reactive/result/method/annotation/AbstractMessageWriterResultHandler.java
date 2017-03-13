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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerHttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.result.HandlerResultHandlerSupport;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;

/**
 * Abstract base class for result handlers that handle return values by writing
 * to the response with {@link HttpMessageWriter}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractMessageWriterResultHandler extends HandlerResultHandlerSupport {

	private final List<HttpMessageWriter<?>> messageWriters;


	/**
	 * Constructor with {@link HttpMessageWriter}s and a
	 * {@code RequestedContentTypeResolver}.
	 *
	 * @param messageWriters for serializing Objects to the response body stream
	 * @param contentTypeResolver for resolving the requested content type
	 */
	protected AbstractMessageWriterResultHandler(List<HttpMessageWriter<?>> messageWriters,
			RequestedContentTypeResolver contentTypeResolver) {

		super(contentTypeResolver);
		Assert.notEmpty(messageWriters, "At least one message writer is required.");
		this.messageWriters = messageWriters;
	}

	/**
	 * Constructor with an additional {@link ReactiveAdapterRegistry}.
	 *
	 * @param messageWriters for serializing Objects to the response body stream
	 * @param contentTypeResolver for resolving the requested content type
	 * @param adapterRegistry for adapting other reactive types (e.g. rx.Observable,
	 * rx.Single, etc.) to Flux or Mono
	 */
	protected AbstractMessageWriterResultHandler(List<HttpMessageWriter<?>> messageWriters,
			RequestedContentTypeResolver contentTypeResolver,
			ReactiveAdapterRegistry adapterRegistry) {

		super(contentTypeResolver, adapterRegistry);
		Assert.notEmpty(messageWriters, "At least one message writer is required.");
		this.messageWriters = messageWriters;
	}


	/**
	 * Return the configured message converters.
	 */
	public List<HttpMessageWriter<?>> getMessageWriters() {
		return this.messageWriters;
	}


	@SuppressWarnings("unchecked")
	protected Mono<Void> writeBody(Object body, MethodParameter bodyParameter, ServerWebExchange exchange) {

		ResolvableType bodyType = ResolvableType.forMethodParameter(bodyParameter);
		Class<?> bodyClass = bodyType.resolve();
		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(bodyClass, body);

		Publisher<?> publisher;
		ResolvableType elementType;
		if (adapter != null) {
			publisher = adapter.toPublisher(body);
			elementType = adapter.isNoValue() ? ResolvableType.forClass(Void.class) : bodyType.getGeneric(0);
		}
		else {
			publisher = Mono.justOrEmpty(body);
			elementType = (bodyClass == null && body != null ? ResolvableType.forInstance(body) : bodyType);
		}

		if (void.class == elementType.getRawClass() || Void.class == elementType.getRawClass()) {
			return Mono.from((Publisher<Void>) publisher);
		}

		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();
		MediaType bestMediaType = selectMediaType(exchange, () -> getProducibleMediaTypes(elementType));
		if (bestMediaType != null) {
			for (HttpMessageWriter<?> messageWriter : getMessageWriters()) {
				if (messageWriter.canWrite(elementType, bestMediaType)) {
					return (messageWriter instanceof ServerHttpMessageWriter ?
							((ServerHttpMessageWriter<?>) messageWriter).write((Publisher) publisher,
									bodyType, elementType, bestMediaType, request, response, Collections.emptyMap()) :
							messageWriter.write((Publisher) publisher, elementType,
									bestMediaType, response, Collections.emptyMap()));
				}
			}
		}
		else {
			if (getProducibleMediaTypes(elementType).isEmpty()) {
				return Mono.error(new IllegalStateException(
						"No converter for return value type: " + elementType));
			}
		}

		return Mono.error(new NotAcceptableStatusException(getProducibleMediaTypes(elementType)));
	}

	private List<MediaType> getProducibleMediaTypes(ResolvableType elementType) {
		return getMessageWriters().stream()
				.filter(converter -> converter.canWrite(elementType, null))
				.flatMap(converter -> converter.getWritableMediaTypes().stream())
				.collect(Collectors.toList());
	}

}
