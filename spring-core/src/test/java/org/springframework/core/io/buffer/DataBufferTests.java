/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.core.io.buffer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.testfixture.io.buffer.AbstractDataBufferAllocatingTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
@ParameterizedClass
@MethodSource("org.springframework.core.testfixture.io.buffer.AbstractDataBufferAllocatingTests#dataBufferFactories()")
class DataBufferTests extends AbstractDataBufferAllocatingTests {

	DataBufferTests(DataBufferFactory bufferFactory) {
		DataBufferTests.this.bufferFactory = bufferFactory;
	}

	@Test
	void byteCountsAndPositions() {
		DataBuffer buffer = createDataBuffer(2);

		assertThat(buffer.readPosition()).isEqualTo(0);
		assertThat(buffer.writePosition()).isEqualTo(0);
		assertThat(buffer.readableByteCount()).isEqualTo(0);
		assertThat(buffer.writableByteCount()).isEqualTo(2);
		assertThat(buffer.capacity()).isEqualTo(2);

		buffer.write((byte) 'a');
		assertThat(buffer.readPosition()).isEqualTo(0);
		assertThat(buffer.writePosition()).isEqualTo(1);
		assertThat(buffer.readableByteCount()).isEqualTo(1);
		assertThat(buffer.writableByteCount()).isEqualTo(1);
		assertThat(buffer.capacity()).isEqualTo(2);

		buffer.write((byte) 'b');
		assertThat(buffer.readPosition()).isEqualTo(0);
		assertThat(buffer.writePosition()).isEqualTo(2);
		assertThat(buffer.readableByteCount()).isEqualTo(2);
		assertThat(buffer.writableByteCount()).isEqualTo(0);
		assertThat(buffer.capacity()).isEqualTo(2);

		buffer.read();
		assertThat(buffer.readPosition()).isEqualTo(1);
		assertThat(buffer.writePosition()).isEqualTo(2);
		assertThat(buffer.readableByteCount()).isEqualTo(1);
		assertThat(buffer.writableByteCount()).isEqualTo(0);
		assertThat(buffer.capacity()).isEqualTo(2);

		buffer.read();
		assertThat(buffer.readPosition()).isEqualTo(2);
		assertThat(buffer.writePosition()).isEqualTo(2);
		assertThat(buffer.readableByteCount()).isEqualTo(0);
		assertThat(buffer.writableByteCount()).isEqualTo(0);
		assertThat(buffer.capacity()).isEqualTo(2);

		release(buffer);
	}

	@Test
	void readPositionSmallerThanZero() {
		DataBuffer buffer = createDataBuffer(1);
		try {
			assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> buffer.readPosition(-1));
		}
		finally {
			release(buffer);
		}
	}

	@Test
	void readPositionGreaterThanWritePosition() {
		DataBuffer buffer = createDataBuffer(1);
		try {
			assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> buffer.readPosition(1));
		}
		finally {
			release(buffer);
		}
	}

	@Test
	void writePositionSmallerThanReadPosition() {
		DataBuffer buffer = createDataBuffer(2);
		try {
			buffer.write((byte) 'a');
			buffer.read();
			assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> buffer.writePosition(0));
		}
		finally {
			release(buffer);
		}
	}

	@Test
	void writePositionGreaterThanCapacity() {
		DataBuffer buffer = createDataBuffer(1);
		try {
			assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> buffer.writePosition(2));
		}
		finally {
			release(buffer);
		}
	}

	@Test
	void writeAndRead() {
		DataBuffer buffer = createDataBuffer(5);
		buffer.write(new byte[]{'a', 'b', 'c'});

		int ch = buffer.read();
		assertThat(ch).isEqualTo((byte) 'a');

		buffer.write((byte) 'd');
		buffer.write((byte) 'e');

		byte[] result = new byte[4];
		buffer.read(result);

		assertThat(result).isEqualTo(new byte[]{'b', 'c', 'd', 'e'});

		release(buffer);
	}

	@Test
	void writeNullString() {
		DataBuffer buffer = createDataBuffer(1);
		try {
			assertThatIllegalArgumentException().isThrownBy(() ->
					buffer.write(null, StandardCharsets.UTF_8));
		}
		finally {
			release(buffer);
		}
	}

	@Test
	void writeNullCharset() {
		DataBuffer buffer = createDataBuffer(1);
		try {
			assertThatIllegalArgumentException().isThrownBy(() ->
					buffer.write("test", null));
		}
		finally {
			release(buffer);
		}
	}

	@Test
	void writeEmptyString() {
		DataBuffer buffer = createDataBuffer(1);
		buffer.write("", StandardCharsets.UTF_8);

		assertThat(buffer.readableByteCount()).isEqualTo(0);

		release(buffer);
	}

	@Test
	void writeUtf8String() {
		DataBuffer buffer = createDataBuffer(6);
		buffer.write("Spring", StandardCharsets.UTF_8);

		byte[] result = new byte[6];
		buffer.read(result);

		assertThat(result).isEqualTo("Spring".getBytes(StandardCharsets.UTF_8));
		release(buffer);
	}

	@Test
	void writeUtf8StringOutGrowsCapacity() {
		DataBuffer buffer = createDataBuffer(5);
		buffer.write("Spring €", StandardCharsets.UTF_8);

		byte[] result = new byte[10];
		buffer.read(result);

		assertThat(result).isEqualTo("Spring €".getBytes(StandardCharsets.UTF_8));
		release(buffer);
	}

	@Test
	void writeIsoString() {
		DataBuffer buffer = createDataBuffer(3);
		buffer.write("\u00A3", StandardCharsets.ISO_8859_1);

		byte[] result = new byte[1];
		buffer.read(result);

		assertThat(result).isEqualTo("\u00A3".getBytes(StandardCharsets.ISO_8859_1));
		release(buffer);
	}

	@Test
	void writeMultipleUtf8String() {
		DataBuffer buffer = createDataBuffer(1);
		buffer.write("abc", StandardCharsets.UTF_8);
		assertThat(buffer.readableByteCount()).isEqualTo(3);

		buffer.write("def", StandardCharsets.UTF_8);
		assertThat(buffer.readableByteCount()).isEqualTo(6);

		buffer.write("ghi", StandardCharsets.UTF_8);
		assertThat(buffer.readableByteCount()).isEqualTo(9);

		byte[] result = new byte[9];
		buffer.read(result);

		assertThat(result).isEqualTo("abcdefghi".getBytes());

		release(buffer);
	}

	@Test
	void toStringNullCharset() {
		DataBuffer buffer = createDataBuffer(1);
		try {
			assertThatIllegalArgumentException().isThrownBy(() ->
					buffer.toString(null));
		}
		finally {
			release(buffer);
		}
	}

	@Test
	void toStringUtf8() {
		String spring = "Spring";
		byte[] bytes = spring.getBytes(StandardCharsets.UTF_8);
		DataBuffer buffer = createDataBuffer(bytes.length);
		buffer.write(bytes);

		String result = buffer.toString(StandardCharsets.UTF_8);

		assertThat(result).isEqualTo(spring);
		release(buffer);
	}

	@Test
	void toStringSection() {
		String spring = "Spring";
		byte[] bytes = spring.getBytes(StandardCharsets.UTF_8);
		DataBuffer buffer = createDataBuffer(bytes.length);
		buffer.write(bytes);

		String result = buffer.toString(1, 3, StandardCharsets.UTF_8);

		assertThat(result).isEqualTo("pri");
		release(buffer);
	}

	@Test
	void inputStream() throws Exception {
		DataBuffer buffer = createDataBuffer(4);
		buffer.write(new byte[]{'a', 'b', 'c', 'd', 'e'});
		buffer.readPosition(1);

		InputStream inputStream = buffer.asInputStream();

		assertThat(inputStream.available()).isEqualTo(4);

		int result = inputStream.read();
		assertThat(result).isEqualTo((byte) 'b');
		assertThat(inputStream.available()).isEqualTo(3);

		assertThat(inputStream.markSupported()).isTrue();
		inputStream.mark(2);

		byte[] bytes = new byte[2];
		int len = inputStream.read(bytes);
		assertThat(len).isEqualTo(2);
		assertThat(bytes).isEqualTo(new byte[]{'c', 'd'});
		assertThat(inputStream.available()).isEqualTo(1);

		Arrays.fill(bytes, (byte) 0);
		len = inputStream.read(bytes);
		assertThat(len).isEqualTo(1);
		assertThat(bytes).isEqualTo(new byte[]{'e', (byte) 0});
		assertThat(inputStream.available()).isEqualTo(0);

		assertThat(inputStream.read()).isEqualTo(-1);
		assertThat(inputStream.read(bytes)).isEqualTo(-1);

		inputStream.reset();
		bytes = new byte[3];
		len = inputStream.read(bytes);
		assertThat(len).isEqualTo(3);
		assertThat(bytes).containsExactly('c', 'd', 'e');

		buffer.readPosition(0);
		inputStream = buffer.asInputStream();
		assertThat(inputStream.readAllBytes()).asString().isEqualTo("abcde");
		assertThat(inputStream.available()).isEqualTo(0);
		assertThat(inputStream.readAllBytes()).isEmpty();

		buffer.readPosition(0);
		inputStream = buffer.asInputStream();
		inputStream.mark(5);
		assertThat(inputStream.readNBytes(0)).isEmpty();
		assertThat(inputStream.readNBytes(1000)).asString().isEqualTo("abcde");
		inputStream.reset();
		assertThat(inputStream.readNBytes(3)).asString().isEqualTo("abc");
		assertThat(inputStream.readNBytes(2)).asString().isEqualTo("de");
		assertThat(inputStream.readNBytes(10)).isEmpty();

		buffer.readPosition(0);
		inputStream = buffer.asInputStream();
		inputStream.mark(5);
		assertThat(inputStream.skip(1)).isEqualTo(1);
		assertThat(inputStream.readAllBytes()).asString().isEqualTo("bcde");
		assertThat(inputStream.skip(10)).isEqualTo(0);
		assertThat(inputStream.available()).isEqualTo(0);
		inputStream.reset();
		assertThat(inputStream.skip(100)).isEqualTo(5);
		assertThat(inputStream.available()).isEqualTo(0);

		buffer.readPosition(0);
		inputStream = buffer.asInputStream();
		inputStream.mark(5);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertThat(inputStream.transferTo(out)).isEqualTo(5);
		assertThat(out.toByteArray()).asString().isEqualTo("abcde");
		assertThat(inputStream.available()).isEqualTo(0);
		out.reset();
		inputStream.reset();
		assertThat(inputStream.read()).isEqualTo('a');
		assertThat(inputStream.transferTo(out)).isEqualTo(4);
		assertThat(out.toByteArray()).asString().isEqualTo("bcde");
		assertThat(inputStream.available()).isEqualTo(0);
		assertThat(inputStream.transferTo(OutputStream.nullOutputStream())).isEqualTo(0);

		release(buffer);
	}

	@Test
	void inputStreamReleaseOnClose() throws Exception {
		DataBuffer buffer = createDataBuffer(3);
		byte[] bytes = {'a', 'b', 'c'};
		buffer.write(bytes);

		try (InputStream inputStream = buffer.asInputStream(true)) {
			byte[] result = new byte[3];
			int len = inputStream.read(result);
			assertThat(len).isEqualTo(3);
			assertThat(result).isEqualTo(bytes);
		}

		// AbstractDataBufferAllocatingTests.leakDetector will verify the buffer's release
	}

	@Test
	void outputStream() throws Exception {
		DataBuffer buffer = createDataBuffer(4);
		buffer.write((byte) 'a');

		OutputStream outputStream = buffer.asOutputStream();
		outputStream.write('b');
		outputStream.write(new byte[]{'c', 'd'});

		buffer.write((byte) 'e');

		byte[] bytes = new byte[5];
		buffer.read(bytes);
		assertThat(bytes).isEqualTo(new byte[]{'a', 'b', 'c', 'd', 'e'});

		release(buffer);
	}

	@Test
	void expand() {
		DataBuffer buffer = createDataBuffer(1);
		buffer.write((byte) 'a');
		assertThat(buffer.capacity()).isEqualTo(1);
		buffer.write((byte) 'b');

		assertThat(buffer.capacity()).isGreaterThan(1);

		release(buffer);
	}

	@Test
	@SuppressWarnings("deprecation")
	void increaseCapacity() {
		DataBuffer buffer = createDataBuffer(1);
		assertThat(buffer.capacity()).isEqualTo(1);

		buffer.capacity(2);
		assertThat(buffer.capacity()).isEqualTo(2);

		release(buffer);
	}

	@Test
	@SuppressWarnings("deprecation")
	void decreaseCapacityLowReadPosition() {
		DataBuffer buffer = createDataBuffer(2);
		buffer.writePosition(2);
		buffer.capacity(1);
		assertThat(buffer.capacity()).isEqualTo(1);

		release(buffer);
	}

	@Test
	@SuppressWarnings("deprecation")
	void decreaseCapacityHighReadPosition() {
		DataBuffer buffer = createDataBuffer(2);
		buffer.writePosition(2);
		buffer.readPosition(2);
		buffer.capacity(1);
		assertThat(buffer.capacity()).isEqualTo(1);

		release(buffer);
	}

	@Test
	@SuppressWarnings("deprecation")
	void capacityLessThanZero() {
		DataBuffer buffer = createDataBuffer(1);
		try {
			assertThatIllegalArgumentException().isThrownBy(() -> buffer.capacity(-1));
		}
		finally {
			release(buffer);
		}
	}

	@Test
	void writeByteBuffer() {
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

		assertThat(buffer1.readableByteCount()).isEqualTo(4);
		byte[] result = new byte[4];
		buffer1.read(result);

		assertThat(result).isEqualTo(new byte[]{'a', 'b', 'c', 'd'});

		release(buffer1);
	}

	private ByteBuffer createByteBuffer(int capacity) {
		return ByteBuffer.allocate(capacity);
	}

	@Test
	void writeDataBuffer() {
		DataBuffer buffer1 = createDataBuffer(1);
		buffer1.write((byte) 'a');
		DataBuffer buffer2 = createDataBuffer(2);
		buffer2.write((byte) 'b');
		DataBuffer buffer3 = createDataBuffer(3);
		buffer3.write((byte) 'c');

		buffer1.write(buffer2, buffer3);
		buffer1.write((byte) 'd'); // make sure the write index is correctly set

		assertThat(buffer1.readableByteCount()).isEqualTo(4);
		byte[] result = new byte[4];
		buffer1.read(result);

		assertThat(result).isEqualTo(new byte[]{'a', 'b', 'c', 'd'});

		release(buffer1, buffer2, buffer3);
	}

	@Test
	@SuppressWarnings("deprecation")
	void asByteBuffer() {
		DataBuffer buffer = createDataBuffer(4);
		buffer.write(new byte[]{'a', 'b', 'c'});
		buffer.read(); // skip a

		ByteBuffer result = buffer.asByteBuffer();
		assertThat(result.capacity()).isEqualTo(2);

		buffer.write((byte) 'd');
		assertThat(result.remaining()).isEqualTo(2);

		byte[] resultBytes = new byte[2];
		result.get(resultBytes);
		assertThat(resultBytes).isEqualTo(new byte[]{'b', 'c'});

		release(buffer);
	}

	@Test
	void asByteBufferIndexLength() {
		DataBuffer buffer = createDataBuffer(3);
		buffer.write(new byte[]{'a', 'b'});

		@SuppressWarnings("deprecation")
		ByteBuffer result = buffer.asByteBuffer(1, 2);
		assertThat(result.capacity()).isEqualTo(2);

		buffer.write((byte) 'c');
		assertThat(result.remaining()).isEqualTo(2);

		byte[] resultBytes = new byte[2];
		result.get(resultBytes);
		assertThat(resultBytes).isEqualTo(new byte[]{'b', 'c'});

		release(buffer);
	}

	@Test
	@SuppressWarnings("deprecation")
	void byteBufferContainsDataBufferChanges() {
		DataBuffer dataBuffer = createDataBuffer(1);
		ByteBuffer byteBuffer = dataBuffer.asByteBuffer(0, 1);

		dataBuffer.write((byte) 'a');

		assertThat(byteBuffer.limit()).isEqualTo(1);
		byte b = byteBuffer.get();
		assertThat(b).isEqualTo((byte) 'a');

		release(dataBuffer);
	}

	@Test
	@SuppressWarnings("deprecation")
	void dataBufferContainsByteBufferChanges() {
		DataBuffer dataBuffer = createDataBuffer(1);
		ByteBuffer byteBuffer = dataBuffer.asByteBuffer(0, 1);

		byteBuffer.put((byte) 'a');
		dataBuffer.writePosition(1);

		byte b = dataBuffer.read();
		assertThat(b).isEqualTo((byte) 'a');

		release(dataBuffer);
	}

	@Test
	@SuppressWarnings("deprecation")
	void emptyAsByteBuffer() {
		DataBuffer buffer = createDataBuffer(1);

		ByteBuffer result = buffer.asByteBuffer();
		assertThat(result.capacity()).isEqualTo(0);

		release(buffer);
	}


	@Test
	void toByteBuffer() {
		DataBuffer buffer = createDataBuffer(4);
		buffer.write(new byte[]{'a', 'b', 'c'});
		buffer.read(); // skip a

		@SuppressWarnings("deprecation")
		ByteBuffer result = buffer.toByteBuffer();
		assertThat(result.capacity()).isEqualTo(2);
		assertThat(result.remaining()).isEqualTo(2);

		byte[] resultBytes = new byte[2];
		result.get(resultBytes);
		assertThat(resultBytes).isEqualTo(new byte[]{'b', 'c'});

		release(buffer);
	}

	@Test
	void toByteBufferIndexLength() {
		DataBuffer buffer = createDataBuffer(3);
		buffer.write(new byte[]{'a', 'b', 'c'});

		@SuppressWarnings("deprecation")
		ByteBuffer result = buffer.toByteBuffer(1, 2);
		assertThat(result.capacity()).isEqualTo(2);
		assertThat(result.remaining()).isEqualTo(2);

		byte[] resultBytes = new byte[2];
		result.get(resultBytes);
		assertThat(resultBytes).isEqualTo(new byte[]{'b', 'c'});

		release(buffer);
	}

	@Test
	void toByteBufferDestination() {
		DataBuffer buffer = createDataBuffer(4);
		buffer.write(new byte[]{'a', 'b', 'c'});

		ByteBuffer byteBuffer = createByteBuffer(2);
		buffer.toByteBuffer(1, byteBuffer, 0, 2);
		assertThat(byteBuffer.capacity()).isEqualTo(2);
		assertThat(byteBuffer.remaining()).isEqualTo(2);

		byte[] resultBytes = new byte[2];
		byteBuffer.get(resultBytes);
		assertThat(resultBytes).isEqualTo(new byte[]{'b', 'c'});

		assertThatExceptionOfType(IndexOutOfBoundsException.class)
				.isThrownBy(() -> buffer.toByteBuffer(0, byteBuffer, 0, 3));

		release(buffer);
	}

	@Test
	void readableByteBuffers() {
		DataBuffer dataBuffer = this.bufferFactory.allocateBuffer(3);
		dataBuffer.write("abc".getBytes(StandardCharsets.UTF_8));
		dataBuffer.readPosition(1);
		dataBuffer.writePosition(2);


		byte[] result = new byte[1];
		try (var iterator = dataBuffer.readableByteBuffers()) {
			assertThat(iterator).hasNext();
			int i = 0;
			while (iterator.hasNext()) {
				ByteBuffer byteBuffer = iterator.next();
				assertThat(byteBuffer.position()).isEqualTo(0);
				assertThat(byteBuffer.limit()).isEqualTo(1);
				assertThat(byteBuffer.capacity()).isEqualTo(1);
				assertThat(byteBuffer.remaining()).isEqualTo(1);

				byteBuffer.get(result, i, 1);

				assertThat(iterator).isExhausted();
			}
		}

		assertThat(result).containsExactly('b');

		release(dataBuffer);
	}

	@Test
	void readableByteBuffersJoined() {
		DataBuffer dataBuffer = this.bufferFactory.join(Arrays.asList(stringBuffer("a"),
				stringBuffer("b"), stringBuffer("c")));

		byte[] result = new byte[3];
		try (var iterator = dataBuffer.readableByteBuffers()) {
			assertThat(iterator).hasNext();
			int i = 0;
			while (iterator.hasNext()) {
				ByteBuffer byteBuffer = iterator.next();
				int len = byteBuffer.remaining();
				byteBuffer.get(result, i, len);
				i += len;
				assertThatException().isThrownBy(() -> byteBuffer.put((byte) 'd'));
			}
		}

		assertThat(result).containsExactly('a', 'b', 'c');

		release(dataBuffer);
	}

	@Test
	void writableByteBuffers() {
		DataBuffer dataBuffer = this.bufferFactory.allocateBuffer(3);
		dataBuffer.write("ab".getBytes(StandardCharsets.UTF_8));
		dataBuffer.readPosition(1);

		try (DataBuffer.ByteBufferIterator iterator = dataBuffer.writableByteBuffers()) {
			assertThat(iterator).hasNext();
			ByteBuffer byteBuffer = iterator.next();
			assertThat(byteBuffer.position()).isEqualTo(0);
			assertThat(byteBuffer.limit()).isEqualTo(1);
			assertThat(byteBuffer.capacity()).isEqualTo(1);
			assertThat(byteBuffer.remaining()).isEqualTo(1);

			byteBuffer.put((byte) 'c');
			dataBuffer.writePosition(3);

			assertThat(iterator).isExhausted();
		}
		byte[] result = new byte[2];
		dataBuffer.read(result);
		assertThat(result).containsExactly('b', 'c');

		release(dataBuffer);
	}

	@Test
	void indexOf() {
		DataBuffer buffer = createDataBuffer(3);
		buffer.write(new byte[]{'a', 'b', 'c'});

		int result = buffer.indexOf(b -> b == 'c', 0);
		assertThat(result).isEqualTo(2);

		result = buffer.indexOf(b -> b == 'c', Integer.MIN_VALUE);
		assertThat(result).isEqualTo(2);

		result = buffer.indexOf(b -> b == 'c', Integer.MAX_VALUE);
		assertThat(result).isEqualTo(-1);

		result = buffer.indexOf(b -> b == 'z', 0);
		assertThat(result).isEqualTo(-1);

		release(buffer);
	}

	@Test
	void lastIndexOf() {
		DataBuffer buffer = createDataBuffer(3);
		buffer.write(new byte[]{'a', 'b', 'c'});

		int result = buffer.lastIndexOf(b -> b == 'b', 2);
		assertThat(result).isEqualTo(1);

		result = buffer.lastIndexOf(b -> b == 'c', 2);
		assertThat(result).isEqualTo(2);

		result = buffer.lastIndexOf(b -> b == 'b', Integer.MAX_VALUE);
		assertThat(result).isEqualTo(1);

		result = buffer.lastIndexOf(b -> b == 'c', Integer.MAX_VALUE);
		assertThat(result).isEqualTo(2);

		result = buffer.lastIndexOf(b -> b == 'b', Integer.MIN_VALUE);
		assertThat(result).isEqualTo(-1);

		result = buffer.lastIndexOf(b -> b == 'c', Integer.MIN_VALUE);
		assertThat(result).isEqualTo(-1);

		result = buffer.lastIndexOf(b -> b == 'z', 0);
		assertThat(result).isEqualTo(-1);

		release(buffer);
	}

	@Test
	@SuppressWarnings("deprecation")
	void slice() {
		DataBuffer buffer = createDataBuffer(3);
		buffer.write(new byte[]{'a', 'b'});

		DataBuffer slice = buffer.slice(1, 2);
		assertThat(slice.readableByteCount()).isEqualTo(2);
		buffer.write((byte) 'c');

		assertThat(buffer.readableByteCount()).isEqualTo(3);
		byte[] result = new byte[3];
		buffer.read(result);

		assertThat(result).isEqualTo(new byte[]{'a', 'b', 'c'});

		assertThat(slice.readableByteCount()).isEqualTo(2);
		result = new byte[2];
		slice.read(result);

		assertThat(result).isEqualTo(new byte[]{'b', 'c'});
		release(buffer);
	}

	@Test
	@SuppressWarnings("deprecation")
	void retainedSlice() {
		DataBuffer buffer = createDataBuffer(3);
		buffer.write(new byte[]{'a', 'b'});

		DataBuffer slice = buffer.retainedSlice(1, 2);
		assertThat(slice.readableByteCount()).isEqualTo(2);
		buffer.write((byte) 'c');

		assertThat(buffer.readableByteCount()).isEqualTo(3);
		byte[] result = new byte[3];
		buffer.read(result);

		assertThat(result).isEqualTo(new byte[]{'a', 'b', 'c'});

		assertThat(slice.readableByteCount()).isEqualTo(2);
		result = new byte[2];
		slice.read(result);

		assertThat(result).isEqualTo(new byte[]{'b', 'c'});

		release(buffer, slice);
	}

	@Test
	@SuppressWarnings("deprecation")
	void spr16351() {
		DataBuffer buffer = createDataBuffer(6);
		byte[] bytes = {'a', 'b', 'c', 'd', 'e', 'f'};
		buffer.write(bytes);
		DataBuffer slice = buffer.slice(3, 3);
		buffer.writePosition(3);
		buffer.write(slice);

		assertThat(buffer.readableByteCount()).isEqualTo(6);
		byte[] result = new byte[6];
		buffer.read(result);

		assertThat(result).isEqualTo(bytes);

		release(slice);
		release(buffer);
	}

	@Test
	void split() {
		DataBuffer buffer = createDataBuffer(3);
		buffer.write(new byte[]{'a', 'b'});

		assertThatException().isThrownBy(() -> buffer.split(-1));
		assertThatException().isThrownBy(() -> buffer.split(4));

		DataBuffer split = buffer.split(1);

		assertThat(split.readPosition()).isEqualTo(0);
		assertThat(split.writePosition()).isEqualTo(1);
		assertThat(split.capacity()).isEqualTo(1);
		assertThat(split.readableByteCount()).isEqualTo(1);
		byte[] bytes = new byte[1];
		split.read(bytes);
		assertThat(bytes).containsExactly('a');

		assertThat(buffer.readPosition()).isEqualTo(0);
		assertThat(buffer.writePosition()).isEqualTo(1);
		assertThat(buffer.capacity()).isEqualTo(2);

		buffer.write((byte) 'c');
		assertThat(buffer.readableByteCount()).isEqualTo(2);
		bytes = new byte[2];
		buffer.read(bytes);

		assertThat(bytes).isEqualTo(new byte[]{'b', 'c'});

		DataBuffer buffer2 = createDataBuffer(1);
		buffer2.write(new byte[]{'a'});
		DataBuffer split2 = buffer2.split(1);

		assertThat(split2.readPosition()).isEqualTo(0);
		assertThat(split2.writePosition()).isEqualTo(1);
		assertThat(split2.capacity()).isEqualTo(1);
		assertThat(split2.readableByteCount()).isEqualTo(1);
		bytes = new byte[1];
		split2.read(bytes);
		assertThat(bytes).containsExactly('a');

		assertThat(buffer2.readPosition()).isEqualTo(0);
		assertThat(buffer2.writePosition()).isEqualTo(0);
		assertThat(buffer2.capacity()).isEqualTo(0);
		assertThat(buffer.readableByteCount()).isEqualTo(0);

		release(buffer, buffer2, split, split2);
	}

	@Test
	void join() {
		DataBuffer composite = this.bufferFactory.join(Arrays.asList(stringBuffer("a"),
				stringBuffer("b"), stringBuffer("c")));
		assertThat(composite.readableByteCount()).isEqualTo(3);
		byte[] bytes = new byte[3];
		composite.read(bytes);

		assertThat(bytes).isEqualTo(new byte[]{'a', 'b', 'c'});

		release(composite);
	}

	@Test
	void getByte() {
		DataBuffer buffer = stringBuffer("abc");

		assertThat(buffer.getByte(0)).isEqualTo((byte) 'a');
		assertThat(buffer.getByte(1)).isEqualTo((byte) 'b');
		assertThat(buffer.getByte(2)).isEqualTo((byte) 'c');
		assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> buffer.getByte(-1));
		assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> buffer.getByte(3));

		release(buffer);
	}

	@Test // gh-31605
	void shouldHonorSourceBuffersReadPosition() {
		DataBuffer dataBuffer = bufferFactory.wrap("ab".getBytes(StandardCharsets.UTF_8));
		dataBuffer.readPosition(1);

		ByteBuffer byteBuffer = ByteBuffer.allocate(dataBuffer.readableByteCount());
		dataBuffer.toByteBuffer(byteBuffer);

		assertThat(StandardCharsets.UTF_8.decode(byteBuffer).toString()).isEqualTo("b");
	}

	@Test // gh-31873
	void repeatedWrites() {
		DataBuffer buffer = bufferFactory.allocateBuffer(256);
		String name = "Müller";
		int repeatCount = 19;
		for (int i = 0; i < repeatCount; i++) {
			buffer.write(name, StandardCharsets.UTF_8);
		}
		String result = buffer.toString(StandardCharsets.UTF_8);
		String expected = name.repeat(repeatCount);
		assertThat(result).isEqualTo(expected);

		release(buffer);
	}

	@Test
	void forEachByteProcessAll() {
		List<Byte> result = new ArrayList<>();
		DataBuffer buffer = byteBuffer(new byte[]{'a', 'b', 'c', 'd'});
		int index = buffer.forEachByte(0, 4, b -> {
			result.add(b);
			return true;
		});
		assertThat(index).isEqualTo(-1);
		assertThat(result).containsExactly((byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd');
		release(buffer);
	}


	@Test
	void forEachByteProcessSome() {
		List<Byte> result = new ArrayList<>();
		DataBuffer buffer = byteBuffer(new byte[]{'a', 'b', 'c', 'd'});
		int index = buffer.forEachByte(0, 4, b -> {
			result.add(b);
			return (b != 'c');
		});
		assertThat(index).isEqualTo(2);
		assertThat(result).containsExactly((byte) 'a', (byte) 'b', (byte) 'c');
		release(buffer);
	}

}
