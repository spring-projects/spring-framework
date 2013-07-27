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

package org.springframework.web.socket.support;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.socket.WebSocketHandler;

import static org.mockito.Mockito.*;


/**
 * Test fixture for {@link MultiProtocolWebSocketHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class MultiProtocolWebSocketHandlerTests {

	private MultiProtocolWebSocketHandler multiProtocolHandler;

	@Mock
	WebSocketHandler stompHandler;

	@Mock
	WebSocketHandler mqttHandler;

	@Mock
	WebSocketHandler defaultHandler;


	@Before
	public void setup() {

		MockitoAnnotations.initMocks(this);

		Map<String, WebSocketHandler> handlers = new HashMap<String, WebSocketHandler>();
		handlers.put("STOMP", this.stompHandler);
		handlers.put("MQTT", this.mqttHandler);

		this.multiProtocolHandler = new MultiProtocolWebSocketHandler();
		this.multiProtocolHandler.setProtocolHandlers(handlers);
		this.multiProtocolHandler.setDefaultProtocolHandler(this.defaultHandler);
	}


	@Test
	public void subProtocol() throws Exception {

		TestWebSocketSession session = new TestWebSocketSession();
		session.setAcceptedProtocol("sToMp");

		this.multiProtocolHandler.afterConnectionEstablished(session);

		verify(this.stompHandler).afterConnectionEstablished(session);
		verifyZeroInteractions(this.mqttHandler);
	}

	@Test(expected=IllegalStateException.class)
	public void subProtocolNoMatch() throws Exception {

		TestWebSocketSession session = new TestWebSocketSession();
		session.setAcceptedProtocol("wamp");

		this.multiProtocolHandler.afterConnectionEstablished(session);
	}

	@Test
	public void noSubProtocol() throws Exception {

		TestWebSocketSession session = new TestWebSocketSession();

		this.multiProtocolHandler.afterConnectionEstablished(session);

		verify(this.defaultHandler).afterConnectionEstablished(session);
		verifyZeroInteractions(this.stompHandler, this.mqttHandler);
	}

	@Test(expected=IllegalStateException.class)
	public void noSubProtocolNoDefaultHandler() throws Exception {

		TestWebSocketSession session = new TestWebSocketSession();

		this.multiProtocolHandler.setDefaultProtocolHandler(null);
		this.multiProtocolHandler.afterConnectionEstablished(session);
	}

}
