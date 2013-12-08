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

package org.springframework.web.socket.sockjs.transport.session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.transport.SockJsServiceConfig;
import org.springframework.web.socket.sockjs.transport.session.WebSocketServerSockJsSessionTests.TestWebSocketServerSockJsSession;
import org.springframework.web.socket.handler.TestWebSocketSession;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Test fixture for {@link WebSocketServerSockJsSession}.
 *
 * @author Rossen Stoyanchev
 */
public class WebSocketServerSockJsSessionTests extends BaseAbstractSockJsSessionTests<TestWebSocketServerSockJsSession> {

	private TestWebSocketSession webSocketSession;


	@Before
	public void setup() {
		super.setUp();
		this.webSocketSession = new TestWebSocketSession();
		this.webSocketSession.setOpen(true);
	}

	@Override
	protected TestWebSocketServerSockJsSession initSockJsSession() {
		return new TestWebSocketServerSockJsSession(this.sockJsConfig, this.webSocketHandler,
				Collections.<String, Object>emptyMap());
	}

	@Test
	public void isActive() throws Exception {
		assertFalse(this.session.isActive());

		this.session.initializeDelegateSession(this.webSocketSession);
		assertTrue(this.session.isActive());

		this.webSocketSession.setOpen(false);
		assertFalse(this.session.isActive());
	}

	@Test
	public void afterSessionInitialized() throws Exception {

		this.session.initializeDelegateSession(this.webSocketSession);

		assertEquals("Open frame not sent",
				Collections.singletonList(new TextMessage("o")), this.webSocketSession.getSentMessages());

		assertEquals(Arrays.asList("schedule"), this.session.heartbeatSchedulingEvents);
		verify(this.webSocketHandler).afterConnectionEstablished(this.session);
		verifyNoMoreInteractions(this.taskScheduler, this.webSocketHandler);
	}

	@Test
	public void handleMessageEmptyPayload() throws Exception {
		this.session.handleMessage(new TextMessage(""), this.webSocketSession);
		verifyNoMoreInteractions(this.webSocketHandler);
	}

	@Test
	public void handleMessage() throws Exception {

		TextMessage message = new TextMessage("[\"x\"]");
		this.session.handleMessage(message, this.webSocketSession);

		verify(this.webSocketHandler).handleMessage(this.session, new TextMessage("x"));
		verifyNoMoreInteractions(this.webSocketHandler);
	}

	@Test
	public void handleMessageBadData() throws Exception {
		TextMessage message = new TextMessage("[\"x]");
		this.session.handleMessage(message, this.webSocketSession);

		this.session.isClosed();
		verify(this.webSocketHandler).handleTransportError(same(this.session), any(IOException.class));
		verifyNoMoreInteractions(this.webSocketHandler);
	}

	@Test
	public void sendMessageInternal() throws Exception {

		this.session.initializeDelegateSession(this.webSocketSession);
		this.session.sendMessageInternal("x");

		assertEquals(Arrays.asList(new TextMessage("o"), new TextMessage("a[\"x\"]")),
				this.webSocketSession.getSentMessages());

		assertEquals(Arrays.asList("schedule", "cancel", "schedule"), this.session.heartbeatSchedulingEvents);
	}

	@Test
	public void disconnect() throws Exception {

		this.session.initializeDelegateSession(this.webSocketSession);
		this.session.close(CloseStatus.NOT_ACCEPTABLE);

		assertEquals(CloseStatus.NOT_ACCEPTABLE, this.webSocketSession.getCloseStatus());
	}


	static class TestWebSocketServerSockJsSession extends WebSocketServerSockJsSession {

		private final List<String> heartbeatSchedulingEvents = new ArrayList<>();

		public TestWebSocketServerSockJsSession(SockJsServiceConfig config, WebSocketHandler handler,
				Map<String, Object> attributes) {

			super("1", config, handler, attributes);
		}

		@Override
		protected void scheduleHeartbeat() {
			this.heartbeatSchedulingEvents.add("schedule");
		}

		@Override
		protected void cancelHeartbeat() {
			this.heartbeatSchedulingEvents.add("cancel");
		}
	}

}
