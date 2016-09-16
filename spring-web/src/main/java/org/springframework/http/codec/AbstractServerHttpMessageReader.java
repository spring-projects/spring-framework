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

/**
 * {@link HttpMessageReader} wrapper to extend that implements {@link ServerHttpMessageReader} in order
 * to allow providing hints.
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
	public boolean canRead(ResolvableType elementType, MediaType mediaType, Map<String, Object> hints) {
		return this.reader.canRead(elementType, mediaType, hints);
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
	public final Map<String, Object> resolveReadHints(ResolvableType streamType,
			ResolvableType elementType, MediaType mediaType, ServerHttpRequest request) {

		Map<String, Object> hints = new HashMap<>();
		if (this.reader instanceof ServerHttpMessageReader) {
			hints.putAll(((ServerHttpMessageReader<T>)this.reader).resolveReadHints(streamType, elementType, mediaType, request));
		}
		hints.putAll(resolveReadHintsInternal(streamType, elementType, mediaType, request));
		return hints;
	}

	/**
	 * Abstract method that returns hints which can be used to customize how the body should be read.
	 * Invoked from {@link #resolveReadHints}.
	 * @param streamType the original type used in the method parameter. For annotation
	 * based controllers, the {@link MethodParameter} is available via {@link ResolvableType#getSource()}.
	 * @param elementType the stream element type to return
	 * @param mediaType the media type to read, can be {@code null} if not specified.
	 * Typically the value of a {@code Content-Type} header.
	 * @param request the current HTTP request
	 * @return Additional information about how to read the body
	 */
	protected abstract Map<String, Object> resolveReadHintsInternal(ResolvableType streamType,
			ResolvableType elementType, MediaType mediaType, ServerHttpRequest request);

}
