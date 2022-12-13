/*
 * Copyright 2002-2022 the original author or authors.
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

import io.netty5.buffer.Buffer;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.Netty5DataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * Encoder for {@link Buffer Buffers}.
 *
 * @author Violeta Georgieva
 * @since 6.0
 */
public class Netty5BufferEncoder extends AbstractEncoder<Buffer> {

	public Netty5BufferEncoder() {
		super(MimeTypeUtils.ALL);
	}


	@Override
	public boolean canEncode(ResolvableType type, @Nullable MimeType mimeType) {
		Class<?> clazz = type.toClass();
		return super.canEncode(type, mimeType) && Buffer.class.isAssignableFrom(clazz);
	}

	@Override
	public Flux<DataBuffer> encode(Publisher<? extends Buffer> inputStream,
			DataBufferFactory bufferFactory, ResolvableType elementType, @Nullable MimeType mimeType,
			@Nullable Map<String, Object> hints) {

		return Flux.from(inputStream).map(byteBuffer ->
				encodeValue(byteBuffer, bufferFactory, elementType, mimeType, hints));
	}

	@Override
	public DataBuffer encodeValue(Buffer buffer, DataBufferFactory bufferFactory, ResolvableType valueType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		if (logger.isDebugEnabled() && !Hints.isLoggingSuppressed(hints)) {
			String logPrefix = Hints.getLogPrefix(hints);
			logger.debug(logPrefix + "Writing " + buffer.readableBytes() + " bytes");
		}
		if (bufferFactory instanceof Netty5DataBufferFactory netty5DataBufferFactory) {
			return netty5DataBufferFactory.wrap(buffer);
		}
		byte[] bytes = new byte[buffer.readableBytes()];
		buffer.readBytes(bytes, 0, bytes.length);
		buffer.close();
		return bufferFactory.wrap(bytes);
	}
}
