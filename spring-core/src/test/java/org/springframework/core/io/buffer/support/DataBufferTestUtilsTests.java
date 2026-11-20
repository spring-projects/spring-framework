/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.io.buffer.support;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.testfixture.io.buffer.AbstractDataBufferAllocatingTests;
import org.springframework.core.testfixture.io.buffer.DataBufferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
class DataBufferTestUtilsTests extends AbstractDataBufferAllocatingTests {

	@ParameterizedDataBufferAllocatingTest
	void dumpBytes(DataBufferFactory bufferFactory) {
		this.bufferFactory = bufferFactory;

		DataBuffer buffer = this.bufferFactory.allocateBuffer(4);
		byte[] source = {'a', 'b', 'c', 'd'};
		buffer.write(source);

		byte[] result = DataBufferTestUtils.dumpBytes(buffer);

		assertThat(result).isEqualTo(source);

		release(buffer);
	}

	@ParameterizedDataBufferAllocatingTest
	void dumpString(DataBufferFactory bufferFactory) {
		this.bufferFactory = bufferFactory;

		DataBuffer buffer = this.bufferFactory.allocateBuffer(4);
		String source = "abcd";
		buffer.write(source.getBytes(StandardCharsets.UTF_8));
		String result = buffer.toString(StandardCharsets.UTF_8);
		release(buffer);

		assertThat(result).isEqualTo(source);
	}

}
