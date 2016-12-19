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

package org.springframework.http.codec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

/**
 * {@link HttpMessageReader} wrapper that implements {@link ServerHttpMessageReader} in order
 * to allow providing hints to the nested {@code reader} or setting the response status for
 * example, by implementing {@link #resolveReadHints(ResolvableType, ResolvableType, ServerHttpRequest)}.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
public abstract class AbstractServerHttpMessageReader<T> implements ServerHttpMessageReader<T> {

	private HttpMessageReader<T> reader;


	public AbstractServerHttpMessageReader(HttpMessageReader<T> reader) {
		this.reader = reader;
	}

	@Override
	public boolean canRead(ResolvableType elementType, MediaType mediaType) {
		return this.reader.canRead(elementType, mediaType);
	}

	@Override
	public Flux<T> read(ResolvableType elementType, ReactiveHttpInputMessage inputMessage, Map<String, Object> hints) {
		return this.reader.read(elementType, inputMessage, hints);
	}

	@Override
	public Mono<T> readMono(ResolvableType elementType, ReactiveHttpInputMessage inputMessage, Map<String, Object> hints) {
		return this.reader.readMono(elementType, inputMessage, hints);
	}

	@Override
	public List<MediaType> getReadableMediaTypes() {
		return this.reader.getReadableMediaTypes();
	}

	@Override
	public Flux<T> read(ResolvableType streamType, ResolvableType elementType,
			ServerHttpRequest request, ServerHttpResponse response, Map<String, Object> hints) {

		Map<String, Object> mergedHints = new HashMap<>(hints);
		mergedHints.putAll(resolveReadHints(streamType, elementType, request));

		return (this.reader instanceof ServerHttpMessageReader ?
				((ServerHttpMessageReader<T>)this.reader).read(streamType, elementType, request, response, mergedHints) :
				this.read(elementType, request, mergedHints));
	}

	@Override
	public Mono<T> readMono(ResolvableType streamType, ResolvableType elementType,
			ServerHttpRequest request, ServerHttpResponse response, Map<String, Object> hints) {

		Map<String, Object> mergedHints = new HashMap<>(hints);
		mergedHints.putAll(resolveReadHints(streamType, elementType, request));

		return (this.reader instanceof ServerHttpMessageReader ?
				((ServerHttpMessageReader<T>)this.reader).readMono(streamType, elementType, request, response, mergedHints) :
				this.readMono(elementType, request, mergedHints));
	}

	/**
	 * Invoked before reading the request to resolve hints by
	 * {@link #read(ResolvableType, ResolvableType, ServerHttpRequest, ServerHttpResponse, Map)}.
	 *
	 * @param streamType the original type used for the method return value. For annotation
	 * based controllers, the {@link MethodParameter} is available via {@link ResolvableType#getSource()}.
	 * @param elementType the stream element type to process
	 * @param request the current HTTP request
	 * @return Additional information about how to write the body
	 */
	protected abstract Map<String, Object> resolveReadHints(ResolvableType streamType,
			ResolvableType elementType, ServerHttpRequest request);

}
