/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.sockjs;

import java.util.List;

/**
 * An exception thrown when a message frame was successfully received over an HTTP POST
 * and parsed but one or more of the messages it contained could not be delivered to the
 * WebSocketHandler either because the handler failed or because the connection got
 * closed.
 *
 * <p>The SockJS session is not automatically closed after this exception.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@SuppressWarnings("serial")
public class SockJsMessageDeliveryException extends SockJsException {

	private final List<String> undeliveredMessages;


	public SockJsMessageDeliveryException(String sessionId, List<String> undeliveredMessages, Throwable cause) {
		super("Failed to deliver message(s) " + undeliveredMessages + " for session " + sessionId, sessionId, cause);
		this.undeliveredMessages = undeliveredMessages;
	}

	public SockJsMessageDeliveryException(String sessionId, List<String> undeliveredMessages, String message) {
		super("Failed to deliver message(s) " + undeliveredMessages + " for session " + sessionId + ": " + message,
				sessionId, null);
		this.undeliveredMessages = undeliveredMessages;
	}

	public List<String> getUndeliveredMessages() {
		return this.undeliveredMessages;
	}

}
