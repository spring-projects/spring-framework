/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.sockjs.transport;

import java.util.Map;

import org.springframework.web.socket.WebSocketHandler;

/**
 * A factory for creating a SockJS session. {@link TransportHandler TransportHandlers} typically also serve
 * as SockJS session factories.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface SockJsSessionFactory {

	/**
	 * Create a new SockJS session.
	 * @param sessionId the ID of the session
	 * @param handler the underlying {@link WebSocketHandler}
	 * @param attributes handshake request specific attributes
	 * @return a new session, never {@code null}
	 */
	SockJsSession createSession(String sessionId, WebSocketHandler handler, Map<String, Object> attributes);

}
