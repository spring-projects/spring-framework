/*
 * Copyright 2002-2014 the original author or authors.
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
package org.springframework.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A variation of {@link java.io.ByteArrayOutputStream} that:
 * <ul>
 * <li>has public {@link org.springframework.util.ResizableByteArrayOutputStream#grow(int)} and
 * {@link org.springframework.util.ResizableByteArrayOutputStream#resize(int)} methods to get more control
 * over the the size of the internal buffer</li>
 * <li>does not synchronize on buffer access - so this class should not be used if concurrent access
 * to the buffer is expected</li>
 * </ul>
 *
 * @author Brian Clozel
 * @since 4.0
 */
public class ResizableByteArrayOutputStream extends OutputStream {

	private static final int INITIAL_BUFFER_SIZE = 32;

	protected byte[] buffer;

	protected int count;

	/**
	 * Create a new <code>ByteArrayOutputStream</code> with the default buffer size of 32 bytes.
	 */
	public ResizableByteArrayOutputStream() {
		this(INITIAL_BUFFER_SIZE);
	}

	/**
	 * Create a new <code>ByteArrayOutputStream</code> with a specified initial buffer size.
	 *
	 * @param size The initial buffer size in bytes
	 */
	public ResizableByteArrayOutputStream(int size) {
		buffer = new byte[size];
		count = 0;
	}

	/**
	 * Return the size of the internal buffer.
	 */
	public int size() {
		return buffer.length;
	}

	/**
	 * Return the number of bytes that have been written to the buffer so far.
	 */
	public int count() {
		return count;
	}

	/**
	 * Discard all bytes written to the internal buffer by setting the <code>count</code> variable to 0.
	 */
	public void reset() {
		count = 0;
	}

	/**
	 * Grow the internal buffer size
	 * @param add number of bytes to add to the current buffer size
	 * @see ResizableByteArrayOutputStream#size()
	 */
	public void grow(int add) {
		if (count + add > buffer.length) {
			int newlen = Math.max(buffer.length * 2, count + add);
			resize(newlen);
		}
	}

	/**
	 * Resize the internal buffer size to a specified value
	 * @param size the size of the buffer
	 * @throws java.lang.IllegalArgumentException if the given size is
	 * smaller than the actual size of the content stored in the buffer
	 * @see ResizableByteArrayOutputStream#size()
	 */
	public void resize(int size) {
		Assert.isTrue(size >= count);

		byte[] newbuf = new byte[size];
		System.arraycopy(buffer, 0, newbuf, 0, count);
		buffer = newbuf;
	}

	/**
	 * Write the specified byte into the internal buffer, thus incrementing the
	 * {{@link org.springframework.util.ResizableByteArrayOutputStream#count()}}
	 * @param oneByte the byte to be written in the buffer
	 * @see ResizableByteArrayOutputStream#count()
	 */
	public void write(int oneByte) {
		grow(1);
		buffer[count++] = (byte) oneByte;
	}

	/**
	 * Write <code>add</code> bytes from the passed in array
	 * <code>inBuffer</code> starting at index <code>offset</code> into the
	 * internal buffer.
	 *
	 * @param inBuffer The byte array to write data from
	 * @param offset The index into the buffer to start writing data from
	 * @param add The number of bytes to write
	 * @see ResizableByteArrayOutputStream#count()
	 */
	public void write(byte[] inBuffer, int offset, int add) {
		if (add >= 0) {
			grow(add);
		}
		System.arraycopy(inBuffer, offset, buffer, count, add);
		count += add;
	}

	/**
	 * Write all bytes that have been written to the specified <code>OutputStream</code>.
	 *
	 * @param out The <code>OutputStream</code> to write to
	 * @exception IOException If an error occurs
	 */
	public void writeTo(OutputStream out) throws IOException {
		out.write(buffer, 0, count);
	}

	/**
	 * Return a byte array containing the bytes that have been written to this stream so far.
	 */
	public byte[] toByteArray() {
		byte[] ret = new byte[count];
		System.arraycopy(buffer, 0, ret, 0, count);
		return ret;
	}

}
