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
 * WITHOUT WARRANTIES OR CONDITIOsNS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.handler.websocket;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.MessageChannel;
import org.springframework.web.socket.support.TestWebSocketSession;

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

	@Mock MessageChannel channel;


	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);

		this.webSocketHandler = new SubProtocolWebSocketHandler(this.channel);
		when(stompHandler.getSupportedProtocols()).thenReturn(Arrays.asList("STOMP"));
		when(mqttHandler.getSupportedProtocols()).thenReturn(Arrays.asList("MQTT"));

		this.session = new TestWebSocketSession();
		this.session.setId("1");
	}


	@Test
	public void subProtocolMatch() throws Exception {
		this.webSocketHandler.setProtocolHandlers(Arrays.asList(stompHandler, mqttHandler));
		this.session.setAcceptedProtocol("sToMp");
		this.webSocketHandler.afterConnectionEstablished(session);

		verify(this.stompHandler).afterSessionStarted(session, this.channel);
		verify(this.mqttHandler, times(0)).afterSessionStarted(session, this.channel);
	}

	@Test
	public void subProtocolDefaultHandlerOnly() throws Exception {
		this.webSocketHandler.setDefaultProtocolHandler(stompHandler);
		this.session.setAcceptedProtocol("sToMp");
		this.webSocketHandler.afterConnectionEstablished(session);

		verify(this.stompHandler).afterSessionStarted(session, this.channel);
	}

	@Test(expected=IllegalStateException.class)
	public void subProtocolNoMatch() throws Exception {
		this.webSocketHandler.setDefaultProtocolHandler(defaultHandler);
		this.webSocketHandler.setProtocolHandlers(Arrays.asList(stompHandler, mqttHandler));
		this.session.setAcceptedProtocol("wamp");

		this.webSocketHandler.afterConnectionEstablished(session);
	}

	@Test
	public void noSubProtocol() throws Exception {
		this.webSocketHandler.setDefaultProtocolHandler(defaultHandler);
		this.webSocketHandler.afterConnectionEstablished(session);

		verify(this.defaultHandler).afterSessionStarted(session, this.channel);
		verify(this.stompHandler, times(0)).afterSessionStarted(session, this.channel);
		verify(this.mqttHandler, times(0)).afterSessionStarted(session, this.channel);
	}

	@Test
	public void noSubProtocolOneHandler() throws Exception {
		this.webSocketHandler.setProtocolHandlers(Arrays.asList(stompHandler));
		this.webSocketHandler.afterConnectionEstablished(session);

		verify(this.stompHandler).afterSessionStarted(session, this.channel);
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

}
