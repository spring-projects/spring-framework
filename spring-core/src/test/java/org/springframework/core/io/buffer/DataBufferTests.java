/*
 * Copyright 2002-2019 the original author or authors.
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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class DataBufferTests extends AbstractDataBufferAllocatingTestCase {

	@Test
	public void byteCountsAndPositions() {
		DataBuffer buffer = createDataBuffer(2);

		assertEquals(0, buffer.readPosition());
		assertEquals(0, buffer.writePosition());
		assertEquals(0, buffer.readableByteCount());
		assertEquals(2, buffer.writableByteCount());
		assertEquals(2, buffer.capacity());

		buffer.write((byte) 'a');
		assertEquals(0, buffer.readPosition());
		assertEquals(1, buffer.writePosition());
		assertEquals(1, buffer.readableByteCount());
		assertEquals(1, buffer.writableByteCount());
		assertEquals(2, buffer.capacity());

		buffer.write((byte) 'b');
		assertEquals(0, buffer.readPosition());
		assertEquals(2, buffer.writePosition());
		assertEquals(2, buffer.readableByteCount());
		assertEquals(0, buffer.writableByteCount());
		assertEquals(2, buffer.capacity());

		buffer.read();
		assertEquals(1, buffer.readPosition());
		assertEquals(2, buffer.writePosition());
		assertEquals(1, buffer.readableByteCount());
		assertEquals(0, buffer.writableByteCount());
		assertEquals(2, buffer.capacity());

		buffer.read();
		assertEquals(2, buffer.readPosition());
		assertEquals(2, buffer.writePosition());
		assertEquals(0, buffer.readableByteCount());
		assertEquals(0, buffer.writableByteCount());
		assertEquals(2, buffer.capacity());

		release(buffer);
	}

	@Test
	public void readPositionSmallerThanZero() {
		DataBuffer buffer = createDataBuffer(1);
		try {
			buffer.readPosition(-1);
			fail("IndexOutOfBoundsException expected");
		}
		catch (IndexOutOfBoundsException ignored) {
		}
		finally {
			release(buffer);
		}
	}

	@Test
	public void readPositionGreaterThanWritePosition() {
		DataBuffer buffer = createDataBuffer(1);
		try {
			buffer.readPosition(1);
			fail("IndexOutOfBoundsException expected");
		}
		catch (IndexOutOfBoundsException ignored) {
		}
		finally {
			release(buffer);
		}
	}

	@Test
	public void writePositionSmallerThanReadPosition() {
		DataBuffer buffer = createDataBuffer(2);
		try {
			buffer.write((byte) 'a');
			buffer.read();

			buffer.writePosition(0);
			fail("IndexOutOfBoundsException expected");
		}
		catch (IndexOutOfBoundsException ignored) {
		}
		finally {
			release(buffer);
		}
	}

	@Test
	public void writePositionGreaterThanCapacity() {
		DataBuffer buffer = createDataBuffer(1);
		try {
			buffer.writePosition(2);
			fail("IndexOutOfBoundsException expected");
		}
		catch (IndexOutOfBoundsException ignored) {
		}
		finally {
			release(buffer);
		}
	}

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
	public void writeNullString() {
		DataBuffer buffer = createDataBuffer(1);
		try {
			buffer.write(null, StandardCharsets.UTF_8);
			fail("IllegalArgumentException expected");
		}
		catch (IllegalArgumentException exc) {
		}
		finally {
			release(buffer);
		}
	}

	@Test
	public void writeNullCharset() {
		DataBuffer buffer = createDataBuffer(1);
		try {
			buffer.write("test", null);
			fail("IllegalArgumentException expected");
		}
		catch (IllegalArgumentException exc) {
		}
		finally {
			release(buffer);
		}
	}

	@Test
	public void writeEmptyString() {
		DataBuffer buffer = createDataBuffer(1);
		buffer.write("", StandardCharsets.UTF_8);

		assertEquals(0, buffer.readableByteCount());

		release(buffer);
	}

	@Test
	public void writeUtf8String() {
		DataBuffer buffer = createDataBuffer(6);
		buffer.write("Spring", StandardCharsets.UTF_8);

		byte[] result = new byte[6];
		buffer.read(result);

		assertArrayEquals("Spring".getBytes(StandardCharsets.UTF_8), result);
		release(buffer);
	}

	@Test
	public void writeUtf8StringOutGrowsCapacity() {
		DataBuffer buffer = createDataBuffer(5);
		buffer.write("Spring €", StandardCharsets.UTF_8);

		byte[] result = new byte[10];
		buffer.read(result);

		assertArrayEquals("Spring €".getBytes(StandardCharsets.UTF_8), result);
		release(buffer);
	}

	@Test
	public void writeIsoString() {
		DataBuffer buffer = createDataBuffer(3);
		buffer.write("\u00A3", StandardCharsets.ISO_8859_1);

		byte[] result = new byte[1];
		buffer.read(result);

		assertArrayEquals("\u00A3".getBytes(StandardCharsets.ISO_8859_1), result);
		release(buffer);
	}

	@Test
	public void writeMultipleUtf8String() {

		DataBuffer buffer = createDataBuffer(1);
		buffer.write("abc", StandardCharsets.UTF_8);
		assertEquals(3, buffer.readableByteCount());

		buffer.write("def", StandardCharsets.UTF_8);
		assertEquals(6, buffer.readableByteCount());

		buffer.write("ghi", StandardCharsets.UTF_8);
		assertEquals(9, buffer.readableByteCount());

		byte[] result = new byte[9];
		buffer.read(result);

		assertArrayEquals("abcdefghi".getBytes(), result);

		release(buffer);
	}

	@Test
	public void inputStream() throws IOException {
		DataBuffer buffer = createDataBuffer(4);
		buffer.write(new byte[]{'a', 'b', 'c', 'd', 'e'});
		buffer.readPosition(1);

		InputStream inputStream = buffer.asInputStream();

		assertEquals(4, inputStream.available());

		int result = inputStream.read();
		assertEquals('b', result);
		assertEquals(3, inputStream.available());

		byte[] bytes = new byte[2];
		int len = inputStream.read(bytes);
		assertEquals(2, len);
		assertArrayEquals(new byte[]{'c', 'd'}, bytes);
		assertEquals(1, inputStream.available());

		Arrays.fill(bytes, (byte) 0);
		len = inputStream.read(bytes);
		assertEquals(1, len);
		assertArrayEquals(new byte[]{'e', (byte) 0}, bytes);
		assertEquals(0, inputStream.available());

		assertEquals(-1, inputStream.read());
		assertEquals(-1, inputStream.read(bytes));

		release(buffer);
	}

	@Test
	public void inputStreamReleaseOnClose() throws IOException {
		DataBuffer buffer = createDataBuffer(3);
		byte[] bytes = {'a', 'b', 'c'};
		buffer.write(bytes);

		InputStream inputStream = buffer.asInputStream(true);

		try {
			byte[] result = new byte[3];
			int len = inputStream.read(result);
			assertEquals(3, len);
			assertArrayEquals(bytes, result);
		} finally {
			inputStream.close();
		}

		// AbstractDataBufferAllocatingTestCase.LeakDetector will verify the buffer's release

	}

	@Test
	public void outputStream() throws IOException {
		DataBuffer buffer = createDataBuffer(4);
		buffer.write((byte) 'a');

		OutputStream outputStream = buffer.asOutputStream();
		outputStream.write('b');
		outputStream.write(new byte[]{'c', 'd'});

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
		assertEquals(1, buffer.capacity());
		buffer.write((byte) 'b');

		assertTrue(buffer.capacity() > 1);

		release(buffer);
	}

	@Test
	public void increaseCapacity() {
		DataBuffer buffer = createDataBuffer(1);
		assertEquals(1, buffer.capacity());

		buffer.capacity(2);
		assertEquals(2, buffer.capacity());

		release(buffer);
	}

	@Test
	public void decreaseCapacityLowReadPosition() {
		DataBuffer buffer = createDataBuffer(2);
		buffer.writePosition(2);
		buffer.capacity(1);
		assertEquals(1, buffer.capacity());

		release(buffer);
	}

	@Test
	public void decreaseCapacityHighReadPosition() {
		DataBuffer buffer = createDataBuffer(2);
		buffer.writePosition(2);
		buffer.readPosition(2);
		buffer.capacity(1);
		assertEquals(1, buffer.capacity());

		release(buffer);
	}

	@Test
	public void capacityLessThanZero() {
		DataBuffer buffer = createDataBuffer(1);
		try {
			buffer.capacity(-1);
			fail("IllegalArgumentException expected");
		}
		catch (IllegalArgumentException ignored) {
		}
		finally {
			release(buffer);
		}
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

		release(buffer1, buffer2, buffer3);
	}

	@Test
	public void asByteBuffer() {
		DataBuffer buffer = createDataBuffer(4);
		buffer.write(new byte[]{'a', 'b', 'c'});
		buffer.read(); // skip a

		ByteBuffer result = buffer.asByteBuffer();
		assertEquals(2, result.capacity());

		buffer.write((byte) 'd');
		assertEquals(2, result.remaining());

		byte[] resultBytes = new byte[2];
		result.get(resultBytes);
		assertArrayEquals(new byte[]{'b', 'c'}, resultBytes);

		release(buffer);
	}

	@Test
	public void asByteBufferIndexLength() {
		DataBuffer buffer = createDataBuffer(3);
		buffer.write(new byte[]{'a', 'b'});

		ByteBuffer result = buffer.asByteBuffer(1, 2);
		assertEquals(2, result.capacity());

		buffer.write((byte) 'c');
		assertEquals(2, result.remaining());

		byte[] resultBytes = new byte[2];
		result.get(resultBytes);
		assertArrayEquals(new byte[]{'b', 'c'}, resultBytes);

		release(buffer);
	}

	@Test
	public void byteBufferContainsDataBufferChanges() {
		DataBuffer dataBuffer = createDataBuffer(1);
		ByteBuffer byteBuffer = dataBuffer.asByteBuffer(0, 1);

		dataBuffer.write((byte) 'a');

		assertEquals(1, byteBuffer.limit());
		byte b = byteBuffer.get();
		assertEquals('a', b);

		release(dataBuffer);
	}

	@Test
	public void dataBufferContainsByteBufferChanges() {
		DataBuffer dataBuffer = createDataBuffer(1);
		ByteBuffer byteBuffer = dataBuffer.asByteBuffer(0, 1);

		byteBuffer.put((byte) 'a');
		dataBuffer.writePosition(1);

		byte b = dataBuffer.read();
		assertEquals('a', b);

		release(dataBuffer);
	}

	@Test
	public void emptyAsByteBuffer() {
		DataBuffer buffer = createDataBuffer(1);

		ByteBuffer result = buffer.asByteBuffer();
		assertEquals(0, result.capacity());

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

		int result = buffer.lastIndexOf(b -> b == 'b', 2);
		assertEquals(1, result);

		result = buffer.lastIndexOf(b -> b == 'c', 2);
		assertEquals(2, result);

		result = buffer.lastIndexOf(b -> b == 'b', Integer.MAX_VALUE);
		assertEquals(1, result);

		result = buffer.lastIndexOf(b -> b == 'c', Integer.MAX_VALUE);
		assertEquals(2, result);

		result = buffer.lastIndexOf(b -> b == 'b', Integer.MIN_VALUE);
		assertEquals(-1, result);

		result = buffer.lastIndexOf(b -> b == 'c', Integer.MIN_VALUE);
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
			fail("Exception expected");
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

	@Test
	public void spr16351() {
		DataBuffer buffer = createDataBuffer(6);
		byte[] bytes = {'a', 'b', 'c', 'd', 'e', 'f'};
		buffer.write(bytes);
		DataBuffer slice = buffer.slice(3, 3);
		buffer.writePosition(3);
		buffer.write(slice);

		assertEquals(6, buffer.readableByteCount());
		byte[] result = new byte[6];
		buffer.read(result);

		assertArrayEquals(bytes, result);

		release(buffer);
	}

	@Test
	public void join() {
		DataBuffer composite = this.bufferFactory.join(Arrays.asList(stringBuffer("a"),
				stringBuffer("b"), stringBuffer("c")));
		assertEquals(3, composite.readableByteCount());
		byte[] bytes = new byte[3];
		composite.read(bytes);

		assertArrayEquals(new byte[] {'a','b','c'}, bytes);

		release(composite);
	}

	@Test
	public void getByte() {
		DataBuffer buffer = stringBuffer("abc");

		assertEquals('a', buffer.getByte(0));
		assertEquals('b', buffer.getByte(1));
		assertEquals('c', buffer.getByte(2));
		try {
			buffer.getByte(-1);
			fail("IndexOutOfBoundsException expected");
		}
		catch (IndexOutOfBoundsException ignored) {
		}

		try {
			buffer.getByte(3);
			fail("IndexOutOfBoundsException expected");
		}
		catch (IndexOutOfBoundsException ignored) {
		}

		release(buffer);
	}

}
