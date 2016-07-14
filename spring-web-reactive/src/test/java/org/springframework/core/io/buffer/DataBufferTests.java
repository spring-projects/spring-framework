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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class DataBufferTests extends AbstractDataBufferAllocatingTestCase {

	@Test
	public void writeAndRead() {

		DataBuffer buffer = createDataBuffer(5);
		buffer.write(new byte[]{'a', 'b', 'c'});

		int ch = buffer.read();
		assertEquals('a', ch);

		buffer.write((byte) 'd');
		buffer.write((byte) 'e');

		byte[] result = new byte[4];
		buffer.read(result);

		assertArrayEquals(new byte[]{'b', 'c', 'd', 'e'}, result);

		release(buffer);
	}

	@Test
	public void inputStream() throws IOException {
		byte[] data = new byte[]{'a', 'b', 'c', 'd', 'e'};

		DataBuffer buffer = createDataBuffer(4);
		buffer.write(data);

		buffer.read(); // readIndex++

		InputStream inputStream = buffer.asInputStream();

		int available = inputStream.available();
		assertEquals(4, available);

		int result = inputStream.read();
		assertEquals('b', result);

		available = inputStream.available();
		assertEquals(3, available);

		byte[] bytes = new byte[2];
		int len = inputStream.read(bytes);
		assertEquals(2, len);
		assertArrayEquals(new byte[]{'c', 'd'}, bytes);

		Arrays.fill(bytes, (byte) 0);
		len = inputStream.read(bytes);
		assertEquals(1, len);
		assertArrayEquals(new byte[]{'e', (byte) 0}, bytes);

		release(buffer);
	}

	@Test
	public void outputStream() throws IOException {
		DataBuffer buffer = createDataBuffer(4);
		buffer.write((byte) 'a');

		OutputStream outputStream = buffer.asOutputStream();
		outputStream.write(new byte[]{'b', 'c', 'd'});

		buffer.write((byte) 'e');

		byte[] bytes = new byte[5];
		buffer.read(bytes);
		assertArrayEquals(new byte[]{'a', 'b', 'c', 'd', 'e'}, bytes);

		release(buffer);
	}

	@Test
	public void expand() {
		DataBuffer buffer = createDataBuffer(1);
		buffer.write((byte) 'a');
		buffer.write((byte) 'b');

		byte[] result = new byte[2];
		buffer.read(result);
		assertArrayEquals(new byte[]{'a', 'b'}, result);

		buffer.write(new byte[]{'c', 'd'});

		result = new byte[2];
		buffer.read(result);
		assertArrayEquals(new byte[]{'c', 'd'}, result);

		release(buffer);
	}

	@Test
	public void writeByteBuffer() {
		DataBuffer buffer1 = createDataBuffer(1);
		buffer1.write((byte) 'a');
		ByteBuffer buffer2 = createByteBuffer(2);
		buffer2.put((byte) 'b');
		buffer2.flip();
		ByteBuffer buffer3 = createByteBuffer(3);
		buffer3.put((byte) 'c');
		buffer3.flip();

		buffer1.write(buffer2, buffer3);
		buffer1.write((byte) 'd'); // make sure the write index is correctly set

		assertEquals(4, buffer1.readableByteCount());
		byte[] result = new byte[4];
		buffer1.read(result);

		assertArrayEquals(new byte[]{'a', 'b', 'c', 'd'}, result);

		release(buffer1);
	}

	private ByteBuffer createByteBuffer(int capacity) {
		return ByteBuffer.allocate(capacity);
	}

	@Test
	public void writeDataBuffer() {
		DataBuffer buffer1 = createDataBuffer(1);
		buffer1.write((byte) 'a');
		DataBuffer buffer2 = createDataBuffer(2);
		buffer2.write((byte) 'b');
		DataBuffer buffer3 = createDataBuffer(3);
		buffer3.write((byte) 'c');

		buffer1.write(buffer2, buffer3);
		buffer1.write((byte) 'd'); // make sure the write index is correctly set

		assertEquals(4, buffer1.readableByteCount());
		byte[] result = new byte[4];
		buffer1.read(result);

		assertArrayEquals(new byte[]{'a', 'b', 'c', 'd'}, result);

		release(buffer1);
	}

	@Test
	public void asByteBuffer() {
		DataBuffer buffer = createDataBuffer(4);
		buffer.write(new byte[]{'a', 'b', 'c'});
		buffer.read(); // skip a

		ByteBuffer result = buffer.asByteBuffer();

		buffer.write((byte) 'd');
		assertEquals(2, result.remaining());
		byte[] resultBytes = new byte[2];
		buffer.read(resultBytes);
		assertArrayEquals(new byte[]{'b', 'c'}, resultBytes);

		release(buffer);
	}

	@Test
	public void indexOf() {
		DataBuffer buffer = createDataBuffer(3);
		buffer.write(new byte[]{'a', 'b', 'c'});

		int result = buffer.indexOf(b -> b == 'c', 0);
		assertEquals(2, result);

		result = buffer.indexOf(b -> b == 'c', Integer.MIN_VALUE);
		assertEquals(2, result);

		result = buffer.indexOf(b -> b == 'c', Integer.MAX_VALUE);
		assertEquals(-1, result);

		result = buffer.indexOf(b -> b == 'z', 0);
		assertEquals(-1, result);

		release(buffer);
	}

	@Test
	public void lastIndexOf() {
		DataBuffer buffer = createDataBuffer(3);
		buffer.write(new byte[]{'a', 'b', 'c'});

		int result = buffer.lastIndexOf(b -> b == 'b', 3);
		assertEquals(1, result);

		result = buffer.lastIndexOf(b -> b == 'b', Integer.MAX_VALUE);
		assertEquals(1, result);

		result = buffer.lastIndexOf(b -> b == 'b', Integer.MIN_VALUE);
		assertEquals(-1, result);

		result = buffer.lastIndexOf(b -> b == 'z', 0);
		assertEquals(-1, result);

		release(buffer);
	}

	@Test
	public void slice() {
		DataBuffer buffer = createDataBuffer(3);
		buffer.write(new byte[]{'a', 'b'});

		DataBuffer slice = buffer.slice(1, 2);
		assertEquals(2, slice.readableByteCount());
		try {
			slice.write((byte) 0);
			fail("IndexOutOfBoundsException expected");
		}
		catch (Exception ignored) {
		}
		buffer.write((byte) 'c');

		assertEquals(3, buffer.readableByteCount());
		byte[] result = new byte[3];
		buffer.read(result);

		assertArrayEquals(new byte[]{'a', 'b', 'c'}, result);

		assertEquals(2, slice.readableByteCount());
		result = new byte[2];
		slice.read(result);

		assertArrayEquals(new byte[]{'b', 'c'}, result);


		release(buffer);
	}


}