/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot;

/**
 * Thrown if an error occurs during AOT build-time processing or AOT run-time
 * execution in the <em>Spring TestContext Framework</em>.
 *
 * @author Sam Brannen
 * @since 6.0
 */
@SuppressWarnings("serial")
public class TestContextAotException extends RuntimeException {

	/**
	 * Create a new {@code TestContextAotException}.
	 * @param message the detail message
	 */
	public TestContextAotException(String message) {
		super(message);
	}

	/**
	 * Create a new {@code TestContextAotException}.
	 * @param message the detail message
	 * @param cause the root cause
	 */
	public TestContextAotException(String message, Throwable cause) {
		super(message, cause);
	}

}
