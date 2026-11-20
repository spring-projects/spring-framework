/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.test.context.TestExecutionListener;

/**
 * {@code AotTestExecutionListener} is an extension of the {@link TestExecutionListener}
 * SPI that allows a listener to optionally provide ahead-of-time (AOT) support.
 *
 * @author Sam Brannen
 * @since 6.0
 */
public interface AotTestExecutionListener extends TestExecutionListener {

	/**
	 * Process the supplied test class ahead-of-time using the given
	 * {@link RuntimeHints} instance.
	 * <p>If possible, implementations should use the specified {@link ClassLoader}
	 * to determine if hints have to be contributed.
	 * @param runtimeHints the {@code RuntimeHints} to use
	 * @param testClass the test class to process
	 * @param classLoader the classloader to use
	 */
	void processAheadOfTime(RuntimeHints runtimeHints, Class<?> testClass, ClassLoader classLoader);

}
