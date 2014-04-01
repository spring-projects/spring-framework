/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIOsNS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.messaging;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.adapter.standard.StandardWebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TestPrincipal;
import org.springframework.web.socket.handler.TestWebSocketSession;

import javax.websocket.Session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * Test fixture for {@link SubProtocolWebSocketHandler}.
 *
 * @author Rossen Stoyanchev
 * @author Andy Wilkinson
 */
public class SubProtocolWebSocketHandlerTests {

	private SubProtocolWebSocketHandler webSocketHandler;

	private TestWebSocketSession session;

	@Mock SubProtocolHandler stompHandler;

	@Mock SubProtocolHandler mqttHandler;

	@Mock SubProtocolHandler defaultHandler;

	@Mock MessageChannel inClientChannel;

	@Mock
	SubscribableChannel outClientChannel;

	@Mock
	private Session nativeSession;


	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);

		this.webSocketHandler = new SubProtocolWebSocketHandler(this.inClientChannel, this.outClientChannel);
		when(stompHandler.getSupportedProtocols()).thenReturn(Arrays.asList("v10.stomp", "v11.stomp", "v12.stomp"));
		when(mqttHandler.getSupportedProtocols()).thenReturn(Arrays.asList("MQTT"));

		this.session = new TestWebSocketSession();
		this.session.setId("1");
	}


	@Test
	public void subProtocolMatch() throws Exception {
		this.webSocketHandler.setProtocolHandlers(Arrays.asList(stompHandler, mqttHandler));
		this.session.setAcceptedProtocol("v12.sToMp");
		this.webSocketHandler.afterConnectionEstablished(session);

		verify(this.stompHandler).afterSessionStarted(
				isA(ConcurrentWebSocketSessionDecorator.class), eq(this.inClientChannel));
		verify(this.mqttHandler, times(0)).afterSessionStarted(session, this.inClientChannel);
	}

	@Test
	public void subProtocolDefaultHandlerOnly() throws Exception {
		this.webSocketHandler.setDefaultProtocolHandler(stompHandler);
		this.session.setAcceptedProtocol("v12.sToMp");
		this.webSocketHandler.afterConnectionEstablished(session);

		verify(this.stompHandler).afterSessionStarted(
				isA(ConcurrentWebSocketSessionDecorator.class), eq(this.inClientChannel));
	}

	@Test(expected=IllegalStateException.class)
	public void subProtocolNoMatch() throws Exception {
		this.webSocketHandler.setDefaultProtocolHandler(defaultHandler);
		this.webSocketHandler.setProtocolHandlers(Arrays.asList(stompHandler, mqttHandler));
		this.session.setAcceptedProtocol("wamp");

		this.webSocketHandler.afterConnectionEstablished(session);
	}

	@Test
	public void nullSubProtocol() throws Exception {
		this.webSocketHandler.setDefaultProtocolHandler(defaultHandler);
		this.webSocketHandler.afterConnectionEstablished(session);

		verify(this.defaultHandler).afterSessionStarted(
				isA(ConcurrentWebSocketSessionDecorator.class), eq(this.inClientChannel));
		verify(this.stompHandler, times(0)).afterSessionStarted(session, this.inClientChannel);
		verify(this.mqttHandler, times(0)).afterSessionStarted(session, this.inClientChannel);
	}

	@Test
	public void emptySubProtocol() throws Exception {
		this.session.setAcceptedProtocol("");
		this.webSocketHandler.setDefaultProtocolHandler(defaultHandler);
		this.webSocketHandler.afterConnectionEstablished(session);

		verify(this.defaultHandler).afterSessionStarted(
				isA(ConcurrentWebSocketSessionDecorator.class), eq(this.inClientChannel));
		verify(this.stompHandler, times(0)).afterSessionStarted(session, this.inClientChannel);
		verify(this.mqttHandler, times(0)).afterSessionStarted(session, this.inClientChannel);
	}

	@Test
	public void noSubProtocolOneHandler() throws Exception {
		this.webSocketHandler.setProtocolHandlers(Arrays.asList(stompHandler));
		this.webSocketHandler.afterConnectionEstablished(session);

		verify(this.stompHandler).afterSessionStarted(
				isA(ConcurrentWebSocketSessionDecorator.class), eq(this.inClientChannel));
	}

	@Test(expected=IllegalStateException.class)
	public void noSubProtocolTwoHandlers() throws Exception {
		this.webSocketHandler.setProtocolHandlers(Arrays.asList(stompHandler, mqttHandler));
		this.webSocketHandler.afterConnectionEstablished(session);
	}

	@Test(expected=IllegalStateException.class)
	public void noSubProtocolNoDefaultHandler() throws Exception {
		this.webSocketHandler.setProtocolHandlers(Arrays.asList(stompHandler, mqttHandler));
		this.webSocketHandler.afterConnectionEstablished(session);
	}

	// SPR-11621

	@Test
	public void availableSessionFieldsafterConnectionClosed() throws Exception {
		this.webSocketHandler.setDefaultProtocolHandler(stompHandler);
		when(nativeSession.getId()).thenReturn("1");
		when(nativeSession.getUserPrincipal()).thenReturn(new TestPrincipal("test"));
		when(nativeSession.getNegotiatedSubprotocol()).thenReturn("v12.sToMp");
		StandardWebSocketSession standardWebsocketSession = new StandardWebSocketSession(null, null, null, null);
		standardWebsocketSession.initializeNativeSession(this.nativeSession);
		this.webSocketHandler.afterConnectionEstablished(standardWebsocketSession);
		standardWebsocketSession.close(CloseStatus.NORMAL);
		this.webSocketHandler.afterConnectionClosed(standardWebsocketSession, CloseStatus.NORMAL);
		assertEquals("v12.sToMp", standardWebsocketSession.getAcceptedProtocol());
		assertNotNull(standardWebsocketSession.getPrincipal());
	}

}
