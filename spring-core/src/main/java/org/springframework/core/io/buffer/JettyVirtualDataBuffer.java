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
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntPredicate;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

public sealed class JettyVirtualDataBuffer implements PooledDataBuffer permits JettyDataBuffer {

	final JettyDataBufferFactory bufferFactory;
	private final DataBuffer delegate;
	private final AtomicInteger refCount = new AtomicInteger(1);

	JettyVirtualDataBuffer(JettyDataBufferFactory bufferFactory, DataBuffer delegate) {
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
	@Deprecated(since = "6.0")
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
	@Deprecated(since = "6.0")
	public DataBuffer slice(int index, int length) {
		DataBuffer delegateSlice = this.delegate.slice(index, length);
		return new JettyVirtualDataBuffer(this.bufferFactory, delegateSlice);
	}

	@Override
	public DataBuffer split(int index) {
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
	public void toByteBuffer(int srcPos, ByteBuffer dest, int destPos, int length) {
		this.delegate.toByteBuffer(srcPos, dest, destPos, length);
	}

	@Override
	public ByteBufferIterator readableByteBuffers() {
		return this.delegate.readableByteBuffers();
	}

	@Override
	public ByteBufferIterator writableByteBuffers() {
		return this.delegate.writableByteBuffers();
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
