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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntPredicate;

import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.io.Retainable;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.PooledDataBuffer;

/**
 * Adapt an Eclipse Jetty {@link Retainable} to a {@link PooledDataBuffer}.
 *
 * @author Greg Wilkins
 * @author Lachlan Roberts
 * @since 6.1.4
 */
public class JettyRetainedDataBuffer implements PooledDataBuffer {

	private final Content.Chunk chunk;

	private final DataBuffer dataBuffer;

	private final AtomicInteger allocated = new AtomicInteger(1);


	public JettyRetainedDataBuffer(DataBufferFactory dataBufferFactory, Content.Chunk chunk) {
		this.chunk = chunk;
		// this.dataBuffer = dataBufferFactory.wrap(BufferUtil.copy(chunk.getByteBuffer())); // TODO this copy avoids multipart bugs
		this.dataBuffer = dataBufferFactory.wrap(chunk.getByteBuffer()); // TODO avoid double slice?
		this.chunk.retain();
	}

	@Override
	public boolean isAllocated() {
		return this.allocated.get() >= 1;
	}

	@Override
	public PooledDataBuffer retain() {
		if (this.allocated.updateAndGet(c -> c >= 1 ? c + 1 : c) < 1) {
			throw new IllegalStateException("released");
		}
		return this;
	}

	@Override
	public PooledDataBuffer touch(Object hint) {
		return this;
	}

	@Override
	public boolean release() {
		if (this.allocated.decrementAndGet() == 0) {
			this.chunk.release();
			return true;
		}
		return false;
	}

	@Override
	public DataBufferFactory factory() {
		return this.dataBuffer.factory();
	}

	@Override
	public int indexOf(IntPredicate predicate, int fromIndex) {
		return this.dataBuffer.indexOf(predicate, fromIndex);
	}

	@Override
	public int lastIndexOf(IntPredicate predicate, int fromIndex) {
		return this.dataBuffer.lastIndexOf(predicate, fromIndex);
	}

	@Override
	public int readableByteCount() {
		return this.dataBuffer.readableByteCount();
	}

	@Override
	public int writableByteCount() {
		return this.dataBuffer.writableByteCount();
	}

	@Override
	public int capacity() {
		return this.dataBuffer.capacity();
	}

	@Override
	@Deprecated(since = "6.0")
	public DataBuffer capacity(int capacity) {
		return this.dataBuffer.capacity(capacity);
	}

	@Override
	@Deprecated(since = "6.0")
	public DataBuffer ensureCapacity(int capacity) {
		return this.dataBuffer.ensureCapacity(capacity);
	}

	@Override
	public DataBuffer ensureWritable(int capacity) {
		return this.dataBuffer.ensureWritable(capacity);
	}

	@Override
	public int readPosition() {
		return this.dataBuffer.readPosition();
	}

	@Override
	public DataBuffer readPosition(int readPosition) {
		return this.dataBuffer.readPosition(readPosition);
	}

	@Override
	public int writePosition() {
		return this.dataBuffer.writePosition();
	}

	@Override
	public DataBuffer writePosition(int writePosition) {
		return this.dataBuffer.writePosition(writePosition);
	}

	@Override
	public byte getByte(int index) {
		return this.dataBuffer.getByte(index);
	}

	@Override
	public byte read() {
		return this.dataBuffer.read();
	}

	@Override
	public DataBuffer read(byte[] destination) {
		return this.dataBuffer.read(destination);
	}

	@Override
	public DataBuffer read(byte[] destination, int offset, int length) {
		return this.dataBuffer.read(destination, offset, length);
	}

	@Override
	public DataBuffer write(byte b) {
		return this.dataBuffer.write(b);
	}

	@Override
	public DataBuffer write(byte[] source) {
		return this.dataBuffer.write(source);
	}

	@Override
	public DataBuffer write(byte[] source, int offset, int length) {
		return this.dataBuffer.write(source, offset, length);
	}

	@Override
	public DataBuffer write(DataBuffer... buffers) {
		return this.dataBuffer.write(buffers);
	}

	@Override
	public DataBuffer write(ByteBuffer... buffers) {
		return this.dataBuffer.write(buffers);
	}

	@Override
	public DataBuffer write(CharSequence charSequence, Charset charset) {
		return this.dataBuffer.write(charSequence, charset);
	}

	@Override
	@Deprecated(since = "6.0")
	public DataBuffer slice(int index, int length) {
		return this.dataBuffer.slice(index, length);
	}

	@Override
	@Deprecated(since = "6.0")
	public DataBuffer retainedSlice(int index, int length) {
		return this.dataBuffer.retainedSlice(index, length);
	}

	@Override
	public DataBuffer split(int index) {
		return this.dataBuffer.split(index);
	}

	@Override
	@Deprecated(since = "6.0")
	public ByteBuffer asByteBuffer() {
		return this.dataBuffer.asByteBuffer();
	}

	@Override
	@Deprecated(since = "6.0")
	public ByteBuffer asByteBuffer(int index, int length) {
		return this.dataBuffer.asByteBuffer(index, length);
	}

	@Override
	@Deprecated(since = "6.0.5")
	public ByteBuffer toByteBuffer() {
		return this.dataBuffer.toByteBuffer();
	}

	@Override
	@Deprecated(since = "6.0.5")
	public ByteBuffer toByteBuffer(int index, int length) {
		return this.dataBuffer.toByteBuffer(index, length);
	}

	@Override
	public void toByteBuffer(ByteBuffer dest) {
		this.dataBuffer.toByteBuffer(dest);
	}

	@Override
	public void toByteBuffer(int srcPos, ByteBuffer dest, int destPos, int length) {
		this.dataBuffer.toByteBuffer(srcPos, dest, destPos, length);
	}

	@Override
	public ByteBufferIterator readableByteBuffers() {
		return this.dataBuffer.readableByteBuffers();
	}

	@Override
	public ByteBufferIterator writableByteBuffers() {
		return this.dataBuffer.writableByteBuffers();
	}

	@Override
	public InputStream asInputStream() {
		return this.dataBuffer.asInputStream();
	}

	@Override
	public InputStream asInputStream(boolean releaseOnClose) {
		return this.dataBuffer.asInputStream(releaseOnClose);
	}

	@Override
	public OutputStream asOutputStream() {
		return this.dataBuffer.asOutputStream();
	}

	@Override
	public String toString(Charset charset) {
		return this.dataBuffer.toString(charset);
	}

	@Override
	public String toString(int index, int length, Charset charset) {
		return this.dataBuffer.toString(index, length, charset);
	}
}
