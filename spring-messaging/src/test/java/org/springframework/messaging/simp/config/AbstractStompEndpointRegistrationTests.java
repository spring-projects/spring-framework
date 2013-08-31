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

package org.springframework.messaging.simp.config;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.messaging.handler.websocket.SubProtocolWebSocketHandler;
import org.springframework.messaging.support.channel.ExecutorSubscribableChannel;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.server.DefaultHandshakeHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.socket.sockjs.transport.handler.DefaultSockJsService;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;

import static org.junit.Assert.*;


/**
 * Test fixture for {@link AbstractStompEndpointRegistration}.
 *
 * @author Rossen Stoyanchev
 */
public class AbstractStompEndpointRegistrationTests {

	private SubProtocolWebSocketHandler wsHandler;

	private TaskScheduler scheduler;


	@Before
	public void setup() {
		this.wsHandler = new SubProtocolWebSocketHandler(new ExecutorSubscribableChannel());
		this.scheduler = Mockito.mock(TaskScheduler.class);
	}

	@Test
	public void minimal() {

		TestStompEndpointRegistration registration =
				new TestStompEndpointRegistration(new String[] {"/foo"}, this.wsHandler, this.scheduler);

		List<Mapping> mappings = registration.getMappings();
		assertEquals(1, mappings.size());

		Mapping m1 = mappings.get(0);
		assertSame(this.wsHandler, m1.webSocketHandler);
		assertEquals("/foo", m1.path);
	}

	@Test
	public void handshakeHandler() {

		DefaultHandshakeHandler handshakeHandler = new DefaultHandshakeHandler();

		TestStompEndpointRegistration registration =
				new TestStompEndpointRegistration(new String[] {"/foo"}, this.wsHandler, this.scheduler);
		registration.setHandshakeHandler(handshakeHandler);

		List<Mapping> mappings = registration.getMappings();
		assertEquals(1, mappings.size());

		Mapping m1 = mappings.get(0);
		assertSame(this.wsHandler, m1.webSocketHandler);
		assertEquals("/foo", m1.path);
		assertSame(handshakeHandler, m1.handshakeHandler);
	}

	@Test
	public void handshakeHandlerPassedToSockJsService() {

		DefaultHandshakeHandler handshakeHandler = new DefaultHandshakeHandler();

		TestStompEndpointRegistration registration =
				new TestStompEndpointRegistration(new String[] {"/foo"}, this.wsHandler, this.scheduler);
		registration.setHandshakeHandler(handshakeHandler);
		registration.withSockJS();

		List<Mapping> mappings = registration.getMappings();
		assertEquals(1, mappings.size());

		Mapping m1 = mappings.get(0);
		assertSame(this.wsHandler, m1.webSocketHandler);
		assertEquals("/foo/**", m1.path);
		assertNotNull(m1.sockJsService);

		WebSocketTransportHandler transportHandler =
				(WebSocketTransportHandler) m1.sockJsService.getTransportHandlers().get(TransportType.WEBSOCKET);
		assertSame(handshakeHandler, transportHandler.getHandshakeHandler());
	}


	private static class TestStompEndpointRegistration extends AbstractStompEndpointRegistration<List<Mapping>> {

		public TestStompEndpointRegistration(String[] paths, SubProtocolWebSocketHandler wsh, TaskScheduler scheduler) {
			super(paths, wsh, scheduler);
		}

		@Override
		protected List<Mapping> createMappings() {
			return new ArrayList<>();
		}

		@Override
		protected void addSockJsServiceMapping(List<Mapping> mappings, SockJsService sockJsService,
				SubProtocolWebSocketHandler wsHandler, String pathPattern) {

			mappings.add(new Mapping(wsHandler, pathPattern, sockJsService));
		}

		@Override
		protected void addWebSocketHandlerMapping(List<Mapping> mappings, SubProtocolWebSocketHandler wsHandler,
				HandshakeHandler handshakeHandler, String path) {

			mappings.add(new Mapping(wsHandler, path, handshakeHandler));
		}
	}

	private static class Mapping {

		private final SubProtocolWebSocketHandler webSocketHandler;

		private final String path;

		private final HandshakeHandler handshakeHandler;

		private final DefaultSockJsService sockJsService;

		public Mapping(SubProtocolWebSocketHandler handler, String path, SockJsService sockJsService) {
			this.webSocketHandler = handler;
			this.path = path;
			this.handshakeHandler = null;
			this.sockJsService = (DefaultSockJsService) sockJsService;
		}

		public Mapping(SubProtocolWebSocketHandler h, String path, HandshakeHandler hh) {
			this.webSocketHandler = h;
			this.path = path;
			this.handshakeHandler = hh;
			this.sockJsService = null;
		}
	}

}
