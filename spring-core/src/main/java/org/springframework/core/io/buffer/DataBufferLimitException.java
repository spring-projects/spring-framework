/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.io.buffer;

/**
 * Exception that indicates the cumulative number of bytes consumed from a
 * stream of {@link DataBuffer DataBuffer}'s exceeded some pre-configured limit.
 * This can be raised when data buffers are cached and aggregated, for example,
 * {@link DataBufferUtils#join}. Or it could also be raised when data buffers
 * have been released but a parsed representation is being aggregated, for example, async
 * parsing with Jackson, SSE parsing and aggregating lines per event.
 *
 * @author Rossen Stoyanchev
 * @since 5.1.11
 */
@SuppressWarnings("serial")
public class DataBufferLimitException extends IllegalStateException {


	/**
	 * Create an instance with the given message.
	 */
	public DataBufferLimitException(String message) {
		super(message);
	}

	/**
	 * Create an instance with a message and a cause, e.g. {@link OutOfMemoryError}.
	 * @since 6.2.12
	 */
	public DataBufferLimitException(String message, Throwable cause) {
		super(message, cause);
	}

}
