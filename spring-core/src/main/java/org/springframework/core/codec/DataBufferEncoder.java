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

package org.springframework.core.codec;

import java.util.Map;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * Simple pass-through encoder for {@link DataBuffer DataBuffers}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public class DataBufferEncoder extends AbstractEncoder<DataBuffer> {

	public DataBufferEncoder() {
		super(MimeTypeUtils.ALL);
	}


	@Override
	public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
		Class<?> clazz = elementType.toClass();
		return super.canEncode(elementType, mimeType) && DataBuffer.class.isAssignableFrom(clazz);
	}

	@Override
	public Flux<DataBuffer> encode(Publisher<? extends DataBuffer> inputStream,
			DataBufferFactory bufferFactory, ResolvableType elementType, @Nullable MimeType mimeType,
			@Nullable Map<String, Object> hints) {

		Flux<DataBuffer> flux = Flux.from(inputStream);
		if (logger.isDebugEnabled() && !Hints.isLoggingSuppressed(hints)) {
			flux = flux.doOnNext(buffer -> logValue(buffer, hints));
		}
		return flux;
	}

	@Override
	public DataBuffer encodeValue(DataBuffer buffer, DataBufferFactory bufferFactory,
			ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		if (logger.isDebugEnabled() && !Hints.isLoggingSuppressed(hints)) {
			logValue(buffer, hints);
		}
		return buffer;
	}

	private void logValue(DataBuffer buffer, @Nullable Map<String, Object> hints) {
		String logPrefix = Hints.getLogPrefix(hints);
		logger.debug(logPrefix + "Writing " + buffer.readableByteCount() + " bytes");
	}

}
