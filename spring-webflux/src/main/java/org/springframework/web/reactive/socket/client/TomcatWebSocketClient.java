/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.socket.client;

import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import org.apache.tomcat.websocket.WsWebSocketContainer;
import reactor.core.publisher.Sinks;

import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.adapter.StandardWebSocketSession;
import org.springframework.web.reactive.socket.adapter.TomcatWebSocketSession;

/**
 * {@link WebSocketClient} implementation for use with Tomcat,
 * based on the Jakarta WebSocket API.
 *
 * @author Violeta Georgieva
 * @since 5.0
 * @see StandardWebSocketClient
 */
public class TomcatWebSocketClient extends StandardWebSocketClient {

	public TomcatWebSocketClient() {
		this(new WsWebSocketContainer());
	}

	public TomcatWebSocketClient(WebSocketContainer webSocketContainer) {
		super(webSocketContainer);
	}


	@Override
	protected StandardWebSocketSession createWebSocketSession(
			Session session, HandshakeInfo info, Sinks.Empty<Void> completionSink) {

		return new TomcatWebSocketSession(session, info, bufferFactory(), completionSink);
	}

}
