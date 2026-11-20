/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.support.converter;

import org.jspecify.annotations.Nullable;

import org.springframework.jms.JmsException;

/**
 * Thrown by {@link MessageConverter} implementations when the conversion
 * of an object to/from a {@link jakarta.jms.Message} fails.
 *
 * @author Mark Pollack
 * @since 1.1
 * @see MessageConverter
 */
@SuppressWarnings("serial")
public class MessageConversionException extends JmsException {

	/**
	 * Create a new MessageConversionException.
	 * @param msg the detail message
	 */
	public MessageConversionException(String msg) {
		super(msg);
	}

	/**
	 * Create a new MessageConversionException.
	 * @param msg the detail message
	 * @param cause the root cause (if any)
	 */
	public MessageConversionException(String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
