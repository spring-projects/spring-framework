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

import java.util.List;
import java.util.Map;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;

/**
 * {@link HttpMessageWriter} wrapper to extend that implements {@link ServerHttpMessageWriter} in order
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
	public Mono<Void> write(Publisher<? extends T> inputStream, ResolvableType elementType, MediaType mediaType, ReactiveHttpOutputMessage outputMessage, Map<String, Object> hints) {
		return this.writer.write(inputStream, elementType, mediaType, outputMessage, hints);
	}

	@Override
	public List<MediaType> getWritableMediaTypes() {
		return this.writer.getWritableMediaTypes();
	}

}
