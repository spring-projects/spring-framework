/*
 * Copyright 2002-2020 the original author or authors.
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

import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Encoder for {@link ByteBuf ByteBufs}.
 *
 * @author Vladislav Kisel
 * @since 5.3
 */
public class ByteBufEncoder extends AbstractEncoder<ByteBuf> {

	public ByteBufEncoder() {
		super(MimeTypeUtils.ALL);
	}


	@Override
	public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
		Class<?> clazz = elementType.toClass();
		return super.canEncode(elementType, mimeType) && ByteBuf.class.isAssignableFrom(clazz);
	}

	@Override
	public Flux<DataBuffer> encode(Publisher<? extends ByteBuf> inputStream,
			DataBufferFactory bufferFactory, ResolvableType elementType, @Nullable MimeType mimeType,
			@Nullable Map<String, Object> hints) {

		return Flux.from(inputStream).map(byteBuffer ->
				encodeValue(byteBuffer, bufferFactory, elementType, mimeType, hints));
	}

	@Override
	public DataBuffer encodeValue(ByteBuf byteBuf, DataBufferFactory bufferFactory,
			ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		DataBuffer dataBuffer;
		if (bufferFactory instanceof NettyDataBufferFactory) {
			dataBuffer = ((NettyDataBufferFactory) bufferFactory).wrap(byteBuf);
		} else {
			dataBuffer = bufferFactory.wrap(byteBuf.nioBuffer());
		}

		if (logger.isDebugEnabled() && !Hints.isLoggingSuppressed(hints)) {
			String logPrefix = Hints.getLogPrefix(hints);
			logger.debug(logPrefix + "Writing " + dataBuffer.readableByteCount() + " bytes");
		}
		return dataBuffer;
	}

}
