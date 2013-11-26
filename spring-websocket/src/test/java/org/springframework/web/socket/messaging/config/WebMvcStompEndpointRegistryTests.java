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

package org.springframework.web.socket.messaging.config;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.handler.DefaultUserSessionRegistry;
import org.springframework.messaging.simp.handler.UserSessionRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * Test fixture for {@link WebMvcStompEndpointRegistry}.
 *
 * @author Rossen Stoyanchev
 */
public class WebMvcStompEndpointRegistryTests {

	private WebMvcStompEndpointRegistry registry;

	private SubProtocolWebSocketHandler webSocketHandler;

	private UserSessionRegistry userSessionRegistry;


	@Before
	public void setup() {
		MessageChannel channel = Mockito.mock(MessageChannel.class);
		this.webSocketHandler = new SubProtocolWebSocketHandler(channel);
		this.userSessionRegistry = new DefaultUserSessionRegistry();
		TaskScheduler taskScheduler = Mockito.mock(TaskScheduler.class);
		this.registry = new WebMvcStompEndpointRegistry(webSocketHandler, userSessionRegistry, taskScheduler);
	}


	@Test
	public void stompProtocolHandler() {

		this.registry.addEndpoint("/stomp");

		Map<String, SubProtocolHandler> protocolHandlers = webSocketHandler.getProtocolHandlers();
		assertEquals(3, protocolHandlers.size());
		assertNotNull(protocolHandlers.get("v10.stomp"));
		assertNotNull(protocolHandlers.get("v11.stomp"));
		assertNotNull(protocolHandlers.get("v12.stomp"));

		StompSubProtocolHandler stompHandler = (StompSubProtocolHandler) protocolHandlers.get("v10.stomp");
		assertSame(this.userSessionRegistry, stompHandler.getUserSessionRegistry());
	}

	@Test
	public void handlerMapping() {

		SimpleUrlHandlerMapping hm = (SimpleUrlHandlerMapping) this.registry.getHandlerMapping();
		assertEquals(0, hm.getUrlMap().size());

		this.registry.addEndpoint("/stompOverWebSocket");
		this.registry.addEndpoint("/stompOverSockJS").withSockJS();

		hm = (SimpleUrlHandlerMapping) this.registry.getHandlerMapping();
		assertEquals(2, hm.getUrlMap().size());
		assertNotNull(hm.getUrlMap().get("/stompOverWebSocket"));
		assertNotNull(hm.getUrlMap().get("/stompOverSockJS/**"));
	}

}
