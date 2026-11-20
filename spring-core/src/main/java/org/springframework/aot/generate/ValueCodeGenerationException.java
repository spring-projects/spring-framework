/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.generate;

import org.jspecify.annotations.Nullable;

/**
 * Thrown when value code generation fails.
 *
 * @author Stephane Nicoll
 * @since 6.1.2
 */
@SuppressWarnings("serial")
public class ValueCodeGenerationException extends RuntimeException {

	private final @Nullable Object value;


	protected ValueCodeGenerationException(String message, @Nullable Object value, @Nullable Throwable cause) {
		super(message, cause);
		this.value = value;
	}

	public ValueCodeGenerationException(@Nullable Object value, Throwable cause) {
		super(buildErrorMessage(value), cause);
		this.value = value;
	}

	private static String buildErrorMessage(@Nullable Object value) {
		StringBuilder message = new StringBuilder("Failed to generate code for '");
		message.append(value).append("'");
		if (value != null) {
			message.append(" with type ").append(value.getClass());
		}
		return message.toString();
	}


	/**
	 * Return the value that failed to be generated.
	 */
	public @Nullable Object getValue() {
		return this.value;
	}

}
