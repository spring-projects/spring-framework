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

package org.springframework.http.server.reactive;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntPredicate;

import org.eclipse.jetty.io.Retainable;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.PooledDataBuffer;

/**
 * Adapt an Eclipse Jetty {@link Retainable} to a {@link PooledDataBuffer}
 *
 * @author Greg Wilkins
 * @author Lachlan Roberts
 * @since 6.1.4
 */
public class JettyRetainedDataBuffer implements PooledDataBuffer {
	private final Retainable retainable;

	private final DataBuffer dataBuffer;

	private final AtomicBoolean allocated = new AtomicBoolean(true);

	public JettyRetainedDataBuffer(DataBuffer dataBuffer, Retainable retainable) {
		this.dataBuffer = dataBuffer;
		this.retainable = retainable;
	}

	@Override
	public boolean isAllocated() {
		return allocated.get();
	}

	@Override
	public PooledDataBuffer retain() {
		retainable.retain();
		return this;
	}

	@Override
	public PooledDataBuffer touch(Object hint) {
		return this;
	}

	@Override
	public boolean release() {
		if (retainable.release()) {
			allocated.set(false);
			return true;
		}
		return false;
	}

	@Override
	public DataBufferFactory factory() {
		return dataBuffer.factory();
	}

	@Override
	public int indexOf(IntPredicate predicate, int fromIndex) {
		return dataBuffer.indexOf(predicate, fromIndex);
	}

	@Override
	public int lastIndexOf(IntPredicate predicate, int fromIndex) {
		return dataBuffer.lastIndexOf(predicate, fromIndex);
	}

	@Override
	public int readableByteCount() {
		return dataBuffer.readableByteCount();
	}

	@Override
	public int writableByteCount() {
		return dataBuffer.writableByteCount();
	}

	@Override
	public int capacity() {
		return dataBuffer.capacity();
	}

	@Override
	@Deprecated(since = "6.0")
	public DataBuffer capacity(int capacity) {
		return dataBuffer.capacity(capacity);
	}

	@Override
	@Deprecated(since = "6.0")
	public DataBuffer ensureCapacity(int capacity) {
		return dataBuffer.ensureCapacity(capacity);
	}

	@Override
	public DataBuffer ensureWritable(int capacity) {
		return dataBuffer.ensureWritable(capacity);
	}

	@Override
	public int readPosition() {
		return dataBuffer.readPosition();
	}

	@Override
	public DataBuffer readPosition(int readPosition) {
		return dataBuffer.readPosition(readPosition);
	}

	@Override
	public int writePosition() {
		return dataBuffer.writePosition();
	}

	@Override
	public DataBuffer writePosition(int writePosition) {
		return dataBuffer.writePosition(writePosition);
	}

	@Override
	public byte getByte(int index) {
		return dataBuffer.getByte(index);
	}

	@Override
	public byte read() {
		return dataBuffer.read();
	}

	@Override
	public DataBuffer read(byte[] destination) {
		return dataBuffer.read(destination);
	}

	@Override
	public DataBuffer read(byte[] destination, int offset, int length) {
		return dataBuffer.read(destination, offset, length);
	}

	@Override
	public DataBuffer write(byte b) {
		return dataBuffer.write(b);
	}

	@Override
	public DataBuffer write(byte[] source) {
		return dataBuffer.write(source);
	}

	@Override
	public DataBuffer write(byte[] source, int offset, int length) {
		return dataBuffer.write(source, offset, length);
	}

	@Override
	public DataBuffer write(DataBuffer... buffers) {
		return dataBuffer.write(buffers);
	}

	@Override
	public DataBuffer write(ByteBuffer... buffers) {
		return dataBuffer.write(buffers);
	}

	@Override
	public DataBuffer write(CharSequence charSequence, Charset charset) {
		return dataBuffer.write(charSequence, charset);
	}

	@Override
	@Deprecated(since = "6.0")
	public DataBuffer slice(int index, int length) {
		return dataBuffer.slice(index, length);
	}

	@Override
	@Deprecated(since = "6.0")
	public DataBuffer retainedSlice(int index, int length) {
		return dataBuffer.retainedSlice(index, length);
	}

	@Override
	public DataBuffer split(int index) {
		return dataBuffer.split(index);
	}

	@Override
	@Deprecated(since = "6.0")
	public ByteBuffer asByteBuffer() {
		return dataBuffer.asByteBuffer();
	}

	@Override
	@Deprecated(since = "6.0")
	public ByteBuffer asByteBuffer(int index, int length) {
		return dataBuffer.asByteBuffer(index, length);
	}

	@Override
	@Deprecated(since = "6.0.5")
	public ByteBuffer toByteBuffer() {
		return dataBuffer.toByteBuffer();
	}

	@Override
	@Deprecated(since = "6.0.5")
	public ByteBuffer toByteBuffer(int index, int length) {
		return dataBuffer.toByteBuffer(index, length);
	}

	@Override
	public void toByteBuffer(ByteBuffer dest) {
		dataBuffer.toByteBuffer(dest);
	}

	@Override
	public void toByteBuffer(int srcPos, ByteBuffer dest, int destPos, int length) {
		dataBuffer.toByteBuffer(srcPos, dest, destPos, length);
	}

	@Override
	public ByteBufferIterator readableByteBuffers() {
		return dataBuffer.readableByteBuffers();
	}

	@Override
	public ByteBufferIterator writableByteBuffers() {
		return dataBuffer.writableByteBuffers();
	}

	@Override
	public InputStream asInputStream() {
		return dataBuffer.asInputStream();
	}

	@Override
	public InputStream asInputStream(boolean releaseOnClose) {
		return dataBuffer.asInputStream(releaseOnClose);
	}

	@Override
	public OutputStream asOutputStream() {
		return dataBuffer.asOutputStream();
	}

	@Override
	public String toString(Charset charset) {
		return dataBuffer.toString(charset);
	}

	@Override
	public String toString(int index, int length, Charset charset) {
		return dataBuffer.toString(index, length, charset);
	}
}
