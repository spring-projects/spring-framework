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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A speedy alternative to {@link java.io.ByteArrayOutputStream}.
 * <p>Unlike {@link java.io.ByteArrayOutputStream}, this implementation is backed by a
 * {@link java.util.LinkedList} of byte[] instead of 1 constantly resizing byte[].
 * It does not copy buffers when it's expanded.</p>
 *
 * <p>The initial buffer is only created when the stream is first written.
 * There's also no copying of the internal buffer if its contents is extracted with the
 * {@link #writeTo(OutputStream)} method.
 * Instances of this class are NOT THREAD SAFE.</p>
 *
 * @author Craig Andrews
 * @since 4.2
 */
public final class FastByteArrayOutputStream extends OutputStream {

	private static final int DEFAULT_BLOCK_SIZE = 256;

	// the buffers used to store the content bytes
	private final LinkedList<byte[]> buffers = new LinkedList<byte[]>();

	// is the stream closed?
	private boolean closed = false;

	// the size, in bytes, to use when allocating the next next byte[]
	private int nextBlockSize;

	// the index in the byte[] found at buffers.getLast() to be written next
	private int index = 0;

	// number of bytes in previous buffers
	// the number of bytes in the current buffer is in index
	private int alreadyBufferedSize = 0;

	// the size, in bytes, to use when allocating the first byte[]
	private final int initialBlockSize;

	/**
	 * Create a new <code>FastByteArrayOutputStream</code>
	 * with the default initial capacity of {@value #DEFAULT_BLOCK_SIZE} bytes.
	 */
	public FastByteArrayOutputStream() {
		this(DEFAULT_BLOCK_SIZE);
	}

	/**
	 * Create a new <code>FastByteArrayOutputStream</code>
	 * with the specified initial capacity.
	 * @param initialBlockSize the initial buffer size in bytes
	 */
	public FastByteArrayOutputStream(int initialBlockSize) {
		Assert.isTrue(initialBlockSize > 0, "Initial block size must be greater than 0");
		this.initialBlockSize = initialBlockSize;
		nextBlockSize = initialBlockSize;
	}

	@Override
	public void write(int datum) throws IOException {
		if (closed) {
			throw new IOException("Stream closed");
		}
		else {
			if (buffers.peekLast() == null || buffers.getLast().length == index) {
				addBuffer(1);
			}
			// store the byte
			buffers.getLast()[index++] = (byte) datum;
		}
	}

	@Override
	public void write(byte[] data, int offset, int length) throws IOException {
		if (data == null) {
			throw new NullPointerException();
		}
		else if ((offset < 0) || ((offset + length) > data.length) || (length < 0)) {
			throw new IndexOutOfBoundsException();
		}
		else if (closed) {
			throw new IOException("Stream closed");
		}
		else {
			if (buffers.peekLast() == null || buffers.getLast().length == index) {
				addBuffer(length);
			}
			if ((index + length) > buffers.getLast().length) {
				do {
					if (index == buffers.getLast().length) {
						addBuffer(length);
					}
					int copyLength = buffers.getLast().length - index;
					if (length < copyLength) {
						copyLength = length;
					}
					System.arraycopy(data, offset, buffers.getLast(), index, copyLength);
					offset += copyLength;
					index += copyLength;
					length -= copyLength;
				} while (length > 0);
			}
			else {
				// Copy in the subarray
				System.arraycopy(data, offset, buffers.getLast(), index, length);
				index += length;
			}
		}
	}

	@Override
	public void close() {
		closed = true;
	}

	/**
	 * Returns the number of bytes stored in this <code>FastByteArrayOutputStream</code>
	 */
	public int size() {
		return alreadyBufferedSize + index;
	}

	/**
	 * Convert the stream's data to a byte array and return the byte array.
	 *
	 * <p>Also replaces the internal structures with the byte array to conserve memory:
	 * if the byte array is being made anyways, mind as well as use it.
	 * This approach also means that if this method is called twice without any writes in between,
	 * the second call is a no-op.
	 * This method is "unsafe" as it returns the internal buffer - callers should not modify the returned buffer.</p>
	 *
	 * @return the current contents of this output stream, as a byte array.
	 * @see #size()
	 * @see #toByteArray()
	 */
	public byte[] toByteArrayUnsafe() {
		int totalSize = size();
		if (totalSize == 0) {
			return new byte[0];
		}
		resize(totalSize);
		return buffers.getFirst();
	}

	/**
	 * Creates a newly allocated byte array.
	 *
	 * <p>Its size is the current
	 * size of this output stream and the valid contents of the buffer
	 * have been copied into it.</p>
	 *
	 * @return the current contents of this output stream, as a byte array.
	 * @see #size()
	 * @see #toByteArrayUnsafe()
	 */
	public byte[] toByteArray() {
		byte[] bytesUnsafe = toByteArrayUnsafe();
		byte[] ret = new byte[bytesUnsafe.length];
		System.arraycopy(bytesUnsafe, 0, ret, 0, bytesUnsafe.length);
		return ret;
	}

	/**
	 * Resets the contents of this <code>FastByteArrayOutputStreamInputStream</code>
	 * <p>All currently accumulated output in the output stream is discarded.
	 * The output stream can be used again.</p>
	 */
	public void reset() {
		buffers.clear();
		nextBlockSize = initialBlockSize;
		closed = false;
		index = 0;
		alreadyBufferedSize = 0;
	}

	/**
	 * Get an {@link java.io.InputStream} to retrieve the data in this OutputStream
	 *
	 * <p>Note that if any methods are called on the OutputStream
	 * (including, but not limited to, any of the write methods, {@link #reset()},
	 * {@link #toByteArray()}, and {@link #toByteArrayUnsafe()}) then the {@link java.io.InputStream}'s
	 * behavior is undefined.</p>
	 *
	 * @return {@link java.io.InputStream} of the contents of this <code>FastByteArrayOutputStreamInputStream</code>
	 */
	public InputStream getInputStream() {
		return new FastByteArrayOutputStreamInputStream(this);
	}

	/**
	 * Write the buffers content to the given OutputStream
	 *
	 * @param out the OutputStream to write to
	 */
	public void writeTo(OutputStream out) throws IOException {
		Iterator<byte[]> iter = buffers.iterator();

		while (iter.hasNext()) {
			byte[] bytes = iter.next();
			if (iter.hasNext()) {
				out.write(bytes, 0, bytes.length);
			}
			else {
				out.write(bytes, 0, index);
			}
		}
	}

	/**
	 * Resize the internal buffer size to a specified capacity.
	 *
	 * @param targetCapacity the desired size of the buffer
	 * @throws IllegalArgumentException if the given capacity is smaller than
	 * the actual size of the content stored in the buffer already
	 * @see FastByteArrayOutputStream#size()
	 */
	public void resize(int targetCapacity) {
		Assert.isTrue(targetCapacity >= size(), "New capacity must not be smaller than current size");
		if (buffers.peekFirst() == null) {
			nextBlockSize = targetCapacity - size();
		}
		else if (size() == targetCapacity && buffers.getFirst().length == targetCapacity) {
			// do nothing - already at the targetCapacity
		}
		else {
			int totalSize = size();
			byte[] data = new byte[targetCapacity];
			int pos = 0;
			Iterator<byte[]> iter = buffers.iterator();
			while (iter.hasNext()) {
				byte[] bytes = iter.next();
				if (iter.hasNext()) {
					System.arraycopy(bytes, 0, data, pos, bytes.length);
					pos += bytes.length;
				}
				else {
					System.arraycopy(bytes, 0, data, pos, index);
				}
			}
			buffers.clear();
			buffers.add(data);
			index = totalSize;
			alreadyBufferedSize = 0;
		}
	}

	/**
	 * Create a new buffer and store it in the LinkedList
	 *
	 * <p>Adds a new buffer that can store at least {@code minCapacity} bytes</p>
	 */
	private void addBuffer(int minCapacity) {
		if (buffers.peekLast() != null) {
			alreadyBufferedSize += index;
			index = 0;
		}
		if (nextBlockSize < minCapacity) {
			nextBlockSize = nextPowerOf2(minCapacity);
		}
		buffers.add(new byte[nextBlockSize]);
		nextBlockSize *= 2; // block size doubles each time
	}

	/**
	 * Get the next power of 2 of a number (ex, the next power of 2 of 119 is 128)
	 */
	private static final int nextPowerOf2(int val) {
		val--;
		val = (val >> 1) | val;
		val = (val >> 2) | val;
		val = (val >> 4) | val;
		val = (val >> 8) | val;
		val = (val >> 16) | val;
		val++;
		return val;
	}

	/**
	 * Converts the buffer's contents into a string decoding bytes using the
	 * platform's default character set. The length of the new <tt>String</tt>
	 * is a function of the character set, and hence may not be equal to the
	 * size of the buffer.
	 *
	 * <p>This method always replaces malformed-input and unmappable-character
	 * sequences with the default replacement string for the platform's
	 * default character set. The {@linkplain java.nio.charset.CharsetDecoder}
	 * class should be used when more control over the decoding process is
	 * required.</p>
	 *
	 * @return String decoded from the buffer's contents.
	 */
	@Override
	public String toString() {
		return new String(toByteArrayUnsafe());
	}


	/**
	 * An implementation of {@link java.io.InputStream} that reads from <code>FastByteArrayOutputStream</code>
	 * Instances of this class are NOT THREAD SAFE.
	 */
	private static final class FastByteArrayOutputStreamInputStream extends UpdateMessageDigestInputStream {
		int totalBytesRead = 0;

		int nextIndexInCurrentBuffer = 0;

		final Iterator<byte[]> buffersIterator;

		byte[] currentBuffer;

		int currentBufferLength;

		final FastByteArrayOutputStream fastByteArrayOutputStream;

		/**
		 * Create a new <code>FastByteArrayOutputStreamInputStream</code> backed
		 * by the given <code>FastByteArrayOutputStream</code>
		 */
		public FastByteArrayOutputStreamInputStream(FastByteArrayOutputStream fastByteArrayOutputStream) {
			this.fastByteArrayOutputStream = fastByteArrayOutputStream;
			buffersIterator = fastByteArrayOutputStream.buffers.iterator();
			if (buffersIterator.hasNext()) {
				currentBuffer = buffersIterator.next();
				if (currentBuffer == fastByteArrayOutputStream.buffers.getLast()) {
					currentBufferLength = fastByteArrayOutputStream.index;
				}
				else {
					currentBufferLength = currentBuffer.length;
				}
			}
			else {
				currentBuffer = null;
			}
		}

		@Override
		public int read() {
			if (currentBuffer == null) {
				// this stream doesn't have any data in it
				return -1;
			}
			else {
				if (nextIndexInCurrentBuffer < currentBufferLength) {
					totalBytesRead++;
					return currentBuffer[nextIndexInCurrentBuffer++];
				}
				else {
					if (buffersIterator.hasNext()) {
						currentBuffer = buffersIterator.next();
						if (currentBuffer == fastByteArrayOutputStream.buffers.getLast()) {
							currentBufferLength = fastByteArrayOutputStream.index;
						}
						else {
							currentBufferLength = currentBuffer.length;
						}
						nextIndexInCurrentBuffer = 0;
					}
					else {
						currentBuffer = null;
					}
					return read();
				}
			}
		}

		@Override
		public int read(byte[] b) {
			return read(b, 0, b.length);
		}

		@Override
		public int read(byte[] b, int off, int len) {
			if (b == null) {
				throw new NullPointerException();
			}
			else if (off < 0 || len < 0 || len > b.length - off) {
				throw new IndexOutOfBoundsException();
			}
			else if (len == 0) {
				return 0;
			}
			else if (len < 0) {
				throw new IllegalArgumentException("len must be 0 or greater: " + len);
			}
			else if (off < 0) {
				throw new IllegalArgumentException("off must be 0 or greater: " + off);
			}
			else {
				if (currentBuffer == null) {
					// this stream doesn't have any data in it
					return 0;
				}
				else {
					if (nextIndexInCurrentBuffer < currentBufferLength) {
						int bytesToCopy = Math.min(len, currentBufferLength - nextIndexInCurrentBuffer);
						System.arraycopy(currentBuffer, nextIndexInCurrentBuffer, b, off, bytesToCopy);
						totalBytesRead += bytesToCopy;
						nextIndexInCurrentBuffer += bytesToCopy;
						return bytesToCopy + read(b, off + bytesToCopy, len - bytesToCopy);
					}
					else {
						if (buffersIterator.hasNext()) {
							currentBuffer = buffersIterator.next();
							if (currentBuffer == fastByteArrayOutputStream.buffers.getLast()) {
								currentBufferLength = fastByteArrayOutputStream.index;
							}
							else {
								currentBufferLength = currentBuffer.length;
							}
							nextIndexInCurrentBuffer = 0;
						}
						else {
							currentBuffer = null;
						}
						return read(b, off, len);
					}
				}
			}
		}

		@Override
		public long skip(long n) throws IOException {
			if (n > Integer.MAX_VALUE) {
				throw new IllegalArgumentException("n exceeds maximum (" +
						Integer.MAX_VALUE + "): " + n);
			}
			else if (n == 0) {
				return 0;
			}
			else if (n < 0) {
				throw new IllegalArgumentException("n must be 0 or greater: " + n);
			}
			int len = (int) n;
			if (currentBuffer == null) {
				// this stream doesn't have any data in it
				return 0;
			}
			else {
				if (nextIndexInCurrentBuffer < currentBufferLength) {
					int bytesToSkip = Math.min(len, currentBufferLength - nextIndexInCurrentBuffer);
					totalBytesRead += bytesToSkip;
					nextIndexInCurrentBuffer += bytesToSkip;
					return bytesToSkip + skip(len - bytesToSkip);
				}
				else {
					if (buffersIterator.hasNext()) {
						currentBuffer = buffersIterator.next();
						if (currentBuffer == fastByteArrayOutputStream.buffers.getLast()) {
							currentBufferLength = fastByteArrayOutputStream.index;
						}
						else {
							currentBufferLength = currentBuffer.length;
						}
						nextIndexInCurrentBuffer = 0;
					}
					else {
						currentBuffer = null;
					}
					return skip(len);
				}
			}
		}

		@Override
		public int available() {
			return fastByteArrayOutputStream.size() - totalBytesRead;
		}

		/**
		 * Update the message digest with the remaining bytes in this stream.
		 *
		 * @param messageDigest The message digest to update
		 */
		public void updateMessageDigest(MessageDigest messageDigest) {
			updateMessageDigest(messageDigest, available());
		}

		/**
		 * Update the message digest with the next len bytes in this stream.
		 * Avoids creating new byte arrays and use internal buffers for performance.
		 * @param messageDigest The message digest to update
		 * @param len how many bytes to read from this stream and use to update the message digest
		 */
		public void updateMessageDigest(MessageDigest messageDigest, int len) {
			if (currentBuffer == null) {
				// this stream doesn't have any data in it
				return;
			}
			else if (len == 0) {
				return;
			}
			else if (len < 0) {
				throw new IllegalArgumentException("len must be 0 or greater: " + len);
			}
			else {
				if (nextIndexInCurrentBuffer < currentBufferLength) {
					int bytesToCopy = Math.min(len, currentBufferLength - nextIndexInCurrentBuffer);
					messageDigest.update(currentBuffer, nextIndexInCurrentBuffer, bytesToCopy);
					nextIndexInCurrentBuffer += bytesToCopy;
					updateMessageDigest(messageDigest, len - bytesToCopy);
				}
				else {
					if (buffersIterator.hasNext()) {
						currentBuffer = buffersIterator.next();
						if (currentBuffer == fastByteArrayOutputStream.buffers.getLast()) {
							currentBufferLength = fastByteArrayOutputStream.index;
						}
						else {
							currentBufferLength = currentBuffer.length;
						}
						nextIndexInCurrentBuffer = 0;
					}
					else {
						currentBuffer = null;
					}
					updateMessageDigest(messageDigest, len);
				}
			}
		}
	}
}
