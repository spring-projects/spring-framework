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

package org.springframework.core.codec.support;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferAllocator;
import org.springframework.util.MimeType;

/**
 * Abstract base class for {@link org.springframework.core.codec.Encoder} classes that
 * can only deal with a single value.
 * @author Arjen Poutsma
 */
public abstract class AbstractSingleValueEncoder<T> extends AbstractEncoder<T> {

	public AbstractSingleValueEncoder(MimeType... supportedMimeTypes) {
		super(supportedMimeTypes);
	}

	@Override
	public final Flux<DataBuffer> encode(Publisher<? extends T> inputStream,
			DataBufferAllocator allocator, ResolvableType type, MimeType mimeType,
			Object... hints) {
		return Flux.from(inputStream).
				take(1).
				concatMap(t -> {
					try {
						return encode(t, allocator, type, mimeType);
					}
					catch (Exception ex) {
						return Flux.error(ex);
					}
				});
	}

	/**
	 * Encodes {@code T} to an output {@link DataBuffer} stream.
	 * @param t the value to process
	 * @param allocator a buffer allocator used to create the output
	 * @param type the stream element type to process
	 * @param mimeType the mime type to process
	 * @param hints Additional information about how to do decode, optional
	 * @return the output stream
	 * @throws Exception in case of errors
	 */
	protected abstract Flux<DataBuffer> encode(T t, DataBufferAllocator allocator,
			ResolvableType type, MimeType mimeType, Object... hints) throws Exception;


}
