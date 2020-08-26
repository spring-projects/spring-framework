/*
 * Copyright 2002-2019 the original author or authors.
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
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test fixture for {@link org.springframework.web.socket.adapter.jetty.JettyWebSocketHandlerAdapter}.
 *
 * @author Rossen Stoyanchev
 */
public class JettyWebSocketHandlerAdapterTests {

	private JettyWebSocketHandlerAdapter adapter;

	private WebSocketHandler webSocketHandler;

	private JettyWebSocketSession webSocketSession;

	private Session session;


	@BeforeEach
	public void setup() {
		this.session = mock(Session.class);
		given(this.session.getUpgradeRequest()).willReturn(Mockito.mock(UpgradeRequest.class));
		given(this.session.getUpgradeResponse()).willReturn(Mockito.mock(UpgradeResponse.class));

		this.webSocketHandler = mock(WebSocketHandler.class);
		this.webSocketSession = new JettyWebSocketSession(null, null);
		this.adapter = new JettyWebSocketHandlerAdapter(this.webSocketHandler, this.webSocketSession);
	}

	@Test
	public void onOpen() throws Throwable {
		this.adapter.onWebSocketConnect(this.session);
		verify(this.webSocketHandler).afterConnectionEstablished(this.webSocketSession);
	}

	@Test
	public void onClose() throws Throwable {
		this.adapter.onWebSocketClose(1000, "reason");
		verify(this.webSocketHandler).afterConnectionClosed(this.webSocketSession, CloseStatus.NORMAL.withReason("reason"));
	}

	@Test
	public void onError() throws Throwable {
		Exception exception = new Exception();
		this.adapter.onWebSocketError(exception);
		verify(this.webSocketHandler).handleTransportError(this.webSocketSession, exception);
	}

}
