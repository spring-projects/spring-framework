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

package org.springframework.core.codec;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;

import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StreamUtils;

/**
 * Encoder for {@link Resource}s.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public class ResourceEncoder extends AbstractSingleValueEncoder<Resource> {

	public static final int DEFAULT_BUFFER_SIZE = StreamUtils.BUFFER_SIZE;

	private final int bufferSize;


	public ResourceEncoder() {
		this(DEFAULT_BUFFER_SIZE);
	}

	public ResourceEncoder(int bufferSize) {
		super(MimeTypeUtils.APPLICATION_OCTET_STREAM, MimeTypeUtils.ALL);
		Assert.isTrue(bufferSize > 0, "'bufferSize' must be larger than 0");
		this.bufferSize = bufferSize;
	}


	@Override
	public boolean canEncode(ResolvableType elementType, MimeType mimeType) {
		Class<?> clazz = elementType.getRawClass();
		return (super.canEncode(elementType, mimeType) && Resource.class.isAssignableFrom(clazz));
	}

	@Override
	protected Flux<DataBuffer> encode(Resource resource, DataBufferFactory dataBufferFactory,
			ResolvableType type, MimeType mimeType, Map<String, Object> hints) {

		try {
			ReadableByteChannel channel = resource.readableChannel();
			return DataBufferUtils.read(channel, dataBufferFactory, this.bufferSize);
		}
		catch (IOException ex) {
			return Flux.error(ex);
		}
	}

}
