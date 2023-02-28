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

package org.springframework.core.testfixture.io.buffer;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;

import org.springframework.core.io.buffer.DataBufferFactory;

/**
 * Abstract base class for unit tests that allocate data buffers via a {@link DataBufferFactory}.
 * After each unit test, this base class checks whether all created buffers have been released,
 * throwing an {@link AssertionError} if not.
 *
 * @author Arjen Poutsma
 * @since 5.1.3
 * @see LeakAwareDataBufferFactory
 */
public abstract class AbstractLeakCheckingTests {

	/**
	 * The data buffer factory.
	 */
	protected final LeakAwareDataBufferFactory bufferFactory = new LeakAwareDataBufferFactory();

	/**
	 * Checks whether any of the data buffers created by {@link #bufferFactory} have not been
	 * released, throwing an assertion error if so.
	 */
	@AfterEach
	final void checkForLeaks() {
		this.bufferFactory.checkForLeaks(Duration.ofSeconds(1));
	}

}
