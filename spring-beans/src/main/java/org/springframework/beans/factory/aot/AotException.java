/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.aot;

import org.jspecify.annotations.Nullable;

/**
 * Abstract superclass for all exceptions thrown by ahead-of-time processing.
 *
 * @author Stephane Nicoll
 * @since 6.2
 */
@SuppressWarnings("serial")
public abstract class AotException extends RuntimeException {

	/**
	 * Create an instance with the specified message and root cause.
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	protected AotException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
