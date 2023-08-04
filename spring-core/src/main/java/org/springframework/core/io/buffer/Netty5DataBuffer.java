/*
 * Copyright 2002-2023 the original author or authors.
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
import java.nio.charset.Charset;
import java.util.NoSuchElementException;
import java.util.function.IntPredicate;

import io.netty5.buffer.Buffer;
import io.netty5.buffer.BufferComponent;
import io.netty5.buffer.ComponentIterator;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Implementation of the {@code DataBuffer} interface that wraps a Netty 5
 * {@link Buffer}. Typically constructed with {@link Netty5DataBufferFactory}.
 *
 * @author Violeta Georgieva
 * @author Arjen Poutsma
 * @since 6.0
 */
public final class Netty5DataBuffer implements CloseableDataBuffer, TouchableDataBuffer {

	private final Buffer buffer;

	private final Netty5DataBufferFactory dataBufferFactory;


	/**
	 * Create a new {@code Netty5DataBuffer} based on the given {@code Buffer}.
	 * @param buffer the buffer to base this buffer on
	 */
	Netty5DataBuffer(Buffer buffer, Netty5DataBufferFactory dataBufferFactory) {
		Assert.notNull(buffer, "Buffer must not be null");
		Assert.notNull(dataBufferFactory, "Netty5DataBufferFactory must not be null");
		this.buffer = buffer;
		this.dataBufferFactory = dataBufferFactory;
	}

	/**
	 * Directly exposes the native {@code Buffer} that this buffer is based on.
	 * @return the wrapped buffer
	 */
	public Buffer getNativeBuffer() {
		return this.buffer;
	}

	@Override
	public DataBufferFactory factory() {
		return this.dataBufferFactory;
	}

	@Override
	public int indexOf(IntPredicate predicate, int fromIndex) {
		Assert.notNull(predicate, "IntPredicate must not be null");
		if (fromIndex < 0) {
			fromIndex = 0;
		}
		else if (fromIndex >= this.buffer.writerOffset()) {
			return -1;
		}
		int length = this.buffer.writerOffset() - fromIndex;
		int bytes = this.buffer.openCursor(fromIndex, length).process(predicate.negate()::test);
		return bytes == -1 ? -1 : fromIndex + bytes;
	}

	@Override
	public int lastIndexOf(IntPredicate predicate, int fromIndex) {
		Assert.notNull(predicate, "IntPredicate must not be null");
		if (fromIndex < 0) {
			return -1;
		}
		fromIndex = Math.min(fromIndex, this.buffer.writerOffset() - 1);
		return this.buffer.openCursor(0, fromIndex + 1).process(predicate.negate()::test);
	}

	@Override
	public int readableByteCount() {
		return this.buffer.readableBytes();
	}

	@Override
	public int writableByteCount() {
		return this.buffer.writableBytes();
	}

	@Override
	public int readPosition() {
		return this.buffer.readerOffset();
	}

	@Override
	public Netty5DataBuffer readPosition(int readPosition) {
		this.buffer.readerOffset(readPosition);
		return this;
	}

	@Override
	public int writePosition() {
		return this.buffer.writerOffset();
	}

	@Override
	public Netty5DataBuffer writePosition(int writePosition) {
		this.buffer.writerOffset(writePosition);
		return this;
	}

	@Override
	public byte getByte(int index) {
		return this.buffer.getByte(index);
	}

	@Override
	public int capacity() {
		return this.buffer.capacity();
	}

	@Override
	@Deprecated
	public Netty5DataBuffer capacity(int capacity) {
		if (capacity <= 0) {
			throw new IllegalArgumentException(String.format("'newCapacity' %d must be higher than 0", capacity));
		}
		int diff = capacity - capacity();
		if (diff > 0) {
			this.buffer.ensureWritable(this.buffer.writableBytes() + diff);
		}
		return this;
	}

	@Override
	public DataBuffer ensureWritable(int capacity) {
		Assert.isTrue(capacity >= 0, "Capacity must be >= 0");
		this.buffer.ensureWritable(capacity);
		return this;
	}

	@Override
	public byte read() {
		return this.buffer.readByte();
	}

	@Override
	public Netty5DataBuffer read(byte[] destination) {
		return read(destination, 0, destination.length);
	}

	@Override
	public Netty5DataBuffer read(byte[] destination, int offset, int length) {
		this.buffer.readBytes(destination, offset, length);
		return this;
	}

	@Override
	public Netty5DataBuffer write(byte b) {
		this.buffer.writeByte(b);
		return this;
	}

	@Override
	public Netty5DataBuffer write(byte[] source) {
		this.buffer.writeBytes(source);
		return this;
	}

	@Override
	public Netty5DataBuffer write(byte[] source, int offset, int length) {
		this.buffer.writeBytes(source, offset, length);
		return this;
	}

	@Override
	public Netty5DataBuffer write(DataBuffer... dataBuffers) {
		if (!ObjectUtils.isEmpty(dataBuffers)) {
			if (hasNetty5DataBuffers(dataBuffers)) {
				Buffer[] nativeBuffers = new Buffer[dataBuffers.length];
				for (int i = 0; i < dataBuffers.length; i++) {
					nativeBuffers[i] = ((Netty5DataBuffer) dataBuffers[i]).getNativeBuffer();
				}
				return write(nativeBuffers);
			}
			else {
				ByteBuffer[] byteBuffers = new ByteBuffer[dataBuffers.length];
				for (int i = 0; i < dataBuffers.length; i++) {
					byteBuffers[i] = ByteBuffer.allocate(dataBuffers[i].readableByteCount());
					dataBuffers[i].toByteBuffer(byteBuffers[i]);
				}
				return write(byteBuffers);
			}
		}
		return this;
	}

	private static boolean hasNetty5DataBuffers(DataBuffer[] buffers) {
		for (DataBuffer buffer : buffers) {
			if (!(buffer instanceof Netty5DataBuffer)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Netty5DataBuffer write(ByteBuffer... buffers) {
		if (!ObjectUtils.isEmpty(buffers)) {
			for (ByteBuffer buffer : buffers) {
				this.buffer.writeBytes(buffer);
			}
		}
		return this;
	}

	/**
	 * Writes one or more Netty 5 {@link Buffer Buffers} to this buffer,
	 * starting at the current writing position.
	 * @param buffers the buffers to write into this buffer
	 * @return this buffer
	 */
	public Netty5DataBuffer write(Buffer... buffers) {
		if (!ObjectUtils.isEmpty(buffers)) {
			for (Buffer buffer : buffers) {
				this.buffer.writeBytes(buffer);
			}
		}
		return this;
	}

	@Override
	public DataBuffer write(CharSequence charSequence, Charset charset) {
		Assert.notNull(charSequence, "CharSequence must not be null");
		Assert.notNull(charset, "Charset must not be null");

		this.buffer.writeCharSequence(charSequence, charset);
		return this;
	}

	/**
	 * {@inheritDoc}
	 * <p><strong>Note</strong> that due to the lack of a {@code slice} method
	 * in Netty 5's {@link Buffer}, this implementation returns a copy that
	 * does <strong>not</strong> share its contents with this buffer.
	 */
	@Override
	@Deprecated
	public DataBuffer slice(int index, int length) {
		Buffer copy = this.buffer.copy(index, length);
		return new Netty5DataBuffer(copy, this.dataBufferFactory);
	}

	@Override
	public DataBuffer split(int index) {
		Buffer split = this.buffer.split(index);
		return new Netty5DataBuffer(split, this.dataBufferFactory);
	}

	@Override
	@Deprecated
	public ByteBuffer asByteBuffer() {
		return toByteBuffer();
	}

	@Override
	@Deprecated
	public ByteBuffer asByteBuffer(int index, int length) {
		return toByteBuffer(index, length);
	}

	@Override
	@Deprecated
	public ByteBuffer toByteBuffer(int index, int length) {
		ByteBuffer copy = this.buffer.isDirect() ?
				ByteBuffer.allocateDirect(length) :
				ByteBuffer.allocate(length);

		this.buffer.copyInto(index, copy, 0, length);
		return copy;
	}

	@Override
	public void toByteBuffer(int srcPos, ByteBuffer dest, int destPos, int length) {
		this.buffer.copyInto(srcPos, dest, destPos, length);
	}

	@Override
	public ByteBufferIterator readableByteBuffers() {
		return new BufferComponentIterator<>(this.buffer.forEachComponent(), true);
	}

	@Override
	public ByteBufferIterator writableByteBuffers() {
		return new BufferComponentIterator<>(this.buffer.forEachComponent(), false);
	}

	@Override
	public String toString(Charset charset) {
		Assert.notNull(charset, "Charset must not be null");
		return this.buffer.toString(charset);
	}

	@Override
	public String toString(int index, int length, Charset charset) {
		Assert.notNull(charset, "Charset must not be null");
		byte[] data = new byte[length];
		this.buffer.copyInto(index, data, 0, length);
		return new String(data, 0, length, charset);
	}

	@Override
	public Netty5DataBuffer touch(Object hint) {
		this.buffer.touch(hint);
		return this;
	}

	@Override
	public void close() {
		this.buffer.close();
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof Netty5DataBuffer that && this.buffer.equals(that.buffer)));
	}

	@Override
	public int hashCode() {
		return this.buffer.hashCode();
	}

	@Override
	public String toString() {
		return this.buffer.toString();
	}


	private static final class BufferComponentIterator<T extends BufferComponent & ComponentIterator.Next>
			implements ByteBufferIterator {

		private final ComponentIterator<T> delegate;

		private final boolean readable;

		@Nullable
		private T next;

		public BufferComponentIterator(ComponentIterator<T> delegate, boolean readable) {
			Assert.notNull(delegate, "Delegate must not be null");
			this.delegate = delegate;
			this.readable = readable;
			this.next = readable ? this.delegate.firstReadable() : this.delegate.firstWritable();
		}

		@Override
		public boolean hasNext() {
			return this.next != null;
		}

		@Override
		public ByteBuffer next() {
			if (this.next != null) {
				ByteBuffer result;
				if (this.readable) {
					result = this.next.readableBuffer();
					this.next = this.next.nextReadable();
				}
				else {
					result = this.next.writableBuffer();
					this.next = this.next.nextWritable();
				}
				return result;
			}
			else {
				throw new NoSuchElementException();
			}
		}

		@Override
		public void close() {
			this.delegate.close();
		}
	}

}
