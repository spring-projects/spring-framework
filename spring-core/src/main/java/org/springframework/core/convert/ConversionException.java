/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert;

import org.springframework.core.NestedRuntimeException;

/**
 * Base class for exceptions thrown by the conversion system.
 *
 * @author Keith Donald
 * @since 3.0
 */
@SuppressWarnings("serial")
public abstract class ConversionException extends NestedRuntimeException {

	/**
	 * Construct a new conversion exception.
	 * @param message the exception message
	 */
	public ConversionException(String message) {
		super(message);
	}

	/**
	 * Construct a new conversion exception.
	 * @param message the exception message
	 * @param cause the cause
	 */
	public ConversionException(String message, Throwable cause) {
		super(message, cause);
	}

}
