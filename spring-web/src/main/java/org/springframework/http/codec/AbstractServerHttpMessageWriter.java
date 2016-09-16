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

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * {@link HttpMessageWriter} wrapper that implements {@link ServerHttpMessageWriter} in order
 * to allow providing hints.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
public abstract class AbstractServerHttpMessageWriter<T> implements ServerHttpMessageWriter<T> {

	private HttpMessageWriter<T> writer;


	public AbstractServerHttpMessageWriter(HttpMessageWriter<T> writer) {
		this.writer = writer;
	}

	@Override
	public boolean canWrite(ResolvableType elementType, MediaType mediaType, Map<String, Object> hints) {
		return this.writer.canWrite(elementType, mediaType, hints);
	}

	@Override
	public Mono<Void> write(Publisher<? extends T> inputStream, ResolvableType elementType,
			MediaType mediaType, ReactiveHttpOutputMessage outputMessage, Map<String, Object> hints) {

		return this.writer.write(inputStream, elementType, mediaType, outputMessage, hints);
	}

	@Override
	public List<MediaType> getWritableMediaTypes() {
		return this.writer.getWritableMediaTypes();
	}

	@Override
	public final Map<String, Object> resolveWriteHints(ResolvableType streamType,
			ResolvableType elementType, MediaType mediaType, ServerHttpRequest request) {

		Map<String, Object> hints = new HashMap<>();
		if (this.writer instanceof ServerHttpMessageWriter) {
			hints.putAll(((ServerHttpMessageWriter<T>)this.writer).resolveWriteHints(streamType, elementType, mediaType, request));
		}
		hints.putAll(resolveWriteHintsInternal(streamType, elementType, mediaType, request));
		return hints;
	}

	/**
	 * Abstract method that returns hints which can be used to customize how the body should be written.
	 * Invoked from {@link #resolveWriteHints}.
	 * @param streamType the original type used for the method return value. For annotation
	 * based controllers, the {@link MethodParameter} is available via {@link ResolvableType#getSource()}.
	 * @param elementType the stream element type to process
	 * @param mediaType the content type to use when writing. May be {@code null} to
	 * indicate that the default content type of the converter must be used.
	 * @param request the current HTTP request
	 * @return Additional information about how to write the body
	 */
	protected abstract Map<String, Object> resolveWriteHintsInternal(ResolvableType streamType,
			ResolvableType elementType, MediaType mediaType, ServerHttpRequest request);

}
