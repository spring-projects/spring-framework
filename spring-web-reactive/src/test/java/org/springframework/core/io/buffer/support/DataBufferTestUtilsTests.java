/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.io.buffer.support;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferAllocator;
import org.springframework.core.io.buffer.DefaultDataBufferAllocator;
import org.springframework.core.io.buffer.NettyDataBufferAllocator;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Arjen Poutsma
 */
@RunWith(Parameterized.class)
public class DataBufferTestUtilsTests {

	@Parameterized.Parameter
	public DataBufferAllocator allocator;

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] buffers() {

		return new Object[][]{
				{new NettyDataBufferAllocator(new UnpooledByteBufAllocator(true))},
				{new NettyDataBufferAllocator(new UnpooledByteBufAllocator(false))},
				{new NettyDataBufferAllocator(new PooledByteBufAllocator(true))},
				{new NettyDataBufferAllocator(new PooledByteBufAllocator(false))},
				{new DefaultDataBufferAllocator(true)},
				{new DefaultDataBufferAllocator(false)}};
	}

	@Test
	public void dumpBytes() {
		DataBuffer buffer = allocator.allocateBuffer(4);
		byte[] source = {'a', 'b', 'c', 'd'};
		buffer.write(source);

		byte[] result = DataBufferTestUtils.dumpBytes(buffer);

		assertArrayEquals(source, result);
	}

	@Test
	public void dumpString() {
		DataBuffer buffer = allocator.allocateBuffer(4);
		String source = "abcd";
		buffer.write(source.getBytes(StandardCharsets.UTF_8));

		String result = DataBufferTestUtils.dumpString(buffer, StandardCharsets.UTF_8);


		assertEquals(source, result);
	}

}