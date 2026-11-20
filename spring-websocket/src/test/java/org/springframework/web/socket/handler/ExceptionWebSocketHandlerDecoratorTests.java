/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
