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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Arjen Poutsma
 */
@RunWith(Parameterized.class)
public class PooledDataBufferTests {

	@Parameterized.Parameter
	public DataBufferFactory dataBufferFactory;

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] buffers() {

		return new Object[][]{
				{new NettyDataBufferFactory(new UnpooledByteBufAllocator(true))},
				{new NettyDataBufferFactory(new UnpooledByteBufAllocator(false))},
				{new NettyDataBufferFactory(new PooledByteBufAllocator(true))},
				{new NettyDataBufferFactory(new PooledByteBufAllocator(false))}};
	}

	private PooledDataBuffer createDataBuffer(int capacity) {
		return (PooledDataBuffer) dataBufferFactory.allocateBuffer(capacity);
	}

	@Test
	public void retainAndRelease() {
		PooledDataBuffer buffer = createDataBuffer(1);
		buffer.write((byte) 'a');

		buffer.retain();
		boolean result = buffer.release();
		assertThat(result).isFalse();
		result = buffer.release();
		assertThat(result).isTrue();
	}

	@Test
	public void tooManyReleases() {
		PooledDataBuffer buffer = createDataBuffer(1);
		buffer.write((byte) 'a');

		buffer.release();
		assertThatIllegalStateException().isThrownBy(
				buffer::release);
	}


}
