/*
 * Copyright 2002-2019 the original author or authors.
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

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.function.IntPredicate;

import org.springframework.util.Assert;

/**
 * Provides a convenient implementation of the {@link DataBuffer} interface
 * that can be overridden to adapt the delegate.
 *
 * <p>These methods default to calling through to the wrapped delegate object.
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
public class DataBufferWrapper implements DataBuffer {

	private final DataBuffer delegate;


	/**
	 * Create a new {@code DataBufferWrapper} that wraps the given buffer.
	 * @param delegate the buffer to wrap
	 */
	public DataBufferWrapper(DataBuffer delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}

	/**
	 * Return the wrapped delegate.
	 */
	public DataBuffer dataBuffer() {
		return this.delegate;
	}

	@Override
	public DataBufferFactory factory() {
		return this.delegate.factory();
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
	public DataBuffer capacity(int capacity) {
		return this.delegate.capacity(capacity);
	}

	@Override
	public DataBuffer ensureCapacity(int capacity) {
		return this.delegate.ensureCapacity(capacity);
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
	public DataBuffer write(CharSequence charSequence,
			Charset charset) {
		return this.delegate.write(charSequence, charset);
	}

	@Override
	public DataBuffer slice(int index, int length) {
		return this.delegate.slice(index, length);
	}

	@Override
	public DataBuffer retainedSlice(int index, int length) {
		return this.delegate.retainedSlice(index, length);
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
	public String toString(Charset charset) {
		return this.delegate.toString(charset);
	}

	@Override
	public String toString(int index, int length, Charset charset) {
		return this.delegate.toString(index, length, charset);
	}

}
