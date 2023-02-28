/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.adapter.jetty;

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
		this.adapter.onWebSocketConnect(this.session);
		verify(this.webSocketHandler).afterConnectionEstablished(this.webSocketSession);
	}

	@Test
	void onClose() throws Exception {
		this.adapter.onWebSocketClose(1000, "reason");
		verify(this.webSocketHandler).afterConnectionClosed(this.webSocketSession, CloseStatus.NORMAL.withReason("reason"));
	}

	@Test
	void onError() throws Exception {
		Exception exception = new Exception();
		this.adapter.onWebSocketError(exception);
		verify(this.webSocketHandler).handleTransportError(this.webSocketSession, exception);
	}

}
