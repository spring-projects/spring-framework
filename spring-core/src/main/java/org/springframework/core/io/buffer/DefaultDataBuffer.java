/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.IntPredicate;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Default implementation of the {@link DataBuffer} interface that uses a
 * {@link ByteBuffer} internally. with separate read and write positions.
 * Constructed using the {@link DefaultDataBufferFactory}.
 *
 * <p>Inspired by Netty's {@code ByteBuf}. Introduced so that non-Netty runtimes
 * (i.e. Servlet) do not require Netty on the classpath.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @since 5.0
 * @see DefaultDataBufferFactory
 */
public class DefaultDataBuffer implements DataBuffer {

	private static final int MAX_CAPACITY = Integer.MAX_VALUE;

	private static final int CAPACITY_THRESHOLD = 1024 * 1024 * 4;


	private final DefaultDataBufferFactory dataBufferFactory;

	private ByteBuffer byteBuffer;

	private int capacity;

	private int readPosition;

	private int writePosition;


	private DefaultDataBuffer(DefaultDataBufferFactory dataBufferFactory, ByteBuffer byteBuffer) {
		Assert.notNull(dataBufferFactory, "DefaultDataBufferFactory must not be null");
		Assert.notNull(byteBuffer, "ByteBuffer must not be null");
		this.dataBufferFactory = dataBufferFactory;
		ByteBuffer slice = byteBuffer.slice();
		this.byteBuffer = slice;
		this.capacity = slice.remaining();
	}

	static DefaultDataBuffer fromFilledByteBuffer(DefaultDataBufferFactory dataBufferFactory, ByteBuffer byteBuffer) {
		DefaultDataBuffer dataBuffer = new DefaultDataBuffer(dataBufferFactory, byteBuffer);
		dataBuffer.writePosition(byteBuffer.remaining());
		return dataBuffer;
	}

	static DefaultDataBuffer fromEmptyByteBuffer(DefaultDataBufferFactory dataBufferFactory, ByteBuffer byteBuffer) {
		return new DefaultDataBuffer(dataBufferFactory, byteBuffer);
	}


	/**
	 * Directly exposes the native {@code ByteBuffer} that this buffer is based
	 * on. The {@linkplain ByteBuffer#position() position} of the returned
	 * {@code ByteBuffer} is set to the {@linkplain #readPosition() read
	 * position}, and the {@linkplain ByteBuffer#limit()} to the
	 * {@linkplain #writePosition() write position}.
	 * @return the wrapped byte buffer
	 */
	public ByteBuffer getNativeBuffer() {
		return this.byteBuffer.duplicate()
				.position(this.readPosition)
				.limit(this.writePosition);
	}

	private void setNativeBuffer(ByteBuffer byteBuffer) {
		this.byteBuffer = byteBuffer;
		this.capacity = byteBuffer.capacity();
	}


	@Override
	public DefaultDataBufferFactory factory() {
		return this.dataBufferFactory;
	}

	@Override
	public int indexOf(IntPredicate predicate, int fromIndex) {
		Assert.notNull(predicate, "IntPredicate must not be null");
		if (fromIndex < 0) {
			fromIndex = 0;
		}
		else if (fromIndex >= this.writePosition) {
			return -1;
		}
		for (int i = fromIndex; i < this.writePosition; i++) {
			byte b = this.byteBuffer.get(i);
			if (predicate.test(b)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public int lastIndexOf(IntPredicate predicate, int fromIndex) {
		Assert.notNull(predicate, "IntPredicate must not be null");
		int i = Math.min(fromIndex, this.writePosition - 1);
		for (; i >= 0; i--) {
			byte b = this.byteBuffer.get(i);
			if (predicate.test(b)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public int readableByteCount() {
		return this.writePosition - this.readPosition;
	}

	@Override
	public int writableByteCount() {
		return this.capacity - this.writePosition;
	}

	@Override
	public int readPosition() {
		return this.readPosition;
	}

	@Override
	public DefaultDataBuffer readPosition(int readPosition) {
		assertIndex(readPosition >= 0, "'readPosition' %d must be >= 0", readPosition);
		assertIndex(readPosition <= this.writePosition, "'readPosition' %d must be <= %d",
				readPosition, this.writePosition);
		this.readPosition = readPosition;
		return this;
	}

	@Override
	public int writePosition() {
		return this.writePosition;
	}

	@Override
	public DefaultDataBuffer writePosition(int writePosition) {
		assertIndex(writePosition >= this.readPosition, "'writePosition' %d must be >= %d",
				writePosition, this.readPosition);
		assertIndex(writePosition <= this.capacity, "'writePosition' %d must be <= %d",
				writePosition, this.capacity);
		this.writePosition = writePosition;
		return this;
	}

	@Override
	public int capacity() {
		return this.capacity;
	}

	@Override
	@Deprecated
	public DataBuffer capacity(int capacity) {
		setCapacity(capacity);
		return this;
	}

	private void setCapacity(int newCapacity) {
		if (newCapacity < 0) {
			throw new IllegalArgumentException(String.format("'newCapacity' %d must be 0 or higher", newCapacity));
		}
		int readPosition = readPosition();
		int writePosition = writePosition();
		int oldCapacity = capacity();

		if (newCapacity > oldCapacity) {
			ByteBuffer oldBuffer = this.byteBuffer;
			ByteBuffer newBuffer = allocate(newCapacity, oldBuffer.isDirect());
			oldBuffer.position(0).limit(oldBuffer.capacity());
			newBuffer.position(0).limit(oldBuffer.capacity());
			newBuffer.put(oldBuffer);
			newBuffer.clear();
			setNativeBuffer(newBuffer);
		}
		else if (newCapacity < oldCapacity) {
			ByteBuffer oldBuffer = this.byteBuffer;
			ByteBuffer newBuffer = allocate(newCapacity, oldBuffer.isDirect());
			if (readPosition < newCapacity) {
				if (writePosition > newCapacity) {
					writePosition = newCapacity;
					writePosition(writePosition);
				}
				oldBuffer.position(readPosition).limit(writePosition);
				newBuffer.position(readPosition).limit(writePosition);
				newBuffer.put(oldBuffer);
				newBuffer.clear();
			}
			else {
				readPosition(newCapacity);
				writePosition(newCapacity);
			}
			setNativeBuffer(newBuffer);
		}
	}

	@Override
	public DataBuffer ensureWritable(int length) {
		if (length > writableByteCount()) {
			int newCapacity = calculateCapacity(this.writePosition + length);
			setCapacity(newCapacity);
		}
		return this;
	}

	private static ByteBuffer allocate(int capacity, boolean direct) {
		return (direct ? ByteBuffer.allocateDirect(capacity) : ByteBuffer.allocate(capacity));
	}

	@Override
	public byte getByte(int index) {
		assertIndex(index >= 0, "index %d must be >= 0", index);
		assertIndex(index <= this.writePosition - 1, "index %d must be <= %d", index, this.writePosition - 1);
		return this.byteBuffer.get(index);
	}

	@Override
	public byte read() {
		assertIndex(this.readPosition <= this.writePosition - 1, "readPosition %d must be <= %d",
				this.readPosition, this.writePosition - 1);
		int pos = this.readPosition;
		byte b = this.byteBuffer.get(pos);
		this.readPosition = pos + 1;
		return b;
	}

	@Override
	public DefaultDataBuffer read(byte[] destination) {
		Assert.notNull(destination, "Byte array must not be null");
		read(destination, 0, destination.length);
		return this;
	}

	@Override
	public DefaultDataBuffer read(byte[] destination, int offset, int length) {
		Assert.notNull(destination, "Byte array must not be null");
		assertIndex(this.readPosition <= this.writePosition - length,
				"readPosition %d and length %d should be smaller than writePosition %d",
				this.readPosition, length, this.writePosition);

		ByteBuffer tmp = this.byteBuffer.duplicate();
		int limit = this.readPosition + length;
		tmp.clear().position(this.readPosition).limit(limit);
		tmp.get(destination, offset, length);

		this.readPosition += length;
		return this;
	}

	@Override
	public DefaultDataBuffer write(byte b) {
		ensureWritable(1);
		int pos = this.writePosition;
		this.byteBuffer.put(pos, b);
		this.writePosition = pos + 1;
		return this;
	}

	@Override
	public DefaultDataBuffer write(byte[] source) {
		Assert.notNull(source, "Byte array must not be null");
		write(source, 0, source.length);
		return this;
	}

	@Override
	public DefaultDataBuffer write(byte[] source, int offset, int length) {
		Assert.notNull(source, "Byte array must not be null");
		ensureWritable(length);

		ByteBuffer tmp = this.byteBuffer.duplicate();
		int limit = this.writePosition + length;
		tmp.clear().position(this.writePosition).limit(limit);
		tmp.put(source, offset, length);

		this.writePosition += length;
		return this;
	}

	@Override
	public DefaultDataBuffer write(DataBuffer... dataBuffers) {
		if (!ObjectUtils.isEmpty(dataBuffers)) {
			ByteBuffer[] byteBuffers = new ByteBuffer[dataBuffers.length];
			for (int i = 0; i < dataBuffers.length; i++) {
				byteBuffers[i] = ByteBuffer.allocate(dataBuffers[i].readableByteCount());
				dataBuffers[i].toByteBuffer(byteBuffers[i]);
			}
			write(byteBuffers);
		}
		return this;
	}

	@Override
	public DefaultDataBuffer write(ByteBuffer... buffers) {
		if (!ObjectUtils.isEmpty(buffers)) {
			int capacity = Arrays.stream(buffers).mapToInt(ByteBuffer::remaining).sum();
			ensureWritable(capacity);
			Arrays.stream(buffers).forEach(this::write);
		}
		return this;
	}

	private void write(ByteBuffer source) {
		int length = source.remaining();
		ByteBuffer tmp = this.byteBuffer.duplicate();
		int limit = this.writePosition + source.remaining();
		tmp.clear().position(this.writePosition).limit(limit);
		tmp.put(source);
		this.writePosition += length;
	}

	@Override
	@Deprecated
	public DefaultDataBuffer slice(int index, int length) {
		checkIndex(index, length);
		int oldPosition = this.byteBuffer.position();
		try {
			this.byteBuffer.position(index);
			ByteBuffer slice = this.byteBuffer.slice();
			slice.limit(length);
			return new SlicedDefaultDataBuffer(slice, this.dataBufferFactory, length);
		}
		finally {
			this.byteBuffer.position(oldPosition);
		}
	}

	@Override
	public DefaultDataBuffer split(int index) {
		checkIndex(index);

		ByteBuffer split = this.byteBuffer.duplicate().clear()
			.position(0)
			.limit(index)
			.slice();

		DefaultDataBuffer result = new DefaultDataBuffer(this.dataBufferFactory, split);
		result.writePosition = Math.min(this.writePosition, index);
		result.readPosition = Math.min(this.readPosition, index);

		this.byteBuffer = this.byteBuffer.duplicate().clear()
				.position(index)
				.limit(this.byteBuffer.capacity())
				.slice();
		this.writePosition = Math.max(this.writePosition, index) - index;
		this.readPosition = Math.max(this.readPosition, index) - index;
		this.capacity = this.byteBuffer.capacity();

		return result;
	}

	@Override
	@Deprecated
	public ByteBuffer asByteBuffer() {
		return asByteBuffer(this.readPosition, readableByteCount());
	}

	@Override
	@Deprecated
	public ByteBuffer asByteBuffer(int index, int length) {
		checkIndex(index, length);

		ByteBuffer duplicate = this.byteBuffer.duplicate();
		duplicate.position(index);
		duplicate.limit(index + length);
		return duplicate.slice();
	}

	@Override
	@Deprecated
	public ByteBuffer toByteBuffer(int index, int length) {
		checkIndex(index, length);

		ByteBuffer copy = allocate(length, this.byteBuffer.isDirect());
		ByteBuffer readOnly = this.byteBuffer.asReadOnlyBuffer();
		readOnly.clear().position(index).limit(index + length);
		copy.put(readOnly);
		return copy.flip();
	}

	@Override
	public void toByteBuffer(int srcPos, ByteBuffer dest, int destPos, int length) {
		checkIndex(srcPos, length);
		Assert.notNull(dest, "Dest must not be null");

		dest = dest.duplicate().clear();
		dest.put(destPos, this.byteBuffer, srcPos, length);
	}

	@Override
	public DataBuffer.ByteBufferIterator readableByteBuffers() {
		ByteBuffer readOnly = this.byteBuffer.slice(this.readPosition, readableByteCount())
				.asReadOnlyBuffer();
		return new ByteBufferIterator(readOnly);
	}

	@Override
	public DataBuffer.ByteBufferIterator writableByteBuffers() {
		ByteBuffer slice = this.byteBuffer.slice(this.writePosition, writableByteCount());
		return new ByteBufferIterator(slice);
	}

	@Override
	public String toString(int index, int length, Charset charset) {
		checkIndex(index, length);
		Assert.notNull(charset, "Charset must not be null");

		byte[] bytes;
		int offset;

		if (this.byteBuffer.hasArray()) {
			bytes = this.byteBuffer.array();
			offset = this.byteBuffer.arrayOffset() + index;
		}
		else {
			bytes = new byte[length];
			offset = 0;
			ByteBuffer duplicate = this.byteBuffer.duplicate();
			duplicate.clear().position(index).limit(index + length);
			duplicate.get(bytes, 0, length);
		}
		return new String(bytes, offset, length, charset);
	}

	/**
	 * Calculate the capacity of the buffer.
	 * @see io.netty.buffer.AbstractByteBufAllocator#calculateNewCapacity(int, int)
	 */
	private int calculateCapacity(int neededCapacity) {
		Assert.isTrue(neededCapacity >= 0, "'neededCapacity' must be >= 0");

		if (neededCapacity == CAPACITY_THRESHOLD) {
			return CAPACITY_THRESHOLD;
		}
		else if (neededCapacity > CAPACITY_THRESHOLD) {
			int newCapacity = neededCapacity / CAPACITY_THRESHOLD * CAPACITY_THRESHOLD;
			if (newCapacity > MAX_CAPACITY - CAPACITY_THRESHOLD) {
				newCapacity = MAX_CAPACITY;
			}
			else {
				newCapacity += CAPACITY_THRESHOLD;
			}
			return newCapacity;
		}
		else {
			int newCapacity = 64;
			while (newCapacity < neededCapacity) {
				newCapacity <<= 1;
			}
			return Math.min(newCapacity, MAX_CAPACITY);
		}
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof DefaultDataBuffer that &&
				this.readPosition == that.readPosition &&
				this.writePosition == that.writePosition &&
				this.byteBuffer.equals(that.byteBuffer)));
	}

	@Override
	public int hashCode() {
		return this.byteBuffer.hashCode();
	}

	@Override
	public String toString() {
		return String.format("DefaultDataBuffer (r: %d, w: %d, c: %d)",
				this.readPosition, this.writePosition, this.capacity);
	}


	private void checkIndex(int index, int length) {
		checkIndex(index);
		checkLength(length);
	}

	private void checkIndex(int index) {
		assertIndex(index >= 0, "index %d must be >= 0", index);
		assertIndex(index <= this.capacity, "index %d must be <= %d", index, this.capacity);
	}

	private void checkLength(int length) {
		assertIndex(length >= 0, "length %d must be >= 0", length);
		assertIndex(length <= this.capacity, "length %d must be <= %d", length, this.capacity);
	}

	private void assertIndex(boolean expression, String format, Object... args) {
		if (!expression) {
			String message = String.format(format, args);
			throw new IndexOutOfBoundsException(message);
		}
	}


	private static class SlicedDefaultDataBuffer extends DefaultDataBuffer {

		SlicedDefaultDataBuffer(ByteBuffer byteBuffer, DefaultDataBufferFactory dataBufferFactory, int length) {
			super(dataBufferFactory, byteBuffer);
			writePosition(length);
		}

		@Override
		@SuppressWarnings("deprecation")
		public DefaultDataBuffer capacity(int newCapacity) {
			throw new UnsupportedOperationException("Changing the capacity of a sliced buffer is not supported");
		}
	}


	private static final class ByteBufferIterator implements DataBuffer.ByteBufferIterator {

		private final ByteBuffer buffer;

		private boolean hasNext = true;


		public ByteBufferIterator(ByteBuffer buffer) {
			this.buffer = buffer;
		}

		@Override
		public boolean hasNext() {
			return this.hasNext;
		}

		@Override
		public ByteBuffer next() {
			if (!this.hasNext) {
				throw new NoSuchElementException();
			}
			else {
				this.hasNext = false;
				return this.buffer;
			}
		}

		@Override
		public void close() {
		}
	}

}
