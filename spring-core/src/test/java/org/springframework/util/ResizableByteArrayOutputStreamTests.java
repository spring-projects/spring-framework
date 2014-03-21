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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Brian Clozel
 */
public class ResizableByteArrayOutputStreamTests {

	private ResizableByteArrayOutputStream baos;

	private String helloString;

	private byte[] helloBytes;

	private static final int INITIAL_SIZE = 32;

	@Before
	public void setUp() throws Exception {
		this.baos = new ResizableByteArrayOutputStream(INITIAL_SIZE);
		this.helloString = "Hello World";
		this.helloBytes = helloString.getBytes("UTF-8");
	}

	@Test
	public void resize() throws Exception {
		assertEquals(INITIAL_SIZE, this.baos.buffer.length);
		this.baos.write(helloBytes);
		int size = 64;
		this.baos.resize(size);
		assertEquals(size, this.baos.buffer.length);
		assertByteArrayEqualsString(helloString, this.baos);
	}

	@Test
	public void autoGrow() {
		assertEquals(INITIAL_SIZE, this.baos.buffer.length);
		for(int i= 0; i < 33; i++) {
			this.baos.write(0);
		}
		assertEquals(64, this.baos.buffer.length);
	}

	@Test
	public void grow() throws Exception {
		assertEquals(INITIAL_SIZE, this.baos.buffer.length);
		this.baos.write(helloBytes);
		this.baos.grow(100);
		assertEquals(this.helloString.length() + 100, this.baos.buffer.length);
		assertByteArrayEqualsString(helloString, this.baos);
	}

	@Test
	public void write() throws Exception{
		this.baos.write(helloBytes);
		assertByteArrayEqualsString(helloString, this.baos);
	}

	@Test(expected = IllegalArgumentException.class)
	public void failResize() throws Exception{
		this.baos.write(helloBytes);
		this.baos.resize(5);
	}

	private void assertByteArrayEqualsString(String expected, ResizableByteArrayOutputStream actual) {
		String actualString = new String(actual.buffer, 0, actual.count());
		assertEquals(expected, actualString);
	}

}
