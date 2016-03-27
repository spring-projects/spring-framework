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

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

/**
 * Test suite for {@link FastByteArrayOutputStream}
 * @author Craig Andrews
 */
public class FastByteArrayOutputStreamTests {

	private static final int INITIAL_CAPACITY = 256;

	private FastByteArrayOutputStream os;

	private byte[] helloBytes;

	@Before
	public void setUp() throws Exception {
		this.os = new FastByteArrayOutputStream(INITIAL_CAPACITY);
		this.helloBytes = "Hello World".getBytes("UTF-8");
	}

	@Test
	public void size() throws Exception {
		this.os.write(helloBytes);
		assertEquals(this.os.size(), helloBytes.length);
	}

	@Test
	public void resize() throws Exception {
		this.os.write(helloBytes);
		int sizeBefore = this.os.size();
		this.os.resize(64);
		assertByteArrayEqualsString(this.os);
		assertEquals(sizeBefore, this.os.size());
	}

	@Test
	public void autoGrow() throws IOException {
		this.os.resize(1);
		for (int i = 0; i < 10; i++) {
			this.os.write(1);
		}
		assertEquals(10, this.os.size());
		assertArrayEquals(this.os.toByteArray(), new byte[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1});
	}

	@Test
	public void write() throws Exception {
		this.os.write(helloBytes);
		assertByteArrayEqualsString(this.os);
	}

	@Test
	public void reset() throws Exception {
		this.os.write(helloBytes);
		assertByteArrayEqualsString(this.os);
		this.os.reset();
		assertEquals(0, this.os.size());
		this.os.write(helloBytes);
		assertByteArrayEqualsString(this.os);
	}

	@Test(expected = IOException.class)
	public void close() throws Exception {
		this.os.close();
		this.os.write(helloBytes);
	}

	@Test
	public void toByteArrayUnsafe() throws Exception {
		this.os.write(helloBytes);
		assertByteArrayEqualsString(this.os);
		assertSame(this.os.toByteArrayUnsafe(), this.os.toByteArrayUnsafe());
		assertArrayEquals(this.os.toByteArray(), helloBytes);
	}

	@Test
	public void writeTo() throws Exception {
		this.os.write(helloBytes);
		assertByteArrayEqualsString(this.os);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		this.os.writeTo(baos);
		assertArrayEquals(baos.toByteArray(), helloBytes);
	}

	@Test(expected = IllegalArgumentException.class)
	public void failResize() throws Exception {
		this.os.write(helloBytes);
		this.os.resize(5);
	}

	@Test
	public void getInputStream() throws Exception {
		this.os.write(helloBytes);
		assertNotNull(this.os.getInputStream());
	}

	@Test
	public void getInputStreamAvailable() throws Exception {
		this.os.write(helloBytes);
		assertEquals(this.os.getInputStream().available(), helloBytes.length);
	}

	@Test
	public void getInputStreamRead() throws Exception {
		this.os.write(helloBytes);
		InputStream inputStream = this.os.getInputStream();
		assertEquals(inputStream.read(), helloBytes[0]);
		assertEquals(inputStream.read(), helloBytes[1]);
		assertEquals(inputStream.read(), helloBytes[2]);
		assertEquals(inputStream.read(), helloBytes[3]);
	}

	@Test
	public void getInputStreamReadAll() throws Exception {
		this.os.write(helloBytes);
		InputStream inputStream = this.os.getInputStream();
		byte[] actual = new byte[inputStream.available()];
		int bytesRead = inputStream.read(actual);
		assertEquals(bytesRead, helloBytes.length);
		assertArrayEquals(actual, helloBytes);
		assertEquals(0, inputStream.available());
	}

	@Test
	public void getInputStreamSkip() throws Exception {
		this.os.write(helloBytes);
		InputStream inputStream = this.os.getInputStream();
		assertEquals(inputStream.read(), helloBytes[0]);
		assertEquals(inputStream.skip(1), 1);
		assertEquals(inputStream.read(), helloBytes[2]);
		assertEquals(helloBytes.length - 3, inputStream.available());
	}

	@Test
	public void getInputStreamSkipAll() throws Exception {
		this.os.write(helloBytes);
		InputStream inputStream = this.os.getInputStream();
		assertEquals(inputStream.skip(1000), helloBytes.length);
		assertEquals(0, inputStream.available());
	}

	@Test
	public void updateMessageDigest() throws Exception {
		StringBuilder builder = new StringBuilder("\"0");
		this.os.write(helloBytes);
		InputStream inputStream = this.os.getInputStream();
		DigestUtils.appendMd5DigestAsHex(inputStream, builder);
		builder.append("\"");
		String actual = builder.toString();
		assertEquals("\"0b10a8db164e0754105b7a99be72e3fe5\"", actual);
	}

	@Test
	public void updateMessageDigestManyBuffers() throws Exception {
		StringBuilder builder = new StringBuilder("\"0");
		// filling at least one 256 buffer
		for ( int i = 0; i < 30; i++) {
			this.os.write(helloBytes);
		}
		InputStream inputStream = this.os.getInputStream();
		DigestUtils.appendMd5DigestAsHex(inputStream, builder);
		builder.append("\"");
		String actual = builder.toString();
		assertEquals("\"06225ca1e4533354c516e74512065331d\"", actual);
	}

	private void assertByteArrayEqualsString(FastByteArrayOutputStream actual) {
		assertArrayEquals(helloBytes, actual.toByteArray());
	}

}
