/*
 * Copyright 2002-2016 the original author or authors.
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
import java.util.function.IntPredicate;

/**
 * Empty {@link DataBuffer} that indicates to the file or the socket writing it
 * that previously buffered data should be flushed.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 * @see FlushingDataBuffer#INSTANCE
 */
public class FlushingDataBuffer implements DataBuffer {

	/** Singleton instance of this class */
	public static final FlushingDataBuffer INSTANCE = new FlushingDataBuffer();

	private final DataBuffer buffer;


	private FlushingDataBuffer() {
		this.buffer = new DefaultDataBufferFactory().allocateBuffer(0);
	}


	@Override
	public DataBufferFactory factory() {
		return this.buffer.factory();
	}

	@Override
	public int indexOf(IntPredicate predicate, int fromIndex) {
		return this.buffer.indexOf(predicate, fromIndex);
	}

	@Override
	public int lastIndexOf(IntPredicate predicate, int fromIndex) {
		return this.buffer.lastIndexOf(predicate, fromIndex);
	}

	@Override
	public int readableByteCount() {
		return this.buffer.readableByteCount();
	}

	@Override
	public byte read() {
		return this.buffer.read();
	}

	@Override
	public DataBuffer read(byte[] destination) {
		return this.buffer.read(destination);
	}

	@Override
	public DataBuffer read(byte[] destination, int offset, int length) {
		return this.buffer.read(destination, offset, length);
	}

	@Override
	public DataBuffer write(byte b) {
		return this.buffer.write(b);
	}

	@Override
	public DataBuffer write(byte[] source) {
		return this.buffer.write(source);
	}

	@Override
	public DataBuffer write(byte[] source, int offset, int length) {
		return this.buffer.write(source, offset, length);
	}

	@Override
	public DataBuffer write(DataBuffer... buffers) {
		return this.buffer.write(buffers);
	}

	@Override
	public DataBuffer write(ByteBuffer... buffers) {
		return this.buffer.write(buffers);
	}

	@Override
	public DataBuffer slice(int index, int length) {
		return this.buffer.slice(index, length);
	}

	@Override
	public ByteBuffer asByteBuffer() {
		return this.buffer.asByteBuffer();
	}

	@Override
	public InputStream asInputStream() {
		return this.buffer.asInputStream();
	}

	@Override
	public OutputStream asOutputStream() {
		return this.buffer.asOutputStream();
	}

}
