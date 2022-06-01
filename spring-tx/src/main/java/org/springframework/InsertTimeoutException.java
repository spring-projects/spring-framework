package org.springframework;

import org.springframework.dao.TransientDataAccessException;

/**
 * Exception to be thrown on an insert timeout. This could have different causes depending on
 * the database API in use but most likely thrown after the database interrupts or stops
 * the processing of an insert before it has completed.
 *
 * <p>This exception can be thrown by user code trapping the native database exception or
 * by exception translation.
 *
 * @author Peng Yin
 * @since 3.1
 */
@SuppressWarnings("serial")
public class InsertTimeoutException extends TransientDataAccessException {
	/**
	 * Constructor for InsertTimeoutException.
	 * @param msg the detail message
	 */
	public InsertTimeoutException(String msg) {
		super(msg);
	}
	/**
	 * Constructor for InsertTimeoutException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public InsertTimeoutException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
