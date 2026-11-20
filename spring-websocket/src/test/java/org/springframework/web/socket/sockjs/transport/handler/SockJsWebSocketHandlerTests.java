/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
