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

package org.springframework.messaging.rsocket;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.util.ByteBufPayload;
import io.rsocket.util.DefaultPayload;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;

/**
 * Static utility methods to create {@link Payload} from {@link DataBuffer}s
 * and vice versa.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public abstract class PayloadUtils {

	/**
	 * Use this method to slice, retain and wrap the data portion of the
	 * {@code Payload}, and also to release the {@code Payload}. This assumes
	 * the Payload metadata has been read by now and ensures downstream code
	 * need only be aware of {@code DataBuffer}s.
	 * @param payload the payload to process
	 * @param bufferFactory the DataBufferFactory to wrap with
	 * @return the created {@code DataBuffer} instance
	 */
	public static DataBuffer retainDataAndReleasePayload(Payload payload, DataBufferFactory bufferFactory) {
		try {
			if (bufferFactory instanceof NettyDataBufferFactory) {
				ByteBuf byteBuf = payload.sliceData().retain();
				return ((NettyDataBufferFactory) bufferFactory).wrap(byteBuf);
			}
			else {
				return bufferFactory.wrap(payload.getData());
			}
		}
		finally {
			if (payload.refCnt() > 0) {
				payload.release();
			}
		}
	}

	/**
	 * Create a Payload from the given metadata and data.
	 * <p>If at least one is {@link NettyDataBuffer} then {@link ByteBufPayload}
	 * is created with either obtaining the underlying native {@link ByteBuf}
	 * or using {@link Unpooled#wrappedBuffer(ByteBuffer...)} if necessary.
	 * Otherwise, if both are {@link DefaultDataBuffer}, then
	 * {@link DefaultPayload} is created.
	 * @param data the data part for the payload
	 * @param metadata the metadata part for the payload
	 * @return the created payload
	 */
	public static Payload createPayload(DataBuffer data, DataBuffer metadata) {
		return data instanceof NettyDataBuffer || metadata instanceof NettyDataBuffer ?
				ByteBufPayload.create(asByteBuf(data), asByteBuf(metadata)) :
				DefaultPayload.create(asByteBuffer(data), asByteBuffer(metadata));
	}

	/**
	 * Create a Payload with data only. The created payload is
	 * {@link ByteBufPayload} if the input is {@link NettyDataBuffer} or
	 * otherwise it is {@link DefaultPayload}.
	 * @param data the data part for the payload
	 * @return created payload
	 */
	public static Payload createPayload(DataBuffer data) {
		return data instanceof NettyDataBuffer ?
				ByteBufPayload.create(asByteBuf(data)) : DefaultPayload.create(asByteBuffer(data));
	}


	static ByteBuf asByteBuf(DataBuffer buffer) {
		return buffer instanceof NettyDataBuffer ?
				((NettyDataBuffer) buffer).getNativeBuffer() : Unpooled.wrappedBuffer(buffer.asByteBuffer());
	}

	private static ByteBuffer asByteBuffer(DataBuffer buffer) {
		return buffer instanceof DefaultDataBuffer ?
				((DefaultDataBuffer) buffer).getNativeBuffer() : buffer.asByteBuffer();
	}

}
