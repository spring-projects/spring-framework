/*
 * Copyright 2002-present the original author or authors.
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

import org.eclipse.jetty.io.Content;

import org.springframework.util.Assert;

/**
 * Implementation of the {@code DataBuffer} interface that can wrap a Jetty {@link Content.Chunk}. Typically constructed
 * with {@link JettyDataBufferFactory}.
 *
 * @author Greg Wilkins
 * @author Lachlan Roberts
 * @author Arjen Poutsma
 * @since 6.2
 */
public final class JettyDataBuffer extends JettyVirtualDataBuffer {

	private final Content.Chunk chunk;

	JettyDataBuffer(JettyDataBufferFactory bufferFactory, DataBuffer delegate, Content.Chunk chunk) {
		super(bufferFactory, delegate);

		Assert.notNull(chunk, "Chunk must not be null");

		this.chunk = chunk;
	}

	@Override
	public PooledDataBuffer retain() {
		int result = incrementRefCount();
		if (result != 0) {
			this.chunk.retain();
		}
		return this;
	}

	@Override
	public boolean release() {
		decrementRefCount();
		return this.chunk.release();
	}

	@Override
	@Deprecated(since = "6.0")
	public DataBuffer slice(int index, int length) {
		DataBuffer delegateSlice = super.slice(index, length);
		this.chunk.retain();
		return new JettyDataBuffer(this.bufferFactory, delegateSlice, this.chunk);
	}

	@Override
	public DataBuffer split(int index) {
		DataBuffer delegateSplit = super.split(index);
		this.chunk.retain();
		return new JettyDataBuffer(this.bufferFactory, delegateSplit, this.chunk);
	}


	@Override
	public ByteBufferIterator readableByteBuffers() {
		ByteBufferIterator delegateIterator = super.readableByteBuffers();
		return new JettyByteBufferIterator(delegateIterator, this.chunk);
	}

	@Override
	public ByteBufferIterator writableByteBuffers() {
		ByteBufferIterator delegateIterator = super.writableByteBuffers();
		return new JettyByteBufferIterator(delegateIterator, this.chunk);
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
