/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp.stomp;

import org.springframework.core.NestedRuntimeException;

/**
 * Raised after a failure to encode or decode a STOMP message.
 *
 * @author Gary Russell
 * @since 4.0
 */
@SuppressWarnings("serial")
public class StompConversionException extends NestedRuntimeException {


	public StompConversionException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public StompConversionException(String msg) {
		super(msg);
	}

}
