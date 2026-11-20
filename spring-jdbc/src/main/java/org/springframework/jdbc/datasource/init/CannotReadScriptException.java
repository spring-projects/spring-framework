/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource.init;

import org.springframework.core.io.support.EncodedResource;

/**
 * Thrown by {@link ScriptUtils} if an SQL script cannot be read.
 *
 * @author Keith Donald
 * @author Sam Brannen
 * @since 3.0
 */
@SuppressWarnings("serial")
public class CannotReadScriptException extends ScriptException {

	/**
	 * Create a new {@code CannotReadScriptException}.
	 * @param resource the resource that cannot be read from
	 * @param cause the underlying cause of the resource access failure
	 */
	public CannotReadScriptException(EncodedResource resource, Throwable cause) {
		super("Cannot read SQL script from " + resource, cause);
	}

}
