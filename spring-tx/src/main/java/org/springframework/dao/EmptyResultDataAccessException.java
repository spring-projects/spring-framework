/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.dao;

import org.jspecify.annotations.Nullable;

/**
 * Data access exception thrown when a result was expected to have at least
 * one row (or element) but zero rows (or elements) were actually returned.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see IncorrectResultSizeDataAccessException
 */
@SuppressWarnings("serial")
public class EmptyResultDataAccessException extends IncorrectResultSizeDataAccessException {

	/**
	 * Constructor for EmptyResultDataAccessException.
	 * @param expectedSize the expected result size
	 */
	public EmptyResultDataAccessException(int expectedSize) {
		super(expectedSize, 0);
	}

	/**
	 * Constructor for EmptyResultDataAccessException.
	 * @param msg the detail message
	 * @param expectedSize the expected result size
	 */
	public EmptyResultDataAccessException(@Nullable String msg, int expectedSize) {
		super(msg, expectedSize, 0);
	}

	/**
	 * Constructor for EmptyResultDataAccessException.
	 * @param msg the detail message
	 * @param expectedSize the expected result size
	 * @param ex the wrapped exception
	 */
	public EmptyResultDataAccessException(@Nullable String msg, int expectedSize, Throwable ex) {
		super(msg, expectedSize, 0, ex);
	}

}
