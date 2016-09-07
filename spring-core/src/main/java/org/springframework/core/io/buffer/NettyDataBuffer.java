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

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.IntPredicate;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Implementation of the {@code DataBuffer} interface that wraps a Netty {@link ByteBuf}.
 * Typically constructed using the {@link NettyDataBufferFactory}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public class NettyDataBuffer implements PooledDataBuffer {

	private final NettyDataBufferFactory dataBufferFactory;

	private ByteBuf byteBuf;

	/**
	 * Creates a new {@code NettyDataBuffer} based on the given {@code ByteBuff}.
	 * @param byteBuf the buffer to base this buffer on
	 */
	NettyDataBuffer(ByteBuf byteBuf, NettyDataBufferFactory dataBufferFactory) {
		Assert.notNull(byteBuf, "'byteBuf' must not be null");
		Assert.notNull(dataBufferFactory, "'dataBufferFactory' must not be null");

		this.byteBuf = byteBuf;
		this.dataBufferFactory = dataBufferFactory;
	}

	@Override
	public NettyDataBufferFactory factory() {
		return this.dataBufferFactory;
	}

	/**
	 * Directly exposes the native {@code ByteBuf} that this buffer is based on.
	 * @return the wrapped byte buffer
	 */
	public ByteBuf getNativeBuffer() {
		return this.byteBuf;
	}

	@Override
	public int indexOf(IntPredicate predicate, int fromIndex) {
		Assert.notNull(predicate, "'predicate' must not be null");
		if (fromIndex < 0) {
			fromIndex = 0;
		}
		else if (fromIndex >= this.byteBuf.writerIndex()) {
			return -1;
		}
		int length = this.byteBuf.writerIndex() - fromIndex;

		return this.byteBuf.forEachByte(fromIndex, length, predicate.negate()::test);
	}

	@Override
	public int lastIndexOf(IntPredicate predicate, int fromIndex) {
		Assert.notNull(predicate, "'predicate' must not be null");
		if (fromIndex < 0) {
			return -1;
		}
		fromIndex = Math.min(fromIndex, this.byteBuf.writerIndex() - 1);

		return this.byteBuf.forEachByteDesc(0, fromIndex, predicate.negate()::test);
	}

	@Override
	public int readableByteCount() {
		return this.byteBuf.readableBytes();
	}

	@Override
	public byte read() {
		return this.byteBuf.readByte();
	}

	@Override
	public NettyDataBuffer read(byte[] destination) {
		this.byteBuf.readBytes(destination);
		return this;
	}

	@Override
	public NettyDataBuffer read(byte[] destination, int offset, int length) {
		this.byteBuf.readBytes(destination, offset, length);
		return this;
	}

	@Override
	public NettyDataBuffer write(byte b) {
		this.byteBuf.writeByte(b);
		return this;
	}

	@Override
	public NettyDataBuffer write(byte[] source) {
		this.byteBuf.writeBytes(source);
		return this;
	}

	@Override
	public NettyDataBuffer write(byte[] source, int offset, int length) {
		this.byteBuf.writeBytes(source, offset, length);
		return this;
	}

	@Override
	public NettyDataBuffer write(DataBuffer... buffers) {
		if (!ObjectUtils.isEmpty(buffers)) {
			if (buffers[0] instanceof NettyDataBuffer) {
				ByteBuf[] nativeBuffers = Arrays.stream(buffers)
						.map(b -> ((NettyDataBuffer) b).getNativeBuffer())
						.toArray(ByteBuf[]::new);

				write(nativeBuffers);
			}
			else {
				ByteBuffer[] byteBuffers =
						Arrays.stream(buffers).map(DataBuffer::asByteBuffer)
								.toArray(ByteBuffer[]::new);
				write(byteBuffers);
			}
		}
		return this;
	}

	@Override
	public NettyDataBuffer write(ByteBuffer... buffers) {
		Assert.notNull(buffers, "'buffers' must not be null");

		ByteBuf[] wrappedBuffers = Arrays.stream(buffers).map(Unpooled::wrappedBuffer)
				.toArray(ByteBuf[]::new);

		return write(wrappedBuffers);
	}

	/**
	 * Writes one or more Netty {@link ByteBuf}s to this buffer, starting at the current
	 * writing position.
	 * @param byteBufs the buffers to write into this buffer
	 * @return this buffer
	 */
	public NettyDataBuffer write(ByteBuf... byteBufs) {
		Assert.notNull(byteBufs, "'byteBufs' must not be null");

		CompositeByteBuf composite =
				new CompositeByteBuf(this.byteBuf.alloc(), this.byteBuf.isDirect(),
						byteBufs.length + 1);
		composite.addComponent(this.byteBuf);
		composite.addComponents(byteBufs);

		int writerIndex = this.byteBuf.readableBytes() +
				Arrays.stream(byteBufs).mapToInt(ByteBuf::readableBytes).sum();
		composite.writerIndex(writerIndex);

		this.byteBuf = composite;

		return this;
	}

	@Override
	public DataBuffer slice(int index, int length) {
		ByteBuf slice = this.byteBuf.slice(index, length);
		return new NettyDataBuffer(slice, this.dataBufferFactory);
	}

	@Override
	public ByteBuffer asByteBuffer() {
		return this.byteBuf.nioBuffer();
	}

	@Override
	public InputStream asInputStream() {
		return new ByteBufInputStream(this.byteBuf);
	}

	@Override
	public OutputStream asOutputStream() {
		return new ByteBufOutputStream(this.byteBuf);
	}

	@Override
	public PooledDataBuffer retain() {
		return new NettyDataBuffer(this.byteBuf.retain(), dataBufferFactory);
	}

	@Override
	public boolean release() {
		return this.byteBuf.release();
	}

	@Override
	public int hashCode() {
		return this.byteBuf.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		else if (obj instanceof NettyDataBuffer) {
			NettyDataBuffer other = (NettyDataBuffer) obj;
			return this.byteBuf.equals(other.byteBuf);
		}
		return false;
	}

	@Override
	public String toString() {
		return this.byteBuf.toString();
	}
}
