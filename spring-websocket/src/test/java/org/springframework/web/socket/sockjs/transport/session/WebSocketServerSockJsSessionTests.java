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

package org.springframework.web.socket.sockjs.transport.session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TestWebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.sockjs.transport.SockJsServiceConfig;
import org.springframework.web.socket.sockjs.transport.session.WebSocketServerSockJsSessionTests.TestWebSocketServerSockJsSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit tests for {@link WebSocketServerSockJsSession}.
 *
 * @author Rossen Stoyanchev
 */
public class WebSocketServerSockJsSessionTests extends AbstractSockJsSessionTests<TestWebSocketServerSockJsSession> {

	private TestWebSocketSession webSocketSession;


	@BeforeEach
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
		assertThat(this.session.isActive()).isFalse();

		this.session.initializeDelegateSession(this.webSocketSession);
		assertThat(this.session.isActive()).isTrue();

		this.webSocketSession.setOpen(false);
		assertThat(this.session.isActive()).isFalse();
	}

	@Test
	public void afterSessionInitialized() throws Exception {
		this.session.initializeDelegateSession(this.webSocketSession);
		assertThat(this.webSocketSession.getSentMessages()).isEqualTo(Collections.singletonList(new TextMessage("o")));
		assertThat(this.session.heartbeatSchedulingEvents).isEqualTo(Arrays.asList("schedule"));
		verify(this.webSocketHandler).afterConnectionEstablished(this.session);
		verifyNoMoreInteractions(this.taskScheduler, this.webSocketHandler);
	}

	@Test
	@SuppressWarnings("resource")
	public void afterSessionInitializedOpenFrameFirst() throws Exception {
		TextWebSocketHandler handler = new TextWebSocketHandler() {
			@Override
			public void afterConnectionEstablished(WebSocketSession session) throws Exception {
				session.sendMessage(new TextMessage("go go"));
			}
		};
		TestWebSocketServerSockJsSession session = new TestWebSocketServerSockJsSession(this.sockJsConfig, handler, null);
		session.initializeDelegateSession(this.webSocketSession);
		List<TextMessage> expected = Arrays.asList(new TextMessage("o"), new TextMessage("a[\"go go\"]"));
		assertThat(this.webSocketSession.getSentMessages()).isEqualTo(expected);
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

		assertThat(this.webSocketSession.getSentMessages()).isEqualTo(Arrays.asList(new TextMessage("o"), new TextMessage("a[\"x\"]")));

		assertThat(this.session.heartbeatSchedulingEvents).isEqualTo(Arrays.asList("schedule", "cancel", "schedule"));
	}

	@Test
	public void disconnect() throws Exception {

		this.session.initializeDelegateSession(this.webSocketSession);
		this.session.close(CloseStatus.NOT_ACCEPTABLE);

		assertThat(this.webSocketSession.getCloseStatus()).isEqualTo(CloseStatus.NOT_ACCEPTABLE);
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
