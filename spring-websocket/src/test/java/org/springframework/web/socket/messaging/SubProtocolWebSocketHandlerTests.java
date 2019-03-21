/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIOsNS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.messaging;

import java.util.Arrays;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TestWebSocketSession;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

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


	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.webSocketHandler = new SubProtocolWebSocketHandler(this.inClientChannel, this.outClientChannel);
		given(stompHandler.getSupportedProtocols()).willReturn(Arrays.asList("v10.stomp", "v11.stomp", "v12.stomp"));
		given(mqttHandler.getSupportedProtocols()).willReturn(Arrays.asList("MQTT"));
		this.session = new TestWebSocketSession();
		this.session.setId("1");
		this.session.setOpen(true);
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
		this.webSocketHandler.setDefaultProtocolHandler(this.defaultHandler);
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

	@Test
	@SuppressWarnings("unchecked")
	public void checkSession() throws Exception {
		TestWebSocketSession session1 = new TestWebSocketSession("id1");
		TestWebSocketSession session2 = new TestWebSocketSession("id2");
		session1.setOpen(true);
		session2.setOpen(true);
		session1.setAcceptedProtocol("v12.stomp");
		session2.setAcceptedProtocol("v12.stomp");

		this.webSocketHandler.setProtocolHandlers(Arrays.asList(this.stompHandler));
		this.webSocketHandler.afterConnectionEstablished(session1);
		this.webSocketHandler.afterConnectionEstablished(session2);

		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(this.webSocketHandler);
		Map<String, ?> map = (Map<String, ?>) handlerAccessor.getPropertyValue("sessions");
		DirectFieldAccessor session1Accessor = new DirectFieldAccessor(map.get("id1"));
		DirectFieldAccessor session2Accessor = new DirectFieldAccessor(map.get("id2"));

		long sixtyOneSecondsAgo = System.currentTimeMillis() - 61 * 1000;
		handlerAccessor.setPropertyValue("lastSessionCheckTime", sixtyOneSecondsAgo);
		session1Accessor.setPropertyValue("createTime", sixtyOneSecondsAgo);
		session2Accessor.setPropertyValue("createTime", sixtyOneSecondsAgo);

		this.webSocketHandler.start();
		this.webSocketHandler.handleMessage(session1, new TextMessage("foo"));

		assertTrue(session1.isOpen());
		assertNull(session1.getCloseStatus());

		assertFalse(session2.isOpen());
		assertEquals(CloseStatus.SESSION_NOT_RELIABLE, session2.getCloseStatus());

		assertNotEquals("lastSessionCheckTime not updated", sixtyOneSecondsAgo,
				handlerAccessor.getPropertyValue("lastSessionCheckTime"));
	}

}
