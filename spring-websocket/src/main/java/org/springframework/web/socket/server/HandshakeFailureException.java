/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.server;

import org.springframework.core.NestedRuntimeException;

/**
 * Thrown when handshake processing failed to complete due to an internal, unrecoverable
 * error. This implies a server error (HTTP status code 500) as opposed to a failure in
 * the handshake negotiation.
 *
 * <p>By contrast, when handshake negotiation fails, the response status code will be 200
 * and the response headers and body will have been updated to reflect the cause for the
 * failure. A {@link HandshakeHandler} implementation will have protected methods to
 * customize updates to the response in those cases.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@SuppressWarnings("serial")
public class HandshakeFailureException extends NestedRuntimeException {

	public HandshakeFailureException(String message) {
		super(message);
	}

	public HandshakeFailureException(String message, Throwable cause) {
		super(message, cause);
	}

}
