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

import java.io.IOException;
import java.io.InputStream;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferAllocator;
import org.springframework.core.io.buffer.support.DataBufferUtils;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StreamUtils;

/**
 * An encoder for {@link Resource}s.
 * @author Arjen Poutsma
 */
public class ResourceEncoder extends AbstractEncoder<Resource> {

	public static final int DEFAULT_BUFFER_SIZE = StreamUtils.BUFFER_SIZE;

	private final int bufferSize;

	public ResourceEncoder() {
		this(DEFAULT_BUFFER_SIZE);
	}

	public ResourceEncoder(int bufferSize) {
		super(MimeTypeUtils.ALL);
		Assert.isTrue(bufferSize > 0, "'bufferSize' must be larger than 0");
		this.bufferSize = bufferSize;
	}

	@Override
	public boolean canEncode(ResolvableType type, MimeType mimeType, Object... hints) {
		Class<?> clazz = type.getRawClass();
		return (super.canEncode(type, mimeType, hints) &&
				Resource.class.isAssignableFrom(clazz));
	}

	@Override
	public Flux<DataBuffer> encode(Publisher<? extends Resource> inputStream,
			DataBufferAllocator allocator, ResolvableType type, MimeType mimeType,
			Object... hints) {
		return Flux.from(inputStream).
				concatMap(resource -> {
					try {
						InputStream is = resource.getInputStream();
						return DataBufferUtils.read(is, allocator, this.bufferSize);
					}
					catch (IOException ex) {
						return Mono.error(ex);
					}
				});
	}
}
