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
import java.util.concurrent.atomic.AtomicInteger;

import guru.mocker.annotation.mixin.Mixin;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

@Mixin
public sealed class JettyVirtualDataBuffer extends JettyVirtualDataBufferForwarder implements PooledDataBuffer permits JettyDataBuffer {

	final JettyDataBufferFactory bufferFactory;
	final DataBuffer delegate;
	private final AtomicInteger refCount = new AtomicInteger(1);

	JettyVirtualDataBuffer(JettyDataBufferFactory bufferFactory, DataBuffer delegate) {
		super(bufferFactory, delegate);
		Assert.notNull(delegate, "Delegate must not be null");

		this.delegate = delegate;
		this.bufferFactory = bufferFactory;
	}

	// delegation
	@Override
	public boolean isAllocated() {
		return this.refCount.get() > 0;
	}

	@Override
	public PooledDataBuffer retain() {
		incrementRefCount();
		return this;
	}

	int incrementRefCount() {
		return this.refCount.updateAndGet(c -> (c != 0 ? c + 1 : 0));
	}

	@Override
	public PooledDataBuffer touch(Object hint) {
		return this;
	}

	@Override
	public boolean release() {
		int result = decrementRefCount();
		return (result == 0);
	}

	int decrementRefCount() {
		return this.refCount.updateAndGet(c -> {
			if (c != 0) {
				return c - 1;
			}
			else {
				throw new IllegalStateException("JettyDataBuffer already released: " + this);
			}
		});
	}

	@Override
	public DataBufferFactory factory() {
		return this.bufferFactory;
	}

	@Override
	@Deprecated(since = "6.0")
	public DataBuffer capacity(int capacity) {
		this.delegate.capacity(capacity);
		return this;
	}

	@Override
	@Deprecated(since = "6.0")
	public DataBuffer slice(int index, int length) {
		DataBuffer delegateSlice = this.delegate.slice(index, length);
		return new JettyVirtualDataBuffer(this.bufferFactory, delegateSlice);
	}

	@Override
	public JettyVirtualDataBuffer split(int index) {
		DataBuffer delegateSplit = this.delegate.split(index);
		return new JettyVirtualDataBuffer(this.bufferFactory, delegateSplit);
	}

	@Override
	@Deprecated(since = "6.0")
	public ByteBuffer asByteBuffer() {
		return this.delegate.asByteBuffer();
	}

	@Override
	@Deprecated(since = "6.0")
	public ByteBuffer asByteBuffer(int index, int length) {
		return this.delegate.asByteBuffer(index, length);
	}

	@Override
	@Deprecated(since = "6.0.5")
	public ByteBuffer toByteBuffer(int index, int length) {
		return this.delegate.toByteBuffer(index, length);
	}

	@Override
	public int hashCode() {
		return this.delegate.hashCode();
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof JettyVirtualDataBuffer otherBuffer &&
				this.delegate.equals(otherBuffer.delegate)));
	}

	@Override
	public String toString() {
		return String.format("JettyDataBuffer (r: %d, w: %d, c: %d)",
				readPosition(), writePosition(), capacity());
	}
}
