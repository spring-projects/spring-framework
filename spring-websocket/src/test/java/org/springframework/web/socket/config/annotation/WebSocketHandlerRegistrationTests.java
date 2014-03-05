/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.socket.sockjs.transport.handler.DefaultSockJsService;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link org.springframework.web.socket.config.annotation.AbstractWebSocketHandlerRegistration}.
 *
 * @author Rossen Stoyanchev
 */
public class WebSocketHandlerRegistrationTests {

	private TestWebSocketHandlerRegistration registration;

	private TaskScheduler taskScheduler;


	@Before
	public void setup() {
		this.taskScheduler = Mockito.mock(TaskScheduler.class);
		this.registration = new TestWebSocketHandlerRegistration(taskScheduler);
	}

	@Test
	public void minimal() {

		WebSocketHandler wsHandler = new TextWebSocketHandler();
		this.registration.addHandler(wsHandler, "/foo", "/bar");

		List<Mapping> mappings = this.registration.getMappings();
		assertEquals(2, mappings.size());

		Mapping m1 = mappings.get(0);
		assertEquals(wsHandler, m1.webSocketHandler);
		assertEquals("/foo", m1.path);

		Mapping m2 = mappings.get(1);
		assertEquals(wsHandler, m2.webSocketHandler);
		assertEquals("/bar", m2.path);
	}

	@Test
	public void interceptors() {

		WebSocketHandler wsHandler = new TextWebSocketHandler();
		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();

		this.registration.addHandler(wsHandler, "/foo").addInterceptors(interceptor);

		List<Mapping> mappings = this.registration.getMappings();
		assertEquals(1, mappings.size());

		Mapping m1 = mappings.get(0);
		assertEquals(wsHandler, m1.webSocketHandler);
		assertEquals("/foo", m1.path);
		assertArrayEquals(new HandshakeInterceptor[] { interceptor }, m1.interceptors);
	}

	@Test
	public void interceptorsPassedToSockJsRegistration() {

		WebSocketHandler wsHandler = new TextWebSocketHandler();
		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();

		this.registration.addHandler(wsHandler, "/foo").addInterceptors(interceptor).withSockJS();

		List<Mapping> mappings = this.registration.getMappings();
		assertEquals(1, mappings.size());

		Mapping m1 = mappings.get(0);
		assertEquals(wsHandler, m1.webSocketHandler);
		assertEquals("/foo/**", m1.path);
		assertNotNull(m1.sockJsService);
		assertEquals(Arrays.asList(interceptor), m1.sockJsService.getHandshakeInterceptors());
	}

	@Test
	public void handshakeHandler() {

		WebSocketHandler wsHandler = new TextWebSocketHandler();
		HandshakeHandler handshakeHandler = new DefaultHandshakeHandler();

		this.registration.addHandler(wsHandler, "/foo").setHandshakeHandler(handshakeHandler);

		List<Mapping> mappings = this.registration.getMappings();
		assertEquals(1, mappings.size());

		Mapping m1 = mappings.get(0);
		assertEquals(wsHandler, m1.webSocketHandler);
		assertEquals("/foo", m1.path);
		assertSame(handshakeHandler, m1.handshakeHandler);
	}

	@Test
	public void handshakeHandlerPassedToSockJsRegistration() {

		WebSocketHandler wsHandler = new TextWebSocketHandler();
		HandshakeHandler handshakeHandler = new DefaultHandshakeHandler();

		this.registration.addHandler(wsHandler, "/foo").setHandshakeHandler(handshakeHandler).withSockJS();

		List<Mapping> mappings = this.registration.getMappings();
		assertEquals(1, mappings.size());

		Mapping m1 = mappings.get(0);
		assertEquals(wsHandler, m1.webSocketHandler);
		assertEquals("/foo/**", m1.path);
		assertNotNull(m1.sockJsService);

		WebSocketTransportHandler transportHandler =
				(WebSocketTransportHandler) m1.sockJsService.getTransportHandlers().get(TransportType.WEBSOCKET);
		assertSame(handshakeHandler, transportHandler.getHandshakeHandler());
	}


	private static class TestWebSocketHandlerRegistration  extends AbstractWebSocketHandlerRegistration<List<Mapping>> {


		public TestWebSocketHandlerRegistration(TaskScheduler sockJsTaskScheduler) {
			super(sockJsTaskScheduler);
		}

		@Override
		protected List<Mapping> createMappings() {
			return new ArrayList<>();
		}

		@Override
		protected void addSockJsServiceMapping(List<Mapping> mappings, SockJsService sockJsService,
				WebSocketHandler wsHandler, String pathPattern) {

			mappings.add(new Mapping(wsHandler, pathPattern, sockJsService));
		}

		@Override
		protected void addWebSocketHandlerMapping(List<Mapping> mappings,
				WebSocketHandler wsHandler, HandshakeHandler handshakeHandler,
				HandshakeInterceptor[] interceptors, String path) {

			mappings.add(new Mapping(wsHandler, path, handshakeHandler, interceptors));
		}
	}


	private static class Mapping {

		private final WebSocketHandler webSocketHandler;

		private final String path;

		private final HandshakeHandler handshakeHandler;

		private final HandshakeInterceptor[] interceptors;

		private final DefaultSockJsService sockJsService;


		public Mapping(WebSocketHandler handler, String path, SockJsService sockJsService) {
			this.webSocketHandler = handler;
			this.path = path;
			this.handshakeHandler = null;
			this.interceptors = null;
			this.sockJsService = (DefaultSockJsService) sockJsService;
		}

		public Mapping(WebSocketHandler h, String path, HandshakeHandler hh, HandshakeInterceptor[] interceptors) {
			this.webSocketHandler = h;
			this.path = path;
			this.handshakeHandler = hh;
			this.interceptors = interceptors;
			this.sockJsService = null;
		}
	}

}
