/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.adapter;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.junit.Before;
import org.junit.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Test fixture for {@link StandardEndpointAdapter}.
 *
 * @author Rossen Stoyanchev
 */
public class StandardEndpointAdapterTests {

	private StandardEndpointAdapter adapter;

	private WebSocketHandler webSocketHandler;

	private StandardWebSocketSessionAdapter webSocketSession;

	private Session session;


	@Before
	public void setup() {
		this.session = mock(Session.class);
		this.webSocketHandler = mock(WebSocketHandler.class);
		this.webSocketSession = new StandardWebSocketSessionAdapter();
		this.adapter = new StandardEndpointAdapter(this.webSocketHandler, this.webSocketSession);
	}

	@Test
	public void onOpen() throws Throwable {
		this.adapter.onOpen(this.session, null);

		verify(this.webSocketHandler).afterConnectionEstablished(this.webSocketSession);
		verify(this.session, atLeast(2)).addMessageHandler(any(MessageHandler.Whole.class));

		when(this.session.getId()).thenReturn("123");
		assertEquals("123", this.webSocketSession.getId());
	}

	@Test
	public void onClose() throws Throwable {
		this.adapter.onClose(this.session, new CloseReason(CloseCodes.NORMAL_CLOSURE, "reason"));
		verify(this.webSocketHandler).afterConnectionClosed(this.webSocketSession, CloseStatus.NORMAL.withReason("reason"));
	}

	@Test
	public void onError() throws Throwable {
		Exception exception = new Exception();
		this.adapter.onError(this.session, exception);
		verify(this.webSocketHandler).handleTransportError(this.webSocketSession, exception);
	}

}
