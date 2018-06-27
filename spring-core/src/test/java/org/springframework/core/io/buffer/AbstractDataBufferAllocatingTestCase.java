/*
 * Copyright 2002-2017 the original author or authors.
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
import java.util.List;
import java.util.function.Consumer;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PoolArenaMetric;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocatorMetric;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.Rule;
import org.junit.rules.Verifier;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.springframework.core.io.buffer.support.DataBufferTestUtils;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
@RunWith(Parameterized.class)
public abstract class AbstractDataBufferAllocatingTestCase {

	@Parameterized.Parameter
	public DataBufferFactory bufferFactory;

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] dataBufferFactories() {
		return new Object[][] {
				{new NettyDataBufferFactory(new UnpooledByteBufAllocator(true))},
				{new NettyDataBufferFactory(new UnpooledByteBufAllocator(false))},
				// disable caching for reliable leak detection, see https://github.com/netty/netty/issues/5275
				{new NettyDataBufferFactory(new PooledByteBufAllocator(true, 1, 1, 8192, 11, 0, 0, 0, true))},
				{new NettyDataBufferFactory(new PooledByteBufAllocator(false, 1, 1, 8192, 11, 0, 0, 0, true))},
				{new DefaultDataBufferFactory(true)},
				{new DefaultDataBufferFactory(false)}

		};
	}

	@Rule
	public final Verifier leakDetector = new LeakDetector();

	protected DataBuffer createDataBuffer(int capacity) {
		return this.bufferFactory.allocateBuffer(capacity);
	}

	protected DataBuffer stringBuffer(String value) {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		DataBuffer buffer = this.bufferFactory.allocateBuffer(bytes.length);
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
			DataBufferUtils.release(dataBuffer);
			assertEquals(expected, value);
		};
	}


	private class LeakDetector extends Verifier {

		@Override
		protected void verify() throws Throwable {
			if (bufferFactory instanceof NettyDataBufferFactory) {
				ByteBufAllocator byteBufAllocator =
						((NettyDataBufferFactory) bufferFactory).getByteBufAllocator();
				if (byteBufAllocator instanceof PooledByteBufAllocator) {
					PooledByteBufAllocator pooledByteBufAllocator =
							(PooledByteBufAllocator) byteBufAllocator;
					PooledByteBufAllocatorMetric metric = pooledByteBufAllocator.metric();
					long allocations = calculateAllocations(metric.directArenas()) +
							calculateAllocations(metric.heapArenas());
					assertTrue("ByteBuf leak detected: " + allocations +
							" allocations were not released", allocations == 0);
				}
			}
		}

		private long calculateAllocations(List<PoolArenaMetric> metrics) {
			return metrics.stream().mapToLong(PoolArenaMetric::numActiveAllocations).sum();
		}

	}

}
