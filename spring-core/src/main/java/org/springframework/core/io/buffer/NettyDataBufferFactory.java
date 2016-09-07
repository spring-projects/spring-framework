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

package org.springframework.core.io.buffer;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

import org.springframework.util.Assert;

/**
 * Implementation of the {@code DataBufferFactory} interface based on a Netty
 * {@link ByteBufAllocator}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 * @see io.netty.buffer.PooledByteBufAllocator
 * @see io.netty.buffer.UnpooledByteBufAllocator
 */
public class NettyDataBufferFactory implements DataBufferFactory {

	private final ByteBufAllocator byteBufAllocator;

	/**
	 * Creates a new {@code NettyDataBufferFactory} based on the given factory.
	 * @param byteBufAllocator the factory to use
	 * @see io.netty.buffer.PooledByteBufAllocator
	 * @see io.netty.buffer.UnpooledByteBufAllocator
	 */
	public NettyDataBufferFactory(ByteBufAllocator byteBufAllocator) {
		Assert.notNull(byteBufAllocator, "'byteBufAllocator' must not be null");

		this.byteBufAllocator = byteBufAllocator;
	}

	@Override
	public NettyDataBuffer allocateBuffer() {
		ByteBuf byteBuf = this.byteBufAllocator.buffer();
		return new NettyDataBuffer(byteBuf, this);
	}

	@Override
	public NettyDataBuffer allocateBuffer(int initialCapacity) {
		ByteBuf byteBuf = this.byteBufAllocator.buffer(initialCapacity);
		return new NettyDataBuffer(byteBuf, this);
	}

	@Override
	public NettyDataBuffer wrap(ByteBuffer byteBuffer) {
		ByteBuf byteBuf = Unpooled.wrappedBuffer(byteBuffer);
		return new NettyDataBuffer(byteBuf, this);
	}

	/**
	 * Wraps the given Netty {@link ByteBuf} in a {@code NettyDataBuffer}.
	 * @param byteBuf the Netty byte buffer to wrap
	 * @return the wrapped buffer
	 */
	public NettyDataBuffer wrap(ByteBuf byteBuf) {
		return new NettyDataBuffer(byteBuf, this);
	}

	@Override
	public String toString() {
		return "NettyDataBufferFactory (" + this.byteBufAllocator + ")";
	}
}
