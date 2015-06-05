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

package org.springframework.rx.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Arjen Poutsma
 */
public class BlockingByteBufQueueTests {

	private BlockingByteBufQueue queue;

	@Before
	public void setUp() throws Exception {
		queue = new BlockingByteBufQueue();
	}

	@Test
	public void normal() throws Exception {
		ByteBuf abc = Unpooled.copiedBuffer(new byte[]{'a', 'b', 'c'});
		ByteBuf def = Unpooled.copiedBuffer(new byte[]{'d', 'e', 'f'});

		queue.putBuffer(abc);
		queue.putBuffer(def);
		queue.complete();

		assertTrue(queue.isHeadBuffer());
		assertFalse(queue.isHeadError());
		assertSame(abc, queue.pollBuffer());

		assertTrue(queue.isHeadBuffer());
		assertFalse(queue.isHeadError());
		assertSame(def, queue.pollBuffer());

		assertTrue(queue.isComplete());
	}

	@Test
	public void empty() throws Exception {
		assertNull(queue.pollBuffer());
	}

	@Test
	public void error() throws Exception {
		ByteBuf abc = Unpooled.copiedBuffer(new byte[]{'a', 'b', 'c'});
		Throwable error = new IllegalStateException();

		queue.putBuffer(abc);
		queue.putError(error);
		queue.complete();

		assertTrue(queue.isHeadBuffer());
		assertFalse(queue.isHeadError());
		assertSame(abc, queue.pollBuffer());

		assertTrue(queue.isHeadError());
		assertFalse(queue.isHeadBuffer());
		assertSame(error, queue.pollError());

		assertTrue(queue.isComplete());
	}
}