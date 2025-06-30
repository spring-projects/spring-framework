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

package org.springframework.web.socket.handler;

import org.junit.jupiter.api.Test;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

/**
 * Test fixture for {@link ExceptionWebSocketHandlerDecorator}.
 *
 * @author Rossen Stoyanchev
 */
class ExceptionWebSocketHandlerDecoratorTests {

	private TestWebSocketSession session = new TestWebSocketSession(true);

	private WebSocketHandler delegate = mock();

	private ExceptionWebSocketHandlerDecorator decorator = new ExceptionWebSocketHandlerDecorator(this.delegate);


	@Test
	void afterConnectionEstablished() throws Exception {
		willThrow(new IllegalStateException("error"))
			.given(this.delegate).afterConnectionEstablished(this.session);

		this.decorator.afterConnectionEstablished(this.session);

		assertThat(this.session.getCloseStatus()).isEqualTo(CloseStatus.SERVER_ERROR);
	}

	@Test
	void handleMessage() throws Exception {
		TextMessage message = new TextMessage("payload");

		willThrow(new IllegalStateException("error"))
			.given(this.delegate).handleMessage(this.session, message);

		this.decorator.handleMessage(this.session, message);

		assertThat(this.session.getCloseStatus()).isEqualTo(CloseStatus.SERVER_ERROR);
	}

	@Test
	void handleTransportError() throws Exception {
		Exception exception = new Exception("transport error");

		willThrow(new IllegalStateException("error"))
			.given(this.delegate).handleTransportError(this.session, exception);

		this.decorator.handleTransportError(this.session, exception);

		assertThat(this.session.getCloseStatus()).isEqualTo(CloseStatus.SERVER_ERROR);
	}

	@Test
	void afterConnectionClosed() throws Exception {
		CloseStatus closeStatus = CloseStatus.NORMAL;

		willThrow(new IllegalStateException("error"))
			.given(this.delegate).afterConnectionClosed(this.session, closeStatus);

		this.decorator.afterConnectionClosed(this.session, closeStatus);

		assertThat(this.session.getCloseStatus()).isNull();
	}

}
