/*
 * Copyright 2002-2019 the original author or authors.
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
import org.springframework.lang.Nullable;
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
 * @author Sebastien Deleuze
 * @since 5.0
 */
public abstract class AbstractMessageWriterResultHandler extends HandlerResultHandlerSupport {

	private final List<HttpMessageWriter<?>> messageWriters;


	/**
	 * Constructor with {@link HttpMessageWriter}s and a
	 * {@code RequestedContentTypeResolver}.
	 * @param messageWriters for serializing Objects to the response body stream
	 * @param contentTypeResolver for resolving the requested content type
	 */
	protected AbstractMessageWriterResultHandler(List<HttpMessageWriter<?>> messageWriters,
			RequestedContentTypeResolver contentTypeResolver) {

		this(messageWriters, contentTypeResolver, ReactiveAdapterRegistry.getSharedInstance());
	}

	/**
	 * Constructor with an additional {@link ReactiveAdapterRegistry}.
	 * @param messageWriters for serializing Objects to the response body stream
	 * @param contentTypeResolver for resolving the requested content type
	 * @param adapterRegistry for adapting other reactive types (e.g. rx.Observable,
	 * rx.Single, etc.) to Flux or Mono
	 */
	protected AbstractMessageWriterResultHandler(List<HttpMessageWriter<?>> messageWriters,
			RequestedContentTypeResolver contentTypeResolver, ReactiveAdapterRegistry adapterRegistry) {

		super(contentTypeResolver, adapterRegistry);
		Assert.notEmpty(messageWriters, "At least one message writer is required");
		this.messageWriters = messageWriters;
	}


	/**
	 * Return the configured message converters.
	 */
	public List<HttpMessageWriter<?>> getMessageWriters() {
		return this.messageWriters;
	}


	/**
	 * Write a given body to the response with {@link HttpMessageWriter}.
	 * @param body the object to write
	 * @param bodyParameter the {@link MethodParameter} of the body to write
	 * @param exchange the current exchange
	 * @return indicates completion or error
	 * @see #writeBody(Object, MethodParameter, MethodParameter, ServerWebExchange)
	 */
	protected Mono<Void> writeBody(@Nullable Object body, MethodParameter bodyParameter, ServerWebExchange exchange) {
		return this.writeBody(body, bodyParameter, null, exchange);
	}

	/**
	 * Write a given body to the response with {@link HttpMessageWriter}.
	 * @param body the object to write
	 * @param bodyParameter the {@link MethodParameter} of the body to write
	 * @param actualParam the actual return type of the method that returned the value;
	 * could be different from {@code bodyParameter} when processing {@code HttpEntity}
	 * for example
	 * @param exchange the current exchange
	 * @return indicates completion or error
	 * @since 5.0.2
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	protected Mono<Void> writeBody(@Nullable Object body, MethodParameter bodyParameter,
			@Nullable MethodParameter actualParam, ServerWebExchange exchange) {

		ResolvableType bodyType = ResolvableType.forMethodParameter(bodyParameter);
		ResolvableType actualType = (actualParam != null ? ResolvableType.forMethodParameter(actualParam) : bodyType);
		Class<?> bodyClass = bodyType.resolve();
		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(bodyClass, body);

		Publisher<?> publisher;
		ResolvableType elementType;
		if (adapter != null) {
			publisher = adapter.toPublisher(body);
			ResolvableType genericType = bodyType.getGeneric();
			elementType = getElementType(adapter, genericType);
		}
		else {
			publisher = Mono.justOrEmpty(body);
			elementType = ((bodyClass == null || bodyClass == Object.class) && body != null ?
					ResolvableType.forInstance(body) : bodyType);
		}

		if (elementType.getRawClass() == void.class || elementType.getRawClass() == Void.class) {
			return Mono.from((Publisher<Void>) publisher);
		}

		MediaType bestMediaType = selectMediaType(exchange, () -> getMediaTypesFor(elementType));
		if (bestMediaType != null) {
			for (HttpMessageWriter<?> writer : getMessageWriters()) {
				if (writer.canWrite(elementType, bestMediaType)) {
					return writer.write((Publisher) publisher, actualType, elementType,
							bestMediaType, exchange.getRequest(), exchange.getResponse(),
							Collections.emptyMap());
				}
			}
		}

		List<MediaType> mediaTypes = getMediaTypesFor(elementType);
		if (bestMediaType == null && mediaTypes.isEmpty()) {
			return Mono.error(new IllegalStateException("No HttpMessageWriter for " + elementType));
		}
		return Mono.error(new NotAcceptableStatusException(mediaTypes));
	}

	private ResolvableType getElementType(ReactiveAdapter adapter, ResolvableType genericType) {
		if (adapter.isNoValue()) {
			return ResolvableType.forClass(Void.class);
		}
		else if (genericType != ResolvableType.NONE) {
			return genericType;
		}
		else {
			return ResolvableType.forClass(Object.class);
		}
	}

	private List<MediaType> getMediaTypesFor(ResolvableType elementType) {
		return getMessageWriters().stream()
				.filter(converter -> converter.canWrite(elementType, null))
				.flatMap(converter -> converter.getWritableMediaTypes().stream())
				.collect(Collectors.toList());
	}

}
