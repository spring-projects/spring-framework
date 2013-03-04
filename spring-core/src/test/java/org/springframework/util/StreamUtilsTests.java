/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.util;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

/**
 * Tests for {@link StreamUtils}.
 *
 * @author Phillip Webb
 */
public class StreamUtilsTests {

	private byte[] bytes = new byte[StreamUtils.BUFFER_SIZE + 10];

	private String string = "";

	@Before
	public void setup() {
		new Random().nextBytes(bytes);
		while (string.length() < StreamUtils.BUFFER_SIZE + 10) {
			string += UUID.randomUUID().toString();
		}
	}

	@Test
	public void copyToByteArray() throws Exception {
		InputStream inputStream = spy(new ByteArrayInputStream(bytes));
		byte[] actual = StreamUtils.copyToByteArray(inputStream);
		assertThat(actual, equalTo(bytes));
		verify(inputStream, never()).close();
	}

	@Test
	public void copyToString() throws Exception {
		Charset charset = Charset.defaultCharset();
		InputStream inputStream = spy(new ByteArrayInputStream(string.getBytes(charset)));
		String actual = StreamUtils.copyToString(inputStream, charset);
		assertThat(actual, equalTo(string));
		verify(inputStream, never()).close();
	}

	@Test
	public void copyBytes() throws Exception {
		ByteArrayOutputStream out = spy(new ByteArrayOutputStream());
		StreamUtils.copy(bytes, out);
		assertThat(out.toByteArray(), equalTo(bytes));
		verify(out, never()).close();
	}

	@Test
	public void copyString() throws Exception {
		Charset charset = Charset.defaultCharset();
		ByteArrayOutputStream out = spy(new ByteArrayOutputStream());
		StreamUtils.copy(string, charset, out);
		assertThat(out.toByteArray(), equalTo(string.getBytes(charset)));
		verify(out, never()).close();
	}

	@Test
	public void copyStream() throws Exception {
		ByteArrayOutputStream out = spy(new ByteArrayOutputStream());
		StreamUtils.copy(new ByteArrayInputStream(bytes), out);
		assertThat(out.toByteArray(), equalTo(bytes));
		verify(out, never()).close();
	}

	@Test
	public void nonClosingInputStream() throws Exception {
		InputStream source = mock(InputStream.class);
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
	public void nonClosingOutputStream() throws Exception {
		OutputStream source = mock(OutputStream.class);
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
