/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
