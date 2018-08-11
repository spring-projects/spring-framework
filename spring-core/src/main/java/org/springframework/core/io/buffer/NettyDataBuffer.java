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

package org.springframework.core.io.buffer;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.IntPredicate;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;

import org.springframework.util.Assert;

/**
 * Implementation of the {@code DataBuffer} interface that wraps a Netty
 * {@link ByteBuf}. Typically constructed with {@link NettyDataBufferFactory}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public class NettyDataBuffer implements PooledDataBuffer {

	private final ByteBuf byteBuf;

	private final NettyDataBufferFactory dataBufferFactory;


	/**
	 * Creates a new {@code NettyDataBuffer} based on the given {@code ByteBuff}.
	 * @param byteBuf the buffer to base this buffer on
	 */
	NettyDataBuffer(ByteBuf byteBuf, NettyDataBufferFactory dataBufferFactory) {
		Assert.notNull(byteBuf, "ByteBuf must not be null");
		Assert.notNull(dataBufferFactory, "NettyDataBufferFactory must not be null");
		this.byteBuf = byteBuf;
		this.dataBufferFactory = dataBufferFactory;
	}


	/**
	 * Directly exposes the native {@code ByteBuf} that this buffer is based on.
	 * @return the wrapped byte buffer
	 */
	public ByteBuf getNativeBuffer() {
		return this.byteBuf;
	}

	@Override
	public NettyDataBufferFactory factory() {
		return this.dataBufferFactory;
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
		return this.byteBuf.forEachByteDesc(0, fromIndex + 1, predicate.negate()::test);
	}

	@Override
	public int readableByteCount() {
		return this.byteBuf.readableBytes();
	}

	@Override
	public int writableByteCount() {
		return this.byteBuf.writableBytes();
	}

	@Override
	public int readPosition() {
		return this.byteBuf.readerIndex();
	}

	@Override
	public NettyDataBuffer readPosition(int readPosition) {
		this.byteBuf.readerIndex(readPosition);
		return this;
	}

	@Override
	public int writePosition() {
		return this.byteBuf.writerIndex();
	}

	@Override
	public NettyDataBuffer writePosition(int writePosition) {
		this.byteBuf.writerIndex(writePosition);
		return this;
	}

	@Override
	public byte getByte(int index) {
		return this.byteBuf.getByte(index);
	}

	@Override
	public int capacity() {
		return this.byteBuf.capacity();
	}

	@Override
	public NettyDataBuffer capacity(int capacity) {
		this.byteBuf.capacity(capacity);
		return this;
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
		Assert.notNull(buffers, "'buffers' must not be null");

		if (buffers.length > 0) {
			if (hasNettyDataBuffers(buffers)) {
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

	private static boolean hasNettyDataBuffers(DataBuffer[] dataBuffers) {
		for (DataBuffer dataBuffer : dataBuffers) {
			if (!(dataBuffer instanceof NettyDataBuffer)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public NettyDataBuffer write(ByteBuffer... buffers) {
		Assert.notNull(buffers, "'buffers' must not be null");

		for (ByteBuffer buffer : buffers) {
			this.byteBuf.writeBytes(buffer);
		}
		return this;
	}

	/**
	 * Writes one or more Netty {@link ByteBuf ByteBufs} to this buffer, starting at the current
	 * writing position.
	 * @param byteBufs the buffers to write into this buffer
	 * @return this buffer
	 */
	public NettyDataBuffer write(ByteBuf... byteBufs) {
		Assert.notNull(byteBufs, "'byteBufs' must not be null");

		for (ByteBuf byteBuf : byteBufs) {
			this.byteBuf.writeBytes(byteBuf);
		}
		return this;
	}

	@Override
	public NettyDataBuffer slice(int index, int length) {
		ByteBuf slice = this.byteBuf.slice(index, length);
		return new NettyDataBuffer(slice, this.dataBufferFactory);
	}

	@Override
	public ByteBuffer asByteBuffer() {
		return this.byteBuf.nioBuffer();
	}

	@Override
	public ByteBuffer asByteBuffer(int index, int length) {
		return this.byteBuf.nioBuffer(index, length);
	}

	@Override
	public InputStream asInputStream() {
		return new ByteBufInputStream(this.byteBuf);
	}

	@Override
	public InputStream asInputStream(boolean releaseOnClose) {
		return new ByteBufInputStream(this.byteBuf, releaseOnClose);
	}

	@Override
	public OutputStream asOutputStream() {
		return new ByteBufOutputStream(this.byteBuf);
	}

	@Override
	public PooledDataBuffer retain() {
		return new NettyDataBuffer(this.byteBuf.retain(), this.dataBufferFactory);
	}

	@Override
	public boolean release() {
		return this.byteBuf.release();
	}


	@Override
	public boolean equals(Object other) {
		return  (this == other || (other instanceof NettyDataBuffer &&
				this.byteBuf.equals(((NettyDataBuffer) other).byteBuf)));
	}

	@Override
	public int hashCode() {
		return this.byteBuf.hashCode();
	}

	@Override
	public String toString() {
		return this.byteBuf.toString();
	}

}
