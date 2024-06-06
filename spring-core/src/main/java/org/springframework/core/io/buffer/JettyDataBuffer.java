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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntPredicate;

import org.eclipse.jetty.io.Content;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Implementation of the {@code DataBuffer} interface that can wrap a Jetty
 * {@link Content.Chunk}. Typically constructed with {@link JettyDataBufferFactory}.
 *
 * @author Greg Wilkins
 * @author Lachlan Roberts
 * @author Arjen Poutsma
 * @since 6.2
 */
public final class JettyDataBuffer implements PooledDataBuffer {

	private final DefaultDataBuffer delegate;

	@Nullable
	private final Content.Chunk chunk;

	private final JettyDataBufferFactory bufferFactory;

	private final AtomicInteger refCount = new AtomicInteger(1);


	JettyDataBuffer(JettyDataBufferFactory bufferFactory, DefaultDataBuffer delegate, Content.Chunk chunk) {
		Assert.notNull(bufferFactory, "BufferFactory must not be null");
		Assert.notNull(delegate, "Delegate must not be null");
		Assert.notNull(chunk, "Chunk must not be null");

		this.bufferFactory = bufferFactory;
		this.delegate = delegate;
		this.chunk = chunk;
		this.chunk.retain();
	}

	JettyDataBuffer(JettyDataBufferFactory bufferFactory, DefaultDataBuffer delegate) {
		Assert.notNull(bufferFactory, "BufferFactory must not be null");
		Assert.notNull(delegate, "Delegate must not be null");

		this.bufferFactory = bufferFactory;
		this.delegate = delegate;
		this.chunk = null;
	}

	@Override
	public boolean isAllocated() {
		return this.refCount.get() > 0;
	}

	@Override
	public PooledDataBuffer retain() {
		int result = this.refCount.updateAndGet(c -> {
			if (c != 0) {
				return c + 1;
			}
			else {
				return 0;
			}
		});
		if (result != 0 && this.chunk != null) {
			this.chunk.retain();
		}
		return this;
	}

	@Override
	public PooledDataBuffer touch(Object hint) {
		return this;
	}

	@Override
	public boolean release() {
		int result = this.refCount.updateAndGet(c -> {
			if (c != 0) {
				return c - 1;
			}
			else {
				throw new IllegalStateException("JettyDataBuffer already released: " + this);
			}
		});
		if (this.chunk != null) {
			return this.chunk.release();
		}
		else {
			return result == 0;
		}
	}

	@Override
	public DataBufferFactory factory() {
		return this.bufferFactory;
	}

	// delegation

	@Override
	public int indexOf(IntPredicate predicate, int fromIndex) {
		return this.delegate.indexOf(predicate, fromIndex);
	}

	@Override
	public int lastIndexOf(IntPredicate predicate, int fromIndex) {
		return this.delegate.lastIndexOf(predicate, fromIndex);
	}

	@Override
	public int readableByteCount() {
		return this.delegate.readableByteCount();
	}

	@Override
	public int writableByteCount() {
		return this.delegate.writableByteCount();
	}

	@Override
	public int capacity() {
		return this.delegate.capacity();
	}

	@Override
	@Deprecated
	public DataBuffer capacity(int capacity) {
		this.delegate.capacity(capacity);
		return this;
	}

	@Override
	public DataBuffer ensureWritable(int capacity) {
		this.delegate.ensureWritable(capacity);
		return this;
	}

	@Override
	public int readPosition() {
		return this.delegate.readPosition();
	}

	@Override
	public DataBuffer readPosition(int readPosition) {
		this.delegate.readPosition(readPosition);
		return this;
	}

	@Override
	public int writePosition() {
		return this.delegate.writePosition();
	}

	@Override
	public DataBuffer writePosition(int writePosition) {
		this.delegate.writePosition(writePosition);
		return this;
	}

	@Override
	public byte getByte(int index) {
		return this.delegate.getByte(index);
	}

	@Override
	public byte read() {
		return this.delegate.read();
	}

	@Override
	public DataBuffer read(byte[] destination) {
		this.delegate.read(destination);
		return this;
	}

	@Override
	public DataBuffer read(byte[] destination, int offset, int length) {
		this.delegate.read(destination, offset, length);
		return this;
	}

	@Override
	public DataBuffer write(byte b) {
		this.delegate.write(b);
		return this;
	}

	@Override
	public DataBuffer write(byte[] source) {
		this.delegate.write(source);
		return this;
	}

	@Override
	public DataBuffer write(byte[] source, int offset, int length) {
		this.delegate.write(source, offset, length);
		return this;
	}

	@Override
	public DataBuffer write(DataBuffer... buffers) {
		this.delegate.write(buffers);
		return this;
	}

	@Override
	public DataBuffer write(ByteBuffer... buffers) {
		this.delegate.write(buffers);
		return this;
	}

	@Override
	@Deprecated
	public DataBuffer slice(int index, int length) {
		DefaultDataBuffer delegateSlice = this.delegate.slice(index, length);
		if (this.chunk != null) {
			this.chunk.retain();
			return new JettyDataBuffer(this.bufferFactory, delegateSlice, this.chunk);
		}
		else {
			return new JettyDataBuffer(this.bufferFactory, delegateSlice);
		}
	}

	@Override
	public DataBuffer split(int index) {
		DefaultDataBuffer delegateSplit = this.delegate.split(index);
		if (this.chunk != null) {
			this.chunk.retain();
			return new JettyDataBuffer(this.bufferFactory, delegateSplit, this.chunk);
		}
		else {
			return new JettyDataBuffer(this.bufferFactory, delegateSplit);
		}
	}

	@Override
	@Deprecated
	public ByteBuffer asByteBuffer() {
		return this.delegate.asByteBuffer();
	}

	@Override
	@Deprecated
	public ByteBuffer asByteBuffer(int index, int length) {
		return this.delegate.asByteBuffer(index, length);
	}

	@Override
	@Deprecated
	public ByteBuffer toByteBuffer(int index, int length) {
		return this.delegate.toByteBuffer(index, length);
	}

	@Override
	public void toByteBuffer(int srcPos, ByteBuffer dest, int destPos, int length) {
		this.delegate.toByteBuffer(srcPos, dest, destPos, length);
	}

	@Override
	public ByteBufferIterator readableByteBuffers() {
		ByteBufferIterator delegateIterator = this.delegate.readableByteBuffers();
		if (this.chunk != null) {
			return new JettyByteBufferIterator(delegateIterator, this.chunk);
		}
		else {
			return delegateIterator;
		}
	}

	@Override
	public ByteBufferIterator writableByteBuffers() {
		ByteBufferIterator delegateIterator = this.delegate.writableByteBuffers();
		if (this.chunk != null) {
			return new JettyByteBufferIterator(delegateIterator, this.chunk);
		}
		else {
			return delegateIterator;
		}
	}

	@Override
	public String toString(int index, int length, Charset charset) {
		return this.delegate.toString(index, length, charset);
	}

	@Override
	public int hashCode() {
		return this.delegate.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return this == o || (o instanceof JettyDataBuffer other &&
				this.delegate.equals(other.delegate));
	}

	@Override
	public String toString() {
		return String.format("JettyDataBuffer (r: %d, w: %d, c: %d)",
				readPosition(), writePosition(), capacity());
	}

	private static final class JettyByteBufferIterator implements ByteBufferIterator {

		private final ByteBufferIterator delegate;

		private final Content.Chunk chunk;


		public JettyByteBufferIterator(ByteBufferIterator delegate, Content.Chunk chunk) {
			Assert.notNull(delegate, "Delegate must not be null");
			Assert.notNull(chunk, "Chunk must not be null");

			this.delegate = delegate;
			this.chunk = chunk;
			this.chunk.retain();
		}


		@Override
		public void close() {
			this.delegate.close();
			this.chunk.release();
		}

		@Override
		public boolean hasNext() {
			return this.delegate.hasNext();
		}

		@Override
		public ByteBuffer next() {
			return this.delegate.next();
		}
	}

}
