/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp.stomp;

/**
 * Raised when the connection for a STOMP session is lost rather than closed.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
@SuppressWarnings("serial")
public class ConnectionLostException extends RuntimeException {

	public ConnectionLostException(String message) {
		super(message);
	}

	public ConnectionLostException(String message, Throwable cause) {
		super(message, cause);
	}

}
