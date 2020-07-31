/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core.io.buffer;

import java.nio.ByteBuffer;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;

import org.springframework.util.Assert;

/**
 * Implementation of the {@code DataBufferFactory} interface based on a
 * Netty {@link ByteBufAllocator}.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 5.0
 * @see io.netty.buffer.PooledByteBufAllocator
 * @see io.netty.buffer.UnpooledByteBufAllocator
 */
public class NettyDataBufferFactory implements DataBufferFactory {

	private final ByteBufAllocator byteBufAllocator;


	/**
	 * Create a new {@code NettyDataBufferFactory} based on the given factory.
	 * @param byteBufAllocator the factory to use
	 * @see io.netty.buffer.PooledByteBufAllocator
	 * @see io.netty.buffer.UnpooledByteBufAllocator
	 */
	public NettyDataBufferFactory(ByteBufAllocator byteBufAllocator) {
		Assert.notNull(byteBufAllocator, "ByteBufAllocator must not be null");
		this.byteBufAllocator = byteBufAllocator;
	}


	/**
	 * Return the {@code ByteBufAllocator} used by this factory.
	 */
	public ByteBufAllocator getByteBufAllocator() {
		return this.byteBufAllocator;
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

	@Override
	public DataBuffer wrap(byte[] bytes) {
		ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
		return new NettyDataBuffer(byteBuf, this);
	}

	/**
	 * Wrap the given Netty {@link ByteBuf} in a {@code NettyDataBuffer}.
	 * @param byteBuf the Netty byte buffer to wrap
	 * @return the wrapped buffer
	 */
	public NettyDataBuffer wrap(ByteBuf byteBuf) {
		byteBuf.touch();
		return new NettyDataBuffer(byteBuf, this);
	}

	/**
	 * {@inheritDoc}
	 * <p>This implementation uses Netty's {@link CompositeByteBuf}.
	 */
	@Override
	public DataBuffer join(List<? extends DataBuffer> dataBuffers) {
		Assert.notEmpty(dataBuffers, "DataBuffer List must not be empty");
		int bufferCount = dataBuffers.size();
		if (bufferCount == 1) {
			return dataBuffers.get(0);
		}
		CompositeByteBuf composite = this.byteBufAllocator.compositeBuffer(bufferCount);
		for (DataBuffer dataBuffer : dataBuffers) {
			Assert.isInstanceOf(NettyDataBuffer.class, dataBuffer);
			composite.addComponent(true, ((NettyDataBuffer) dataBuffer).getNativeBuffer());
		}
		return new NettyDataBuffer(composite, this);
	}

	/**
	 * Return the given Netty {@link DataBuffer} as a {@link ByteBuf}.
	 * <p>Returns the {@linkplain NettyDataBuffer#getNativeBuffer() native buffer}
	 * if {@code buffer} is a {@link NettyDataBuffer}; returns
	 * {@link Unpooled#wrappedBuffer(ByteBuffer)} otherwise.
	 * @param buffer the {@code DataBuffer} to return a {@code ByteBuf} for
	 * @return the netty {@code ByteBuf}
	 */
	public static ByteBuf toByteBuf(DataBuffer buffer) {
		if (buffer instanceof NettyDataBuffer) {
			return ((NettyDataBuffer) buffer).getNativeBuffer();
		}
		else {
			return Unpooled.wrappedBuffer(buffer.asByteBuffer());
		}
	}


	@Override
	public String toString() {
		return "NettyDataBufferFactory (" + this.byteBufAllocator + ")";
	}

}
