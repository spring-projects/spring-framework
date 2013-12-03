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

package org.springframework.web.socket.handler;

import org.junit.Before;
import org.junit.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test fixture for {@link ExceptionWebSocketHandlerDecorator}.
 *
 * @author Rossen Stoyanchev
 */
public class ExceptionWebSocketHandlerDecoratorTests {

	private TestWebSocketSession session;

	private ExceptionWebSocketHandlerDecorator decorator;

	private WebSocketHandler delegate;


	@Before
	public void setup() {

		this.delegate = mock(WebSocketHandler.class);
		this.decorator = new ExceptionWebSocketHandlerDecorator(this.delegate);

		this.session = new TestWebSocketSession();
		this.session.setOpen(true);
	}

	@Test
	public void afterConnectionEstablished() throws Exception {

		doThrow(new IllegalStateException("error"))
			.when(this.delegate).afterConnectionEstablished(this.session);

		this.decorator.afterConnectionEstablished(this.session);

		assertEquals(CloseStatus.SERVER_ERROR, this.session.getCloseStatus());
	}

	@Test
	public void handleMessage() throws Exception {

		TextMessage message = new TextMessage("payload");

		doThrow(new IllegalStateException("error"))
			.when(this.delegate).handleMessage(this.session, message);

		this.decorator.handleMessage(this.session, message);

		assertEquals(CloseStatus.SERVER_ERROR, this.session.getCloseStatus());
	}

	@Test
	public void handleTransportError() throws Exception {

		Exception exception = new Exception("transport error");

		doThrow(new IllegalStateException("error"))
			.when(this.delegate).handleTransportError(this.session, exception);

		this.decorator.handleTransportError(this.session, exception);

		assertEquals(CloseStatus.SERVER_ERROR, this.session.getCloseStatus());
	}

	@Test
	public void afterConnectionClosed() throws Exception {

		CloseStatus closeStatus = CloseStatus.NORMAL;

		doThrow(new IllegalStateException("error"))
			.when(this.delegate).afterConnectionClosed(this.session, closeStatus);

		this.decorator.afterConnectionClosed(this.session, closeStatus);

		assertNull(this.session.getCloseStatus());
	}

}