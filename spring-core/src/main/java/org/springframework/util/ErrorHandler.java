/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util;

/**
 * A strategy for handling errors. This is especially useful for handling
 * errors that occur during asynchronous execution of tasks that have been
 * submitted to a TaskScheduler. In such cases, it may not be possible to
 * throw the error to the original caller.
 *
 * @author Mark Fisher
 * @since 3.0
 */
@FunctionalInterface
public interface ErrorHandler {

	/**
	 * Handle the given error, possibly rethrowing it as a fatal exception.
	 */
	void handleError(Throwable t);

}
