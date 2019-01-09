/*
 * Copyright 2002-2019 the original author or authors.
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
import java.nio.charset.Charset;
import java.util.function.IntPredicate;

import org.springframework.util.Assert;

/**
 * DataBuffer implementation created by {@link LeakAwareDataBufferFactory}.
 *
 * @author Arjen Poutsma
 */
class LeakAwareDataBuffer implements PooledDataBuffer {

	private final DataBuffer delegate;

	private final AssertionError leakError;

	private final LeakAwareDataBufferFactory dataBufferFactory;

	private int refCount = 1;


	LeakAwareDataBuffer(DataBuffer delegate, LeakAwareDataBufferFactory dataBufferFactory) {
		Assert.notNull(delegate, "Delegate must not be null");
		Assert.notNull(dataBufferFactory, "DataBufferFactory must not be null");
		this.delegate = delegate;
		this.dataBufferFactory = dataBufferFactory;
		this.leakError = createLeakError(delegate);
	}

	private static AssertionError createLeakError(DataBuffer delegate) {
		String message = String.format("DataBuffer leak detected: {%s} has not been released.%n" +
				"Stack trace of buffer allocation statement follows:",
				delegate);
		AssertionError result = new AssertionError(message);
		// remove first four irrelevant stack trace elements
		StackTraceElement[] oldTrace = result.getStackTrace();
		StackTraceElement[] newTrace = new StackTraceElement[oldTrace.length - 4];
		System.arraycopy(oldTrace, 4, newTrace, 0, oldTrace.length - 4);
		result.setStackTrace(newTrace);
		return result;
	}

	AssertionError leakError() {
		return this.leakError;
	}

	@Override
	public boolean isAllocated() {
		return this.refCount > 0;
	}

	@Override
	public PooledDataBuffer retain() {
		this.refCount++;
		return this;
	}

	@Override
	public boolean release() {
		this.refCount--;
		return this.refCount == 0;
	}

	// delegation


	@Override
	public LeakAwareDataBufferFactory factory() {
		return this.dataBufferFactory;
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
	public int readPosition() {
		return this.delegate.readPosition();
	}

	@Override
	public DataBuffer readPosition(int readPosition) {
		return this.delegate.readPosition(readPosition);
	}

	@Override
	public int writePosition() {
		return this.delegate.writePosition();
	}

	@Override
	public DataBuffer writePosition(int writePosition) {
		return this.delegate.writePosition(writePosition);
	}

	@Override
	public int capacity() {
		return this.delegate.capacity();
	}

	@Override
	public DataBuffer capacity(int newCapacity) {
		return this.delegate.capacity(newCapacity);
	}

	@Override
	public DataBuffer ensureCapacity(int capacity) {
		return this.delegate.ensureCapacity(capacity);
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
		return this.delegate.read(destination);
	}

	@Override
	public DataBuffer read(byte[] destination, int offset, int length) {
		return this.delegate.read(destination, offset, length);
	}

	@Override
	public DataBuffer write(byte b) {
		return this.delegate.write(b);
	}

	@Override
	public DataBuffer write(byte[] source) {
		return this.delegate.write(source);
	}

	@Override
	public DataBuffer write(byte[] source, int offset, int length) {
		return this.delegate.write(source, offset, length);
	}

	@Override
	public DataBuffer write(DataBuffer... buffers) {
		return this.delegate.write(buffers);
	}

	@Override
	public DataBuffer write(ByteBuffer... buffers) {
		return this.delegate.write(buffers);
	}

	@Override
	public DataBuffer write(CharSequence charSequence, Charset charset) {
		return this.delegate.write(charSequence, charset);
	}

	@Override
	public DataBuffer slice(int index, int length) {
		return this.delegate.slice(index, length);
	}

	@Override
	public ByteBuffer asByteBuffer() {
		return this.delegate.asByteBuffer();
	}

	@Override
	public ByteBuffer asByteBuffer(int index, int length) {
		return this.delegate.asByteBuffer(index, length);
	}

	@Override
	public InputStream asInputStream() {
		return this.delegate.asInputStream();
	}

	@Override
	public InputStream asInputStream(boolean releaseOnClose) {
		return this.delegate.asInputStream(releaseOnClose);
	}

	@Override
	public OutputStream asOutputStream() {
		return this.delegate.asOutputStream();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof LeakAwareDataBuffer) {
			LeakAwareDataBuffer other = (LeakAwareDataBuffer) o;
			return this.delegate.equals(other.delegate);
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.delegate.hashCode();
	}

	@Override
	public String toString() {
		return String.format("LeakAwareDataBuffer (%s)", this.delegate);
	}
}
