/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.sockjs;

import org.jspecify.annotations.Nullable;

import org.springframework.core.NestedRuntimeException;

/**
 * Base class for exceptions raised while processing SockJS HTTP requests.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@SuppressWarnings("serial")
public class SockJsException extends NestedRuntimeException {

	private final @Nullable String sessionId;


	/**
	 * Constructor for SockJsException.
	 * @param message the exception message
	 * @param cause the root cause
	 */
	public SockJsException(String message, @Nullable Throwable cause) {
		this(message, null, cause);
	}

	/**
	 * Constructor for SockJsException.
	 * @param message the exception message
	 * @param sessionId the SockJS session id
	 * @param cause the root cause
	 */
	public SockJsException(String message, @Nullable String sessionId, @Nullable Throwable cause) {
		super(message, cause);
		this.sessionId = sessionId;
	}


	/**
	 * Return the SockJS session id.
	 */
	public @Nullable String getSockJsSessionId() {
		return this.sessionId;
	}

}
