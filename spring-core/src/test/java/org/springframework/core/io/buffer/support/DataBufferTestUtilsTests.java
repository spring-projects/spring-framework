/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core.io.buffer.support;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Arjen Poutsma
 */
public class DataBufferTestUtilsTests extends AbstractDataBufferAllocatingTestCase {

	@Test
	public void dumpBytes() {
		DataBuffer buffer = this.bufferFactory.allocateBuffer(4);
		byte[] source = {'a', 'b', 'c', 'd'};
		buffer.write(source);

		byte[] result = DataBufferTestUtils.dumpBytes(buffer);

		assertArrayEquals(source, result);

		release(buffer);
	}

	@Test
	public void dumpString() {
		DataBuffer buffer = this.bufferFactory.allocateBuffer(4);
		String source = "abcd";
		buffer.write(source.getBytes(StandardCharsets.UTF_8));

		String result = DataBufferTestUtils.dumpString(buffer, StandardCharsets.UTF_8);

		assertEquals(source, result);

		release(buffer);
	}

}
