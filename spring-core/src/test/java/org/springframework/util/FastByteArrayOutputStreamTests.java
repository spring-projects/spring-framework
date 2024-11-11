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

package org.springframework.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link FastByteArrayOutputStream}.
 *
 * @author Craig Andrews
 */
class FastByteArrayOutputStreamTests {

	private final FastByteArrayOutputStream os = new FastByteArrayOutputStream();

	private final byte[] helloBytes = "Hello World".getBytes(StandardCharsets.UTF_8);


	@Test
	void size() throws Exception {
		this.os.write(this.helloBytes);
		assertThat(this.helloBytes).hasSize(this.os.size());
	}

	@Test
	void resize() throws Exception {
		this.os.write(this.helloBytes);
		int sizeBefore = this.os.size();
		this.os.resize(64);
		assertByteArrayEqualsString(this.os);
		assertThat(this.os.size()).isEqualTo(sizeBefore);
	}

	@Test
	void stringConversion() throws Exception {
		this.os.write(this.helloBytes);
		assertThat(this.os.toString()).isEqualTo("Hello World");
		assertThat(this.os.toString(StandardCharsets.UTF_8)).isEqualTo("Hello World");

		@SuppressWarnings("resource")
		FastByteArrayOutputStream empty = new FastByteArrayOutputStream();
		assertThat(empty.toString()).isEqualTo("");
		assertThat(empty.toString(StandardCharsets.US_ASCII)).isEqualTo("");

		@SuppressWarnings("resource")
		FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream(5);
		// Add bytes in multiple writes to ensure we get more than one buffer internally
		outputStream.write(this.helloBytes, 0, 5);
		outputStream.write(this.helloBytes, 5, 6);
		assertThat(outputStream.toString()).isEqualTo("Hello World");
		assertThat(outputStream.toString(StandardCharsets.UTF_8)).isEqualTo("Hello World");
	}

	@Test
	void autoGrow() throws IOException {
		this.os.resize(1);
		for (int i = 0; i < 10; i++) {
			this.os.write(1);
		}
		assertThat(this.os.size()).isEqualTo(10);
		assertThat(new byte[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}).isEqualTo(this.os.toByteArray());
	}

	@Test
	void write() throws Exception {
		this.os.write(this.helloBytes);
		assertByteArrayEqualsString(this.os);
	}

	@Test
	void reset() throws Exception {
		this.os.write(this.helloBytes);
		assertByteArrayEqualsString(this.os);
		this.os.reset();
		assertThat(this.os.size()).isEqualTo(0);
		this.os.write(this.helloBytes);
		assertByteArrayEqualsString(this.os);
	}

	@Test
	void close() {
		this.os.close();
		assertThatIOException().isThrownBy(() -> this.os.write(this.helloBytes));
	}

	@Test
	void toByteArrayUnsafe() throws Exception {
		this.os.write(this.helloBytes);
		assertByteArrayEqualsString(this.os);
		assertThat(this.os.toByteArrayUnsafe()).isSameAs(this.os.toByteArrayUnsafe());
		assertThat(this.helloBytes).isEqualTo(this.os.toByteArray());
	}

	@Test
	void writeTo() throws Exception {
		this.os.write(this.helloBytes);
		assertByteArrayEqualsString(this.os);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		this.os.writeTo(baos);
		assertThat(this.helloBytes).isEqualTo(baos.toByteArray());
	}

	@Test
	void failResize() throws Exception {
		this.os.write(this.helloBytes);
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.os.resize(5))
			.withMessage("New capacity must not be smaller than current size");
	}

	@Test
	void getInputStream() throws Exception {
		this.os.write(this.helloBytes);
		assertThat(this.os.getInputStream()).isNotNull();
	}

	@Test
	void getInputStreamAvailable() throws Exception {
		this.os.write(this.helloBytes);
		assertThat(this.helloBytes).hasSize(this.os.getInputStream().available());
	}

	@Test
	void getInputStreamRead() throws Exception {
		this.os.write(this.helloBytes);
		InputStream inputStream = this.os.getInputStream();
		assertThat(this.helloBytes[0]).isEqualTo((byte) inputStream.read());
		assertThat(this.helloBytes[1]).isEqualTo((byte) inputStream.read());
		assertThat(this.helloBytes[2]).isEqualTo((byte) inputStream.read());
		assertThat(this.helloBytes[3]).isEqualTo((byte) inputStream.read());
	}

	@Test
	void getInputStreamReadBytePromotion() throws Exception {
		byte[] bytes = { -1 };
		this.os.write(bytes);
		InputStream inputStream = this.os.getInputStream();
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		assertThat(inputStream.read()).isEqualTo(bais.read());
	}

	@Test
	void getInputStreamReadAll() throws Exception {
		this.os.write(this.helloBytes);
		InputStream inputStream = this.os.getInputStream();
		byte[] actual = new byte[inputStream.available()];
		int bytesRead = inputStream.read(actual);
		assertThat(bytesRead).isEqualTo(this.helloBytes.length);
		assertThat(actual).isEqualTo(this.helloBytes);
		assertThat(inputStream.available()).isEqualTo(0);
	}

	@Test
	void getInputStreamReadBeyondEndOfStream() throws Exception {
		this.os.write(this.helloBytes);
		InputStream inputStream = os.getInputStream();
		byte[] actual = new byte[inputStream.available() + 1];
		int bytesRead = inputStream.read(actual);
		assertThat(bytesRead).isEqualTo(this.helloBytes.length);
		for (int i = 0; i < bytesRead; i++) {
			assertThat(actual[i]).isEqualTo(this.helloBytes[i]);
		}
		assertThat(actual[this.helloBytes.length]).isEqualTo((byte) 0);
		assertThat(inputStream.available()).isEqualTo(0);
	}

	@Test
	void getInputStreamSkip() throws Exception {
		this.os.write(this.helloBytes);
		InputStream inputStream = this.os.getInputStream();
		assertThat(this.helloBytes[0]).isEqualTo((byte) inputStream.read());
		assertThat(inputStream.skip(1)).isEqualTo(1);
		assertThat(this.helloBytes[2]).isEqualTo((byte) inputStream.read());
		assertThat(inputStream.available()).isEqualTo((this.helloBytes.length - 3));
	}

	@Test
	void getInputStreamSkipAll() throws Exception {
		this.os.write(this.helloBytes);
		InputStream inputStream = this.os.getInputStream();
		assertThat(this.helloBytes.length).isEqualTo(inputStream.skip(1000));
		assertThat(inputStream.available()).isEqualTo(0);
	}

	@Test
	void updateMessageDigest() throws Exception {
		StringBuilder builder = new StringBuilder("\"0");
		this.os.write(this.helloBytes);
		InputStream inputStream = this.os.getInputStream();
		DigestUtils.appendMd5DigestAsHex(inputStream, builder);
		builder.append('"');
		String actual = builder.toString();
		assertThat(actual).isEqualTo("\"0b10a8db164e0754105b7a99be72e3fe5\"");
	}

	@Test
	void updateMessageDigestManyBuffers() throws Exception {
		StringBuilder builder = new StringBuilder("\"0");
		// filling at least one 256 buffer
		for ( int i = 0; i < 30; i++) {
			this.os.write(this.helloBytes);
		}
		InputStream inputStream = this.os.getInputStream();
		DigestUtils.appendMd5DigestAsHex(inputStream, builder);
		builder.append('"');
		String actual = builder.toString();
		assertThat(actual).isEqualTo("\"06225ca1e4533354c516e74512065331d\"");
	}


	private void assertByteArrayEqualsString(FastByteArrayOutputStream actual) {
		assertThat(actual.toByteArray()).isEqualTo(this.helloBytes);
	}

}
