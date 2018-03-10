/*
 * Copyright 2002-2018 the original author or authors.
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
 * <p>{@code DataBuffer}s has a separate {@linkplain #readPosition() read} and
 * {@linkplain #writePosition() write} position, as opposed to {@code ByteBuffer}'s single
 * {@linkplain ByteBuffer#position() position}. As such, the {@code DataBuffer} does not require
 * a {@linkplain ByteBuffer#flip() flip} to read after writing. In general, the following invariant
 * holds for the read and write positions, and the capacity:
 *
 * <blockquote>
 *     <tt>0</tt> <tt>&lt;=</tt>
 *     <i>readPosition</i> <tt>&lt;=</tt>
 *     <i>writePosition</i> <tt>&lt;=</tt>
 *     <i>capacity</i>
 * </blockquote>
 *
 * <p>The {@linkplain #capacity() capacity} of a {@code DataBuffer} is expanded on demand,
 * similar to {@code StringBuilder}.
 *
 * <p>The main purpose of the {@code DataBuffer} abstraction is to provide a convenient wrapper
 * around {@link ByteBuffer} that is similar to Netty's {@link io.netty.buffer.ByteBuf}, but that
 * can also be used on non-Netty platforms (i.e. Servlet).
 *
 * @author Arjen Poutsma
 * @since 5.0
 * @see DataBufferFactory
 */
public interface DataBuffer {

	/**
	 * Return the {@link DataBufferFactory} that created this buffer.
	 * @return the creating buffer factory
	 */
	DataBufferFactory factory();

	/**
	 * Return the index of the first byte in this buffer that matches the given
	 * predicate.
	 * @param predicate the predicate to match
	 * @param fromIndex the index to start the search from
	 * @return the index of the first byte that matches {@code predicate}; or {@code -1}
	 * if none match
	 */
	int indexOf(IntPredicate predicate, int fromIndex);

	/**
	 * Return the index of the last byte in this buffer that matches the given
	 * predicate.
	 * @param predicate the predicate to match
	 * @param fromIndex the index to start the search from
	 * @return the index of the last byte that matches {@code predicate}; or {@code -1}
	 * if none match
	 */
	int lastIndexOf(IntPredicate predicate, int fromIndex);

	/**
	 * Return the number of bytes that can be read from this data buffer.
	 * @return the readable byte count
	 */
	int readableByteCount();

	/**
	 * Return the number of bytes that can be written to this data buffer.
	 * @return the writable byte count
	 * @since 5.0.1
	 */
	int writableByteCount();

	/**
	 * Return the number of bytes that this buffer can contain.
	 * @return the capacity
	 * @since 5.0.1
	 */
	int capacity();

	/**
	 * Sets the number of bytes that this buffer can contain. If the new capacity is lower than
	 * the current capacity, the contents of this buffer will be truncated. If the new capacity
	 * is higher than the current capacity, it will be expanded.
	 * @param capacity the new capacity
	 * @return this buffer
	 */
	DataBuffer capacity(int capacity);

	/**
	 * Return the position from which this buffer will read.
	 * @return the read position
	 * @since 5.0.1
	 */
	int readPosition();

	/**
	 * Set the position from which this buffer will read.
	 * @param readPosition the new read position
	 * @return this buffer
	 * @throws IndexOutOfBoundsException if {@code readPosition} is smaller than 0 or greater than
	 * {@link #writePosition()}
	 * @since 5.0.1
	 */
	DataBuffer readPosition(int readPosition);

	/**
	 * Return the position to which this buffer will write.
	 * @return the write position
	 * @since 5.0.1
	 */
	int writePosition();

	/**
	 * Set the position to which this buffer will write.
	 * @param writePosition the new write position
	 * @return this buffer
	 * @throws IndexOutOfBoundsException if {@code writePosition} is smaller than
	 * {@link #readPosition()} or greater than {@link #capacity()}
	 * @since 5.0.1
	 */
	DataBuffer writePosition(int writePosition);

	/**
	 * Read a single byte at the given index from this data buffer.
	 * @param index the index at which the byte will be read
	 * @return the byte at the given index
	 * @throws IndexOutOfBoundsException when {@code index} is out of bounds
	 * @since 5.0.4
	 */
	byte getByte(int index);

	/**
	 * Read a single byte from the current reading position from this data buffer.
	 * @return the byte at this buffer's current reading position
	 */
	byte read();

	/**
	 * Read this buffer's data into the specified destination, starting at the current
	 * reading position of this buffer.
	 * @param destination the array into which the bytes are to be written
	 * @return this buffer
	 */
	DataBuffer read(byte[] destination);

	/**
	 * Read at most {@code length} bytes of this buffer into the specified destination,
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
	 * Write the given source into this buffer, startin at the current writing position
	 * of this buffer.
	 * @param source the bytes to be written into this buffer
	 * @return this buffer
	 */
	DataBuffer write(byte[] source);

	/**
	 * Write at most {@code length} bytes of the given source into this buffer, starting
	 * at the current writing position of this buffer.
	 * @param source the bytes to be written into this buffer
	 * @param offset the index withing {@code source} to start writing from
	 * @param length the maximum number of bytes to be written from {@code source}
	 * @return this buffer
	 */
	DataBuffer write(byte[] source, int offset, int length);

	/**
	 * Write one or more {@code DataBuffer}s to this buffer, starting at the current
	 * writing position. It is the responsibility of the caller to
	 * {@linkplain DataBufferUtils#release(DataBuffer) release} the given data buffers.
	 * @param buffers the byte buffers to write into this buffer
	 * @return this buffer
	 */
	DataBuffer write(DataBuffer... buffers);

	/**
	 * Write one or more {@link ByteBuffer} to this buffer, starting at the current
	 * writing position.
	 * @param buffers the byte buffers to write into this buffer
	 * @return this buffer
	 */
	DataBuffer write(ByteBuffer... buffers);

	/**
	 * Create a new {@code DataBuffer} whose contents is a shared subsequence of this
	 * data buffer's content.  Data between this data buffer and the returned buffer is
	 * shared; though changes in the returned buffer's position will not be reflected
	 * in the reading nor writing position of this data buffer.
	 * @param index the index at which to start the slice
	 * @param length the length of the slice
	 * @return the specified slice of this data buffer
	 */
	DataBuffer slice(int index, int length);

	/**
	 * Expose this buffer's bytes as a {@link ByteBuffer}. Data between this
	 * {@code DataBuffer} and the returned {@code ByteBuffer} is shared; though
	 * changes in the returned buffer's {@linkplain ByteBuffer#position() position}
	 * will not be reflected in the reading nor writing position of this data buffer.
	 * @return this data buffer as a byte buffer
	 */
	ByteBuffer asByteBuffer();

	/**
	 * Expose a subsequence of this buffer's bytes as a {@link ByteBuffer}. Data between this
	 * {@code DataBuffer} and the returned {@code ByteBuffer} is shared; though
	 * changes in the returned buffer's {@linkplain ByteBuffer#position() position}
	 * will not be reflected in the reading nor writing position of this data buffer.
	 * @param index the index at which to start the byte buffer
	 * @param length the length of the returned byte buffer
	 * @return this data buffer as a byte buffer
	 * @since 5.0.1
	 */
	ByteBuffer asByteBuffer(int index, int length);

	/**
	 * Expose this buffer's data as an {@link InputStream}. Both data and read position are
	 * shared between the returned stream and this data buffer. The underlying buffer will
	 * <strong>not</strong> be {@linkplain DataBufferUtils#release(DataBuffer) released} when the
	 * input stream is {@linkplain InputStream#close() closed}.
	 * @return this data buffer as an input stream
	 * @see #asInputStream(boolean)
	 */
	InputStream asInputStream();

	/**
	 * Expose this buffer's data as an {@link InputStream}. Both data and read position are
	 * shared between the returned stream and this data buffer.
	 * @param releaseOnClose whether the underlying buffer will be
	 * {@linkplain DataBufferUtils#release(DataBuffer) released} when the input stream is
	 * {@linkplain InputStream#close() closed}.
	 * @return this data buffer as an input stream
	 * @since 5.0.4
	 */
	InputStream asInputStream(boolean releaseOnClose);

	/**
	 * Expose this buffer's data as an {@link OutputStream}. Both data and write position are
	 * shared between the returned stream and this data buffer.
	 * @return this data buffer as an output stream
	 */
	OutputStream asOutputStream();

}
