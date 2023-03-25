/*
 * Copyright 2002-2022 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.core.testfixture.io.buffer.LeakAwareDataBufferFactory;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.core.io.buffer.DataBufferUtils.release;

/**
 * @author Arjen Poutsma
 */
class LeakAwareDataBufferFactoryTests {

	private final LeakAwareDataBufferFactory bufferFactory = new LeakAwareDataBufferFactory();


	@Test
	void leak() {
		DataBuffer dataBuffer = this.bufferFactory.allocateBuffer();
		try {
			assertThatExceptionOfType(AssertionError.class).isThrownBy(this.bufferFactory::checkForLeaks);
		}
		finally {
			release(dataBuffer);
		}
	}

	@Test
	void noLeak() {
		DataBuffer dataBuffer = this.bufferFactory.allocateBuffer();
		release(dataBuffer);
		this.bufferFactory.checkForLeaks();
	}

}
