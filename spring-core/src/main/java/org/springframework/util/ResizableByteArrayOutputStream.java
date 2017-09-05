/*
 * Copyright 2002-2015 the original author or authors.
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

import java.io.ByteArrayOutputStream;

/**
 * An extension of {@link java.io.ByteArrayOutputStream} that:
 * <ul>
 * <li>has public {@link org.springframework.util.ResizableByteArrayOutputStream#grow(int)}
 * and {@link org.springframework.util.ResizableByteArrayOutputStream#resize(int)} methods
 * to get more control over the size of the internal buffer</li>
 * <li>has a higher initial capacity (256) by default</li>
 * </ul>
 *
 * <p>As of 4.2, this class has been superseded by {@link FastByteArrayOutputStream}
 * for Spring's internal use where no assignability to {@link ByteArrayOutputStream}
 * is needed (since {@link FastByteArrayOutputStream} is more efficient with buffer
 * resize management but doesn't extend the standard {@link ByteArrayOutputStream}).
 *
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 4.0.3
 * @see #resize
 * @see FastByteArrayOutputStream
 */
public class ResizableByteArrayOutputStream extends ByteArrayOutputStream {

	private static final int DEFAULT_INITIAL_CAPACITY = 256;


	/**
	 * Create a new <code>ResizableByteArrayOutputStream</code>
	 * with the default initial capacity of 256 bytes.
	 */
	public ResizableByteArrayOutputStream() {
		super(DEFAULT_INITIAL_CAPACITY);
	}

	/**
	 * Create a new <code>ResizableByteArrayOutputStream</code>
	 * with the specified initial capacity.
	 * @param initialCapacity the initial buffer size in bytes
	 */
	public ResizableByteArrayOutputStream(int initialCapacity) {
		super(initialCapacity);
	}


	/**
	 * Resize the internal buffer size to a specified capacity.
	 * @param targetCapacity the desired size of the buffer
	 * @throws IllegalArgumentException if the given capacity is smaller than
	 * the actual size of the content stored in the buffer already
	 * @see ResizableByteArrayOutputStream#size()
	 */
	public synchronized void resize(int targetCapacity) {
		Assert.isTrue(targetCapacity >= this.count, "New capacity must not be smaller than current size");
		byte[] resizedBuffer = new byte[targetCapacity];
		System.arraycopy(this.buf, 0, resizedBuffer, 0, this.count);
		this.buf = resizedBuffer;
	}

	/**
	 * Grow the internal buffer size.
	 * @param additionalCapacity the number of bytes to add to the current buffer size
	 * @see ResizableByteArrayOutputStream#size()
	 */
	public synchronized void grow(int additionalCapacity) {
		Assert.isTrue(additionalCapacity >= 0, "Additional capacity must be 0 or higher");
		if (this.count + additionalCapacity > this.buf.length) {
			int newCapacity = Math.max(this.buf.length * 2, this.count + additionalCapacity);
			resize(newCapacity);
		}
	}

	/**
	 * Return the current size of this stream's internal buffer.
	 */
	public synchronized int capacity() {
		return this.buf.length;
	}

}
