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
 * Basic abstraction over byte buffers.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface DataBuffer {

	/**
	 * Returns the {@link DataBufferFactory} that created this buffer.
	 * @return the creating buffer factory
	 */
	DataBufferFactory factory();

	/**
	 * Returns the index of the first byte in this buffer that matches the given
	 * predicate.
	 * @param predicate the predicate to match
	 * @param fromIndex the index to start the search from
	 * @return the index of the first byte that matches {@code predicate}; or {@code -1}
	 * if none match
	 */
	int indexOf(IntPredicate predicate, int fromIndex);

	/**
	 * Returns the index of the last byte in this buffer that matches the given
	 * predicate.
	 * @param predicate the predicate to match
	 * @param fromIndex the index to start the search from
	 * @return the index of the last byte that matches {@code predicate}; or {@code -1}
	 * if none match
	 */
	int lastIndexOf(IntPredicate predicate, int fromIndex);

	/**
	 * Returns the number of bytes that can be read from this data buffer.
	 * @return the readable byte count
	 */
	int readableByteCount();

	/**
	 * Reads a single byte from the current reading position of this data buffer.
	 * @return the byte at this buffer's current reading position
	 */
	byte read();

	/**
	 * Reads this buffer's data into the specified destination, starting at the current
	 * reading position of this buffer.
	 *
	 * @param destination the array into which the bytes are to be written
	 * @return this buffer
	 */
	DataBuffer read(byte[] destination);

	/**
	 * Reads at most {@code length} bytes of this buffer into the specified destination,
	 * starting at the current reading position of this buffer.
	 * @param destination the array into which the bytes are to be written
	 * @param offset the index within {@code destination} of the first byte to be written
	 * @param length the maximum number of bytes to be written in {@code destination}
	 * @return this buffer
	 */
	DataBuffer read(byte[] destination, int offset, int length);

	/**
	 * Write a single byte into this buffer at the current writing position.
	 * @param b the byte to be written
	 * @return this buffer
	 */
	DataBuffer write(byte b);

	/**
	 * Writes the given source into this buffer, startin at the current writing position
	 * of this buffer.
	 * @param source the bytes to be written into this buffer
	 * @return this buffer
	 */
	DataBuffer write(byte[] source);

	/**
	 * Writes at most {@code length} bytes of the given source into this buffer, starting
	 * at the current writing position of this buffer.
	 * @param source the bytes to be written into this buffer
	 * @param offset the index withing {@code source} to start writing from
	 * @param length the maximum number of bytes to be written from {@code source}
	 * @return this buffer
	 */
	DataBuffer write(byte[] source, int offset, int length);

	/**
	 * Writes one or more {@code DataBuffer}s to this buffer, starting at the current
	 * writing position.
	 * @param buffers the byte buffers to write into this buffer
	 * @return this buffer
	 */
	DataBuffer write(DataBuffer... buffers);

	/**
	 * Writes one or more {@link ByteBuffer} to this buffer, starting at the current
	 * writing position.
	 * @param buffers the byte buffers to write into this buffer
	 * @return this buffer
	 */
	DataBuffer write(ByteBuffer... buffers);

	/**
	 * Creates a new {@code DataBuffer} whose contents is a shared subsequence of this
	 * data buffer's content.  Data between this data buffer and the returned buffer is
	 * shared; though changes in the returned buffer's position will not be reflected
	 * in the reading nor writing position of this data buffer.
	 * @param index the index at which to start the slice
	 * @param length the length of the slice
	 * @return the specified slice of this data buffer
	 */
	DataBuffer slice(int index, int length);

	/**
	 * Exposes this buffer's bytes as a {@link ByteBuffer}. Data between this {@code
	 * DataBuffer} and the returned {@code ByteBuffer} is shared; though changes in the
	 * returned buffer's {@linkplain ByteBuffer#position() position} will not be reflected
	 * in the reading nor writing position of this data buffer.
	 * @return this data buffer as a byte buffer
	 */
	ByteBuffer asByteBuffer();

	/**
	 * Exposes this buffer's data as an {@link InputStream}. Both data and position are
	 * shared between the returned stream and this data buffer.
	 * @return this data buffer as an input stream
	 */
	InputStream asInputStream();

	/**
	 * Exposes this buffer's data as an {@link OutputStream}. Both data and position are
	 * shared between the returned stream and this data buffer.
	 * @return this data buffer as an output stream
	 */
	OutputStream asOutputStream();

}
