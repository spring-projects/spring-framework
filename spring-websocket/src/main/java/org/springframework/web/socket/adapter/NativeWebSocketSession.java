/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.adapter;

import org.jspecify.annotations.Nullable;

import org.springframework.web.socket.WebSocketSession;

/**
 * A {@link WebSocketSession} that exposes the underlying, native WebSocketSession
 * through a getter.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface NativeWebSocketSession extends WebSocketSession {

	/**
	 * Return the underlying native WebSocketSession.
	 */
	Object getNativeSession();

	/**
	 * Return the underlying native WebSocketSession, if available.
	 * @param requiredType the required type of the session
	 * @return the native session of the required type,
	 * or {@code null} if not available
	 */
	<T> @Nullable T getNativeSession(@Nullable Class<T> requiredType);

}
