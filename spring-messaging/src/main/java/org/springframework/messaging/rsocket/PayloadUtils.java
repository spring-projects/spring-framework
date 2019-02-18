/*
 * Copyright 2002-2019 the original author or authors.
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
package org.springframework.messaging.rsocket;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
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
abstract class PayloadUtils {

	/**
	 * Return the Payload data wrapped as DataBuffer. If the bufferFactory is
	 * {@link NettyDataBufferFactory} the payload retained and sliced.
	 * @param payload the input payload
	 * @param bufferFactory the BufferFactory to use to wrap
	 * @return the DataBuffer wrapper
	 */
	public static DataBuffer asDataBuffer(Payload payload, DataBufferFactory bufferFactory) {
		if (bufferFactory instanceof NettyDataBufferFactory) {
			return ((NettyDataBufferFactory) bufferFactory).wrap(payload.retain().sliceData());
		}
		else {
			return bufferFactory.wrap(payload.getData());
		}
	}

	/**
	 * Create a Payload from the given metadata and data.
	 * @param metadata the metadata part for the payload
	 * @param data the data part for the payload
	 * @return the created Payload
	 */
	public static Payload asPayload(DataBuffer metadata, DataBuffer data) {
		if (metadata instanceof NettyDataBuffer && data instanceof NettyDataBuffer) {
			return ByteBufPayload.create(getByteBuf(data), getByteBuf(metadata));
		}
		else if (metadata instanceof DefaultDataBuffer && data instanceof DefaultDataBuffer) {
			return DefaultPayload.create(getByteBuffer(data), getByteBuffer(metadata));
		}
		else {
			return DefaultPayload.create(data.asByteBuffer(), metadata.asByteBuffer());
		}
	}

	/**
	 * Create a Payload from the given data.
	 * @param data the data part for the payload
	 * @return the created Payload
	 */
	public static Payload asPayload(DataBuffer data) {
		if (data instanceof NettyDataBuffer) {
			return ByteBufPayload.create(getByteBuf(data));
		}
		else if (data instanceof DefaultDataBuffer) {
			return DefaultPayload.create(getByteBuffer(data));
		}
		else {
			return DefaultPayload.create(data.asByteBuffer());
		}
	}

	private static ByteBuf getByteBuf(DataBuffer dataBuffer) {
		return ((NettyDataBuffer) dataBuffer).getNativeBuffer();
	}

	private static
	ByteBuffer getByteBuffer(DataBuffer dataBuffer) {
		return ((DefaultDataBuffer) dataBuffer).getNativeBuffer();
	}
}
