/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.socket.adapter.standard;

import java.net.URI;

import jakarta.websocket.CloseReason;
import jakarta.websocket.CloseReason.CloseCodes;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import org.junit.jupiter.api.Test;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link StandardWebSocketHandlerAdapter}.
 *
 * @author Rossen Stoyanchev
 */
class StandardWebSocketHandlerAdapterTests {

	private WebSocketHandler webSocketHandler = mock();

	private Session session = mock();

	private StandardWebSocketSession webSocketSession = new StandardWebSocketSession(null, null, null, null);

	private StandardWebSocketHandlerAdapter adapter = new StandardWebSocketHandlerAdapter(this.webSocketHandler, this.webSocketSession);


	@Test
	void onOpen() throws Throwable {
		URI uri = URI.create("https://example.org");
		given(this.session.getRequestURI()).willReturn(uri);
		this.adapter.onOpen(this.session, null);

		verify(this.webSocketHandler).afterConnectionEstablished(this.webSocketSession);
		verify(this.session, atLeast(2)).addMessageHandler(any(MessageHandler.Whole.class));

		given(this.session.getRequestURI()).willReturn(uri);
		assertThat(this.webSocketSession.getUri()).isEqualTo(uri);
	}

	@Test
	void onClose() throws Throwable {
		this.adapter.onClose(this.session, new CloseReason(CloseCodes.NORMAL_CLOSURE, "reason"));
		verify(this.webSocketHandler).afterConnectionClosed(this.webSocketSession, CloseStatus.NORMAL.withReason("reason"));
	}

	@Test
	void onError() throws Throwable {
		Exception exception = new Exception();
		this.adapter.onError(this.session, exception);
		verify(this.webSocketHandler).handleTransportError(this.webSocketSession, exception);
	}

}
