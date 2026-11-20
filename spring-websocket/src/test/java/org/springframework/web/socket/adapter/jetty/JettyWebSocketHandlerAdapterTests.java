/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.adapter.jetty;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link JettyWebSocketHandlerAdapter}.
 *
 * @author Rossen Stoyanchev
 */
class JettyWebSocketHandlerAdapterTests {

	private Session session = mock();

	private WebSocketHandler webSocketHandler = mock();

	private JettyWebSocketSession webSocketSession = new JettyWebSocketSession(null, null);

	private JettyWebSocketHandlerAdapter adapter = new JettyWebSocketHandlerAdapter(this.webSocketHandler, this.webSocketSession);


	@BeforeEach
	void setup() {
		given(this.session.getUpgradeRequest()).willReturn(mock());
		given(this.session.getUpgradeResponse()).willReturn(mock());
	}

	@Test
	void onOpen() throws Exception {
		this.adapter.onWebSocketOpen(this.session);
		verify(this.webSocketHandler).afterConnectionEstablished(this.webSocketSession);
	}

	@Test
	void onClose() throws Exception {
		this.adapter.onWebSocketClose(1000, "reason", Callback.NOOP);
		verify(this.webSocketHandler).afterConnectionClosed(this.webSocketSession, CloseStatus.NORMAL.withReason("reason"));
	}

	@Test
	void onError() throws Exception {
		Exception exception = new Exception();
		this.adapter.onWebSocketError(exception);
		verify(this.webSocketHandler).handleTransportError(this.webSocketSession, exception);
	}

}
