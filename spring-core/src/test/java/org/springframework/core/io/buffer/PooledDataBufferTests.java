/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.io.buffer;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
class PooledDataBufferTests {

	@Nested
	class UnpooledByteBufAllocatorWithPreferDirectTrueTests implements PooledDataBufferTestingTrait {

		@Override
		public DataBufferFactory createDataBufferFactory() {
			return new NettyDataBufferFactory(new UnpooledByteBufAllocator(true));
		}
	}

	@Nested
	class UnpooledByteBufAllocatorWithPreferDirectFalseTests implements PooledDataBufferTestingTrait {

		@Override
		public DataBufferFactory createDataBufferFactory() {
			return new NettyDataBufferFactory(new UnpooledByteBufAllocator(true));
		}
	}

	@Nested
	class PooledByteBufAllocatorWithPreferDirectTrueTests implements PooledDataBufferTestingTrait {

		@Override
		public DataBufferFactory createDataBufferFactory() {
			return new NettyDataBufferFactory(new PooledByteBufAllocator(true));
		}
	}

	@Nested
	class PooledByteBufAllocatorWithPreferDirectFalseTests implements PooledDataBufferTestingTrait {

		@Override
		public DataBufferFactory createDataBufferFactory() {
			return new NettyDataBufferFactory(new PooledByteBufAllocator(true));
		}
	}

	interface PooledDataBufferTestingTrait {

		DataBufferFactory createDataBufferFactory();

		default PooledDataBuffer createDataBuffer(int capacity) {
			return (PooledDataBuffer) createDataBufferFactory().allocateBuffer(capacity);
		}

		@Test
		default void retainAndRelease() {
			PooledDataBuffer buffer = createDataBuffer(1);
			buffer.write((byte) 'a');

			buffer.retain();
			assertThat(buffer.release()).isFalse();
			assertThat(buffer.release()).isTrue();
		}

		@Test
		default void tooManyReleases() {
			PooledDataBuffer buffer = createDataBuffer(1);
			buffer.write((byte) 'a');

			buffer.release();
			assertThatIllegalStateException().isThrownBy(buffer::release);
		}

	}

}
