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

package org.springframework.core.io.buffer;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.core.io.buffer.support.DataBufferUtils;

import static org.junit.Assert.assertEquals;

/**
 * @author Arjen Poutsma
 */
@RunWith(Parameterized.class)
public abstract class AbstractDataBufferAllocatingTestCase {

	@Parameterized.Parameter
	public DataBufferAllocator allocator;

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] allocators() {
		return new Object[][]{
				{new NettyDataBufferAllocator(new UnpooledByteBufAllocator(true))},
				{new NettyDataBufferAllocator(new UnpooledByteBufAllocator(false))},
				{new NettyDataBufferAllocator(new PooledByteBufAllocator(true))},
				{new NettyDataBufferAllocator(new PooledByteBufAllocator(false))},
				{new DefaultDataBufferAllocator(true)},
				{new DefaultDataBufferAllocator(false)}

		};
	}

	protected DataBuffer createDataBuffer(int capacity) {
		return this.allocator.allocateBuffer(capacity);
	}

	protected DataBuffer stringBuffer(String value) {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		DataBuffer buffer = this.allocator.allocateBuffer(bytes.length);
		buffer.write(bytes);
		return buffer;
	}

	protected void release(DataBuffer... buffers) {
		Arrays.stream(buffers).forEach(DataBufferUtils::release);
	}

	protected Consumer<DataBuffer> stringConsumer(String expected) {
		return dataBuffer -> {
			String value =
					DataBufferTestUtils.dumpString(dataBuffer, StandardCharsets.UTF_8);
			assertEquals(expected, value);
			DataBufferUtils.release(dataBuffer);
		};
	}

}
