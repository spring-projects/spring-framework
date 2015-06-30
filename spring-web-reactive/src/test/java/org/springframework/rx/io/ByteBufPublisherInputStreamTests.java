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

package org.springframework.rx.io;

import org.junit.Before;
import org.junit.Test;

import org.springframework.rx.util.BlockingSignalQueue;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class ByteBufPublisherInputStreamTests {

	private BlockingSignalQueue<byte[]> queue;

	private ByteArrayPublisherInputStream is;


	@Before
	public void setUp() throws Exception {
		queue = new BlockingSignalQueue<byte[]>();
		is = new ByteArrayPublisherInputStream(queue);

	}

	@Test
	public void readSingleByte() throws Exception {
		queue.putSignal(new byte[]{'a', 'b', 'c'});
		queue.putSignal(new byte[]{'d', 'e', 'f'});
		queue.complete();


		int ch = is.read();
		assertEquals('a', ch);
		ch = is.read();
		assertEquals('b', ch);
		ch = is.read();
		assertEquals('c', ch);

		ch = is.read();
		assertEquals('d', ch);
		ch = is.read();
		assertEquals('e', ch);
		ch = is.read();
		assertEquals('f', ch);

		ch = is.read();
		assertEquals(-1, ch);
	}

	@Test
	public void readBytes() throws Exception {
		queue.putSignal(new byte[]{'a', 'b', 'c'});
		queue.putSignal(new byte[]{'d', 'e', 'f'});
		queue.complete();

		byte[] buf = new byte[2];
		int read = this.is.read(buf);
		assertEquals(2, read);
		assertArrayEquals(new byte[] { 'a', 'b'}, buf);

		read = this.is.read(buf);
		assertEquals(1, read);
		assertEquals('c', buf[0]);

		read = this.is.read(buf);
		assertEquals(2, read);
		assertArrayEquals(new byte[] { 'd', 'e'}, buf);

		read = this.is.read(buf);
		assertEquals(1, read);
		assertEquals('f', buf[0]);

		read = this.is.read(buf);
		assertEquals(-1, read);
	}

}