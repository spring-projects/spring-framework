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

package org.springframework.reactive.util;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Arjen Poutsma
 */
public class BlockingByteBufQueueTests {

	private BlockingSignalQueue<byte[]> queue;

	@Before
	public void setUp() throws Exception {
		queue = new BlockingSignalQueue<byte[]>();
	}

	@Test
	public void normal() throws Exception {
		byte[] abc = new byte[]{'a', 'b', 'c'};
		byte[] def = new byte[]{'d', 'e', 'f'};

		queue.putSignal(abc);
		queue.putSignal(def);
		queue.complete();

		assertTrue(queue.isHeadSignal());
		assertFalse(queue.isHeadError());
		assertSame(abc, queue.pollSignal());

		assertTrue(queue.isHeadSignal());
		assertFalse(queue.isHeadError());
		assertSame(def, queue.pollSignal());

		assertTrue(queue.isComplete());
	}


	@Test
	public void error() throws Exception {
		byte[] abc = new byte[]{'a', 'b', 'c'};
		Throwable error = new IllegalStateException();

		queue.putSignal(abc);
		queue.putError(error);
		queue.complete();

		assertTrue(queue.isHeadSignal());
		assertFalse(queue.isHeadError());
		assertSame(abc, queue.pollSignal());

		assertTrue(queue.isHeadError());
		assertFalse(queue.isHeadSignal());
		assertSame(error, queue.pollError());

		assertTrue(queue.isComplete());
	}
}