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

package org.springframework.web.socket.sockjs.transport.handler;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.SubscribableChannel;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;
import org.springframework.web.socket.sockjs.transport.session.WebSocketServerSockJsSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SockJsWebSocketHandler}.
 *
 * @author Rossen Stoyanchev
 */
class SockJsWebSocketHandlerTests {

	@Test
	void getSubProtocols() {
		SubscribableChannel channel = mock();
		SubProtocolWebSocketHandler handler = new SubProtocolWebSocketHandler(channel, channel);
		StompSubProtocolHandler stompHandler = new StompSubProtocolHandler();
		handler.addProtocolHandler(stompHandler);

		TaskScheduler scheduler = mock();
		DefaultSockJsService service = new DefaultSockJsService(scheduler);
		WebSocketServerSockJsSession session = new WebSocketServerSockJsSession("1", service, handler, null);
		SockJsWebSocketHandler sockJsHandler = new SockJsWebSocketHandler(service, handler, session);

		assertThat(sockJsHandler.getSubProtocols()).isEqualTo(stompHandler.getSupportedProtocols());
	}

	@Test
	void getSubProtocolsNone() {
		WebSocketHandler handler = new TextWebSocketHandler();
		TaskScheduler scheduler = mock();
		DefaultSockJsService service = new DefaultSockJsService(scheduler);
		WebSocketServerSockJsSession session = new WebSocketServerSockJsSession("1", service, handler, null);
		SockJsWebSocketHandler sockJsHandler = new SockJsWebSocketHandler(service, handler, session);

		assertThat(sockJsHandler.getSubProtocols()).isEqualTo(Collections.emptyList());
	}

}
