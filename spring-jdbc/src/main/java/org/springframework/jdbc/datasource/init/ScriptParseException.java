/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource.init;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.support.EncodedResource;

/**
 * Thrown by {@link ScriptUtils} if an SQL script cannot be properly parsed.
 *
 * @author Sam Brannen
 * @since 4.0.3
 */
@SuppressWarnings("serial")
public class ScriptParseException extends ScriptException {

	/**
	 * Create a new {@code ScriptParseException}.
	 * @param message detailed message
	 * @param resource the resource from which the SQL script was read
	 */
	public ScriptParseException(String message, @Nullable EncodedResource resource) {
		super(buildMessage(message, resource));
	}

	/**
	 * Create a new {@code ScriptParseException}.
	 * @param message detailed message
	 * @param resource the resource from which the SQL script was read
	 * @param cause the underlying cause of the failure
	 */
	public ScriptParseException(String message, @Nullable EncodedResource resource, @Nullable Throwable cause) {
		super(buildMessage(message, resource), cause);
	}


	private static String buildMessage(String message, @Nullable EncodedResource resource) {
		return String.format("Failed to parse SQL script from resource [%s]: %s",
				(resource == null ? "<unknown>" : resource), message);
	}

}
