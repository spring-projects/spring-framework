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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.config.annotation;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.messaging.SubscribableChannel;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;
import org.springframework.web.util.UrlPathHelper;

/**
 * Test fixture for
 * {@link org.springframework.web.socket.config.annotation.WebMvcStompEndpointRegistry}.
 *
 * @author Rossen Stoyanchev
 */
public class WebMvcStompEndpointRegistryTests {

	private WebMvcStompEndpointRegistry endpointRegistry;

	private SubProtocolWebSocketHandler webSocketHandler;


	@Before
	public void setup() {
		SubscribableChannel inChannel = mock(SubscribableChannel.class);
		SubscribableChannel outChannel = mock(SubscribableChannel.class);
		this.webSocketHandler = new SubProtocolWebSocketHandler(inChannel, outChannel);

		WebSocketTransportRegistration transport = new WebSocketTransportRegistration();
		TaskScheduler scheduler = mock(TaskScheduler.class);
		this.endpointRegistry = new WebMvcStompEndpointRegistry(this.webSocketHandler, transport, null, scheduler);
	}


	@Test
	public void stompProtocolHandler() {
		this.endpointRegistry.addEndpoint("/stomp");

		Map<String, SubProtocolHandler> protocolHandlers = webSocketHandler.getProtocolHandlerMap();
		assertEquals(3, protocolHandlers.size());
		assertNotNull(protocolHandlers.get("v10.stomp"));
		assertNotNull(protocolHandlers.get("v11.stomp"));
		assertNotNull(protocolHandlers.get("v12.stomp"));
	}

	@Test
	public void handlerMapping() {
		SimpleUrlHandlerMapping hm = (SimpleUrlHandlerMapping) this.endpointRegistry.getHandlerMapping();
		assertEquals(0, hm.getUrlMap().size());

		UrlPathHelper pathHelper = new UrlPathHelper();
		this.endpointRegistry.setUrlPathHelper(pathHelper);
		this.endpointRegistry.addEndpoint("/stompOverWebSocket");
		this.endpointRegistry.addEndpoint("/stompOverSockJS").withSockJS();

		//SPR-12403
		assertEquals(1, this.webSocketHandler.getProtocolHandlers().size());

		hm = (SimpleUrlHandlerMapping) this.endpointRegistry.getHandlerMapping();
		assertEquals(2, hm.getUrlMap().size());
		assertNotNull(hm.getUrlMap().get("/stompOverWebSocket"));
		assertNotNull(hm.getUrlMap().get("/stompOverSockJS/**"));
		assertSame(pathHelper, hm.getUrlPathHelper());
	}

	@Test
	public void errorHandler() throws Exception {
		StompSubProtocolErrorHandler errorHandler = mock(StompSubProtocolErrorHandler.class);
		this.endpointRegistry.setErrorHandler(errorHandler);
		this.endpointRegistry.addEndpoint("/stompOverWebSocket");

		Map<String, SubProtocolHandler> protocolHandlers = this.webSocketHandler.getProtocolHandlerMap();
		StompSubProtocolHandler stompHandler = (StompSubProtocolHandler) protocolHandlers.get("v12.stomp");
		assertSame(errorHandler, stompHandler.getErrorHandler());
	}

}
