/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.aot;

import org.jspecify.annotations.Nullable;

/**
 * Throw when an AOT processor failed.
 *
 * @author Stephane Nicoll
 * @since 6.2
 */
@SuppressWarnings("serial")
public class AotProcessingException extends AotException {

	/**
	 * Create a new instance with the detail message and a root cause, if any.
	 * @param msg the detail message
	 * @param cause the root cause, if any
	 */
	public AotProcessingException(String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
