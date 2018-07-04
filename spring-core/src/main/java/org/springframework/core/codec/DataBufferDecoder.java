/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Map;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * Simple pass-through decoder for {@link DataBuffer DataBuffers}.
 * <p><strong>Note</strong> that the "decoded" buffers returned by instances of this class should
 * be released after usage by calling
 * {@link org.springframework.core.io.buffer.DataBufferUtils#release(DataBuffer)}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class DataBufferDecoder extends AbstractDataBufferDecoder<DataBuffer> {


	public DataBufferDecoder() {
		super(MimeTypeUtils.ALL);
	}


	@Override
	public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
		Class<?> clazz = elementType.getRawClass();
		return (super.canDecode(elementType, mimeType) && clazz != null && DataBuffer.class.isAssignableFrom(clazz));
	}

	@Override
	public Flux<DataBuffer> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return Flux.from(inputStream);
	}

	@Override
	protected DataBuffer decodeDataBuffer(DataBuffer buffer, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		if (logger.isDebugEnabled()) {
			logger.debug(Hints.getLogPrefix(hints) + "Read " + buffer.readableByteCount() + " bytes");
		}
		return buffer;
	}

}
