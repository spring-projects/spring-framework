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

import io.netty.buffer.ByteBuf;
import io.rsocket.Frame;
import io.rsocket.Payload;
import io.rsocket.util.ByteBufPayload;
import io.rsocket.util.DefaultPayload;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.util.Assert;

/**
 * Static utility methods to create {@link Payload} from {@link DataBuffer}s
 * and vice versa.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
abstract class PayloadUtils {

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

			Assert.isTrue(!(payload instanceof ByteBufPayload) && !(payload instanceof Frame),
					"NettyDataBufferFactory expected, actual: " + bufferFactory.getClass().getSimpleName());

			return bufferFactory.wrap(payload.getData());
		}
		finally {
			if (payload.refCnt() > 0) {
				payload.release();
			}
		}
	}

	/**
	 * Create a Payload from the given metadata and data.
	 * @param metadata the metadata part for the payload
	 * @param data the data part for the payload
	 * @return the created Payload
	 */
	public static Payload createPayload(DataBuffer metadata, DataBuffer data) {
		if (metadata instanceof NettyDataBuffer && data instanceof NettyDataBuffer) {
			return ByteBufPayload.create(
					((NettyDataBuffer) data).getNativeBuffer(),
					((NettyDataBuffer) metadata).getNativeBuffer());
		}
		else if (metadata instanceof DefaultDataBuffer && data instanceof DefaultDataBuffer) {
			return DefaultPayload.create(
					((DefaultDataBuffer) data).getNativeBuffer(),
					((DefaultDataBuffer) metadata).getNativeBuffer());
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
	public static Payload createPayload(DataBuffer data) {
		if (data instanceof NettyDataBuffer) {
			return ByteBufPayload.create(((NettyDataBuffer) data).getNativeBuffer());
		}
		else if (data instanceof DefaultDataBuffer) {
			return DefaultPayload.create(((DefaultDataBuffer) data).getNativeBuffer());
		}
		else {
			return DefaultPayload.create(data.asByteBuffer());
		}
	}

}
