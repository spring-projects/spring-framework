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

package org.springframework.core.io.buffer.support;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTests;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
class DataBufferTestUtilsTests extends AbstractDataBufferAllocatingTests {

	@ParameterizedDataBufferAllocatingTest
	void dumpBytes(String displayName, DataBufferFactory bufferFactory) {
		this.bufferFactory = bufferFactory;

		DataBuffer buffer = this.bufferFactory.allocateBuffer(4);
		byte[] source = {'a', 'b', 'c', 'd'};
		buffer.write(source);

		byte[] result = DataBufferTestUtils.dumpBytes(buffer);

		assertThat(result).isEqualTo(source);

		release(buffer);
	}

	@ParameterizedDataBufferAllocatingTest
	void dumpString(String displayName, DataBufferFactory bufferFactory) {
		this.bufferFactory = bufferFactory;

		DataBuffer buffer = this.bufferFactory.allocateBuffer(4);
		String source = "abcd";
		buffer.write(source.getBytes(StandardCharsets.UTF_8));

		String result = DataBufferTestUtils.dumpString(buffer, StandardCharsets.UTF_8);

		assertThat(result).isEqualTo(source);

		release(buffer);
	}

}
