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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.messaging.support.channel.ExecutorSubscribableChannel;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;
import org.springframework.web.socket.server.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;
import org.springframework.web.socket.sockjs.SockJsHttpRequestHandler;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.socket.sockjs.transport.handler.DefaultSockJsService;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link WebMvcStompWebSocketEndpointRegistration}.
 *
 * @author Rossen Stoyanchev
 */
public class WebMvcStompEndpointRegistrationTests {

	private SubProtocolWebSocketHandler wsHandler;

	private TaskScheduler scheduler;


	@Before
	public void setup() {
		this.wsHandler = new SubProtocolWebSocketHandler(
				new ExecutorSubscribableChannel(), new ExecutorSubscribableChannel());
		this.scheduler = Mockito.mock(TaskScheduler.class);
	}

	@Test
	public void minimalRegistration() {


		WebMvcStompWebSocketEndpointRegistration registration = new WebMvcStompWebSocketEndpointRegistration(
				new String[] {"/foo"}, this.wsHandler, this.scheduler);

		MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
		assertEquals(1, mappings.size());

		Map.Entry<HttpRequestHandler, List<String>> entry = mappings.entrySet().iterator().next();
		assertNotNull(((WebSocketHttpRequestHandler) entry.getKey()).getWebSocketHandler());
		assertEquals(Arrays.asList("/foo"), entry.getValue());
	}

	@Test
	public void customHandshakeHandler() {

		DefaultHandshakeHandler handshakeHandler = new DefaultHandshakeHandler();

		WebMvcStompWebSocketEndpointRegistration registration = new WebMvcStompWebSocketEndpointRegistration(
				new String[] {"/foo"}, this.wsHandler, this.scheduler);

		registration.setHandshakeHandler(handshakeHandler);

		MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
		assertEquals(1, mappings.size());

		Map.Entry<HttpRequestHandler, List<String>> entry = mappings.entrySet().iterator().next();
		assertEquals(Arrays.asList("/foo"), entry.getValue());

		WebSocketHttpRequestHandler requestHandler = (WebSocketHttpRequestHandler) entry.getKey();
		assertNotNull(requestHandler.getWebSocketHandler());
		assertSame(handshakeHandler, requestHandler.getHandshakeHandler());
	}

	@Test
	public void customHandshakeHandlerPassedToSockJsService() {

		DefaultHandshakeHandler handshakeHandler = new DefaultHandshakeHandler();

		WebMvcStompWebSocketEndpointRegistration registration = new WebMvcStompWebSocketEndpointRegistration(
				new String[] {"/foo"}, this.wsHandler, this.scheduler);

		registration.setHandshakeHandler(handshakeHandler);
		registration.withSockJS();

		MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
		assertEquals(1, mappings.size());

		Map.Entry<HttpRequestHandler, List<String>> entry = mappings.entrySet().iterator().next();
		assertEquals(Arrays.asList("/foo/**"), entry.getValue());

		SockJsHttpRequestHandler requestHandler = (SockJsHttpRequestHandler) entry.getKey();
		assertNotNull(requestHandler.getWebSocketHandler());

		DefaultSockJsService sockJsService = (DefaultSockJsService) requestHandler.getSockJsService();
		assertNotNull(sockJsService);

		WebSocketTransportHandler transportHandler =
				(WebSocketTransportHandler) sockJsService.getTransportHandlers().get(TransportType.WEBSOCKET);
		assertSame(handshakeHandler, transportHandler.getHandshakeHandler());
	}

}
