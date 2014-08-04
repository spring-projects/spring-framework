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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.config.annotation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;
import org.springframework.web.socket.sockjs.support.SockJsHttpRequestHandler;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.socket.sockjs.transport.handler.DefaultSockJsService;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Test fixture for
 * {@link org.springframework.web.socket.config.annotation.WebMvcStompWebSocketEndpointRegistration}.
 *
 * @author Rossen Stoyanchev
 */
public class WebMvcStompWebSocketEndpointRegistrationTests {

	private SubProtocolWebSocketHandler handler;

	private TaskScheduler scheduler;


	@Before
	public void setup() {
		this.handler = new SubProtocolWebSocketHandler(mock(MessageChannel.class), mock(SubscribableChannel.class));
		this.scheduler = mock(TaskScheduler.class);
	}

	@Test
	public void minimalRegistration() {
		WebMvcStompWebSocketEndpointRegistration registration =
				new WebMvcStompWebSocketEndpointRegistration(new String[] {"/foo"}, this.handler, this.scheduler);

		MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
		assertEquals(1, mappings.size());

		Map.Entry<HttpRequestHandler, List<String>> entry = mappings.entrySet().iterator().next();
		assertNotNull(((WebSocketHttpRequestHandler) entry.getKey()).getWebSocketHandler());
		assertEquals(Arrays.asList("/foo"), entry.getValue());
	}

	@Test
	public void handshakeHandlerAndInterceptors() {
		WebMvcStompWebSocketEndpointRegistration registration =
				new WebMvcStompWebSocketEndpointRegistration(new String[] {"/foo"}, this.handler, this.scheduler);

		DefaultHandshakeHandler handshakeHandler = new DefaultHandshakeHandler();
		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();

		registration.setHandshakeHandler(handshakeHandler);
		registration.addInterceptors(interceptor);

		MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
		assertEquals(1, mappings.size());

		Map.Entry<HttpRequestHandler, List<String>> entry = mappings.entrySet().iterator().next();
		assertEquals(Arrays.asList("/foo"), entry.getValue());

		WebSocketHttpRequestHandler requestHandler = (WebSocketHttpRequestHandler) entry.getKey();
		assertNotNull(requestHandler.getWebSocketHandler());
		assertSame(handshakeHandler, requestHandler.getHandshakeHandler());
		assertEquals(Arrays.asList(interceptor), requestHandler.getHandshakeInterceptors());
	}

	@Test
	public void handshakeHandlerAndInterceptorsWithSockJsService() {
		WebMvcStompWebSocketEndpointRegistration registration =
				new WebMvcStompWebSocketEndpointRegistration(new String[] {"/foo"}, this.handler, this.scheduler);

		DefaultHandshakeHandler handshakeHandler = new DefaultHandshakeHandler();
		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();

		registration.setHandshakeHandler(handshakeHandler);
		registration.addInterceptors(interceptor);
		registration.withSockJS();

		MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
		assertEquals(1, mappings.size());

		Map.Entry<HttpRequestHandler, List<String>> entry = mappings.entrySet().iterator().next();
		assertEquals(Arrays.asList("/foo/**"), entry.getValue());

		SockJsHttpRequestHandler requestHandler = (SockJsHttpRequestHandler) entry.getKey();
		assertNotNull(requestHandler.getWebSocketHandler());

		DefaultSockJsService sockJsService = (DefaultSockJsService) requestHandler.getSockJsService();
		assertNotNull(sockJsService);

		Map<TransportType, TransportHandler> handlers = sockJsService.getTransportHandlers();
		WebSocketTransportHandler transportHandler = (WebSocketTransportHandler) handlers.get(TransportType.WEBSOCKET);
		assertSame(handshakeHandler, transportHandler.getHandshakeHandler());
		assertEquals(Arrays.asList(interceptor), sockJsService.getHandshakeInterceptors());
	}

}
