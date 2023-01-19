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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link StreamUtils}.
 *
 * @author Phillip Webb
 */
class StreamUtilsTests {

	private byte[] bytes = new byte[StreamUtils.BUFFER_SIZE + 10];

	private String string = "";

	@BeforeEach
	void setup() {
		new Random().nextBytes(bytes);
		while (string.length() < StreamUtils.BUFFER_SIZE + 10) {
			string += UUID.randomUUID().toString();
		}
	}

	@Test
	void copyToByteArray() throws Exception {
		InputStream inputStream = new ByteArrayInputStream(bytes);
		byte[] actual = StreamUtils.copyToByteArray(inputStream);
		assertThat(actual).isEqualTo(bytes);
	}

	@Test
	void copyToString() throws Exception {
		Charset charset = Charset.defaultCharset();
		InputStream inputStream = new ByteArrayInputStream(string.getBytes(charset));
		String actual = StreamUtils.copyToString(inputStream, charset);
		assertThat(actual).isEqualTo(string);
	}

	@Test
	void copyBytes() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		StreamUtils.copy(bytes, out);
		assertThat(out.toByteArray()).isEqualTo(bytes);
	}

	@Test
	void copyString() throws Exception {
		Charset charset = Charset.defaultCharset();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		StreamUtils.copy(string, charset, out);
		assertThat(out.toByteArray()).isEqualTo(string.getBytes(charset));
	}

	@Test
	void copyStream() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		StreamUtils.copy(new ByteArrayInputStream(bytes), out);
		assertThat(out.toByteArray()).isEqualTo(bytes);
	}

	@Test
	void copyRange() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		StreamUtils.copyRange(new ByteArrayInputStream(bytes), out, 0, 100);
		byte[] range = Arrays.copyOfRange(bytes, 0, 101);
		assertThat(out.toByteArray()).isEqualTo(range);
	}

	@Test
	void nonClosingInputStream() throws Exception {
		InputStream source = mock();
		InputStream nonClosing = StreamUtils.nonClosing(source);
		nonClosing.read();
		nonClosing.read(bytes);
		nonClosing.read(bytes, 1, 2);
		nonClosing.close();
		InOrder ordered = inOrder(source);
		ordered.verify(source).read();
		ordered.verify(source).read(bytes, 0, bytes.length);
		ordered.verify(source).read(bytes, 1, 2);
		ordered.verify(source, never()).close();
	}

	@Test
	void nonClosingOutputStream() throws Exception {
		OutputStream source = mock();
		OutputStream nonClosing = StreamUtils.nonClosing(source);
		nonClosing.write(1);
		nonClosing.write(bytes);
		nonClosing.write(bytes, 1, 2);
		nonClosing.close();
		InOrder ordered = inOrder(source);
		ordered.verify(source).write(1);
		ordered.verify(source).write(bytes, 0, bytes.length);
		ordered.verify(source).write(bytes, 1, 2);
		ordered.verify(source, never()).close();
	}
}
