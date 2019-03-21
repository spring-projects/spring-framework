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
import org.springframework.web.socket.server.support.OriginHandshakeInterceptor;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;
import org.springframework.web.socket.sockjs.support.SockJsHttpRequestHandler;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.socket.sockjs.transport.handler.DefaultSockJsService;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
		assertEquals(1, ((WebSocketHttpRequestHandler) entry.getKey()).getHandshakeInterceptors().size());
		assertEquals(Arrays.asList("/foo"), entry.getValue());
	}

	@Test
	public void allowedOrigins() {
		WebMvcStompWebSocketEndpointRegistration registration =
				new WebMvcStompWebSocketEndpointRegistration(new String[] {"/foo"}, this.handler, this.scheduler);

		registration.setAllowedOrigins();

		MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
		assertEquals(1, mappings.size());
		HttpRequestHandler handler = mappings.entrySet().iterator().next().getKey();
		WebSocketHttpRequestHandler wsHandler = (WebSocketHttpRequestHandler) handler;
		assertNotNull(wsHandler.getWebSocketHandler());
		assertEquals(1, wsHandler.getHandshakeInterceptors().size());
		assertEquals(OriginHandshakeInterceptor.class, wsHandler.getHandshakeInterceptors().get(0).getClass());
	}

	@Test
	public void sameOrigin() {
		WebMvcStompWebSocketEndpointRegistration registration = new WebMvcStompWebSocketEndpointRegistration(
				new String[] {"/foo"}, this.handler, this.scheduler);

		registration.setAllowedOrigins();

		MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
		assertEquals(1, mappings.size());
		HttpRequestHandler handler = mappings.entrySet().iterator().next().getKey();
		WebSocketHttpRequestHandler wsHandler = (WebSocketHttpRequestHandler) handler;
		assertNotNull(wsHandler.getWebSocketHandler());
		assertEquals(1, wsHandler.getHandshakeInterceptors().size());
		assertEquals(OriginHandshakeInterceptor.class, wsHandler.getHandshakeInterceptors().get(0).getClass());
	}

	@Test
	public void allowedOriginsWithSockJsService() {
		WebMvcStompWebSocketEndpointRegistration registration =
				new WebMvcStompWebSocketEndpointRegistration(new String[] {"/foo"}, this.handler, this.scheduler);

		String origin = "https://mydomain.com";
		registration.setAllowedOrigins(origin).withSockJS();

		MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
		assertEquals(1, mappings.size());
		SockJsHttpRequestHandler requestHandler = (SockJsHttpRequestHandler)mappings.entrySet().iterator().next().getKey();
		assertNotNull(requestHandler.getSockJsService());
		DefaultSockJsService sockJsService = (DefaultSockJsService)requestHandler.getSockJsService();
		assertTrue(sockJsService.getAllowedOrigins().contains(origin));
		assertFalse(sockJsService.shouldSuppressCors());

		registration =
				new WebMvcStompWebSocketEndpointRegistration(new String[] {"/foo"}, this.handler, this.scheduler);
		registration.withSockJS().setAllowedOrigins(origin);
		mappings = registration.getMappings();
		assertEquals(1, mappings.size());
		requestHandler = (SockJsHttpRequestHandler)mappings.entrySet().iterator().next().getKey();
		assertNotNull(requestHandler.getSockJsService());
		sockJsService = (DefaultSockJsService)requestHandler.getSockJsService();
		assertTrue(sockJsService.getAllowedOrigins().contains(origin));
		assertFalse(sockJsService.shouldSuppressCors());
	}

	@Test  // SPR-12283
	public void disableCorsWithSockJsService() {
		WebMvcStompWebSocketEndpointRegistration registration =
				new WebMvcStompWebSocketEndpointRegistration(new String[] {"/foo"}, this.handler, this.scheduler);

		registration.withSockJS().setSupressCors(true);

		MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
		assertEquals(1, mappings.size());
		SockJsHttpRequestHandler requestHandler = (SockJsHttpRequestHandler)mappings.entrySet().iterator().next().getKey();
		assertNotNull(requestHandler.getSockJsService());
		DefaultSockJsService sockJsService = (DefaultSockJsService)requestHandler.getSockJsService();
		assertTrue(sockJsService.shouldSuppressCors());
	}

	@Test
	public void handshakeHandlerAndInterceptor() {
		WebMvcStompWebSocketEndpointRegistration registration =
				new WebMvcStompWebSocketEndpointRegistration(new String[] {"/foo"}, this.handler, this.scheduler);

		DefaultHandshakeHandler handshakeHandler = new DefaultHandshakeHandler();
		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();

		registration.setHandshakeHandler(handshakeHandler).addInterceptors(interceptor);

		MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
		assertEquals(1, mappings.size());

		Map.Entry<HttpRequestHandler, List<String>> entry = mappings.entrySet().iterator().next();
		assertEquals(Arrays.asList("/foo"), entry.getValue());

		WebSocketHttpRequestHandler requestHandler = (WebSocketHttpRequestHandler) entry.getKey();
		assertNotNull(requestHandler.getWebSocketHandler());
		assertSame(handshakeHandler, requestHandler.getHandshakeHandler());
		assertEquals(2, requestHandler.getHandshakeInterceptors().size());
		assertEquals(interceptor, requestHandler.getHandshakeInterceptors().get(0));
		assertEquals(OriginHandshakeInterceptor.class, requestHandler.getHandshakeInterceptors().get(1).getClass());
	}

	@Test
	public void handshakeHandlerAndInterceptorWithAllowedOrigins() {
		WebMvcStompWebSocketEndpointRegistration registration =
				new WebMvcStompWebSocketEndpointRegistration(new String[] {"/foo"}, this.handler, this.scheduler);

		DefaultHandshakeHandler handshakeHandler = new DefaultHandshakeHandler();
		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();
		String origin = "https://mydomain.com";
		registration.setHandshakeHandler(handshakeHandler).addInterceptors(interceptor).setAllowedOrigins(origin);

		MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
		assertEquals(1, mappings.size());

		Map.Entry<HttpRequestHandler, List<String>> entry = mappings.entrySet().iterator().next();
		assertEquals(Arrays.asList("/foo"), entry.getValue());

		WebSocketHttpRequestHandler requestHandler = (WebSocketHttpRequestHandler) entry.getKey();
		assertNotNull(requestHandler.getWebSocketHandler());
		assertSame(handshakeHandler, requestHandler.getHandshakeHandler());
		assertEquals(2, requestHandler.getHandshakeInterceptors().size());
		assertEquals(interceptor, requestHandler.getHandshakeInterceptors().get(0));
		assertEquals(OriginHandshakeInterceptor.class, requestHandler.getHandshakeInterceptors().get(1).getClass());
	}

	@Test
	public void handshakeHandlerInterceptorWithSockJsService() {
		WebMvcStompWebSocketEndpointRegistration registration =
				new WebMvcStompWebSocketEndpointRegistration(new String[] {"/foo"}, this.handler, this.scheduler);

		DefaultHandshakeHandler handshakeHandler = new DefaultHandshakeHandler();
		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();

		registration.setHandshakeHandler(handshakeHandler).addInterceptors(interceptor).withSockJS();

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
		assertEquals(2, sockJsService.getHandshakeInterceptors().size());
		assertEquals(interceptor, sockJsService.getHandshakeInterceptors().get(0));
		assertEquals(OriginHandshakeInterceptor.class, sockJsService.getHandshakeInterceptors().get(1).getClass());
	}

	@Test
	public void handshakeHandlerInterceptorWithSockJsServiceAndAllowedOrigins() {
		WebMvcStompWebSocketEndpointRegistration registration =
				new WebMvcStompWebSocketEndpointRegistration(new String[] {"/foo"}, this.handler, this.scheduler);

		DefaultHandshakeHandler handshakeHandler = new DefaultHandshakeHandler();
		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();
		String origin = "https://mydomain.com";

		registration.setHandshakeHandler(handshakeHandler)
				.addInterceptors(interceptor).setAllowedOrigins(origin).withSockJS();

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
		assertEquals(2, sockJsService.getHandshakeInterceptors().size());
		assertEquals(interceptor, sockJsService.getHandshakeInterceptors().get(0));
		assertEquals(OriginHandshakeInterceptor.class,
				sockJsService.getHandshakeInterceptors().get(1).getClass());
		assertTrue(sockJsService.getAllowedOrigins().contains(origin));
	}

}
