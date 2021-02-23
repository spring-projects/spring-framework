/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import org.springframework.web.socket.server.support.OriginHandshakeInterceptor;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.socket.sockjs.transport.handler.DefaultSockJsService;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test fixture for {@link AbstractWebSocketHandlerRegistration}.
 *
 * @author Rossen Stoyanchev
 */
public class WebSocketHandlerRegistrationTests {

	private TestWebSocketHandlerRegistration registration = new TestWebSocketHandlerRegistration();

	private TaskScheduler taskScheduler = mock(TaskScheduler.class);


	@Test
	public void minimal() {
		WebSocketHandler handler = new TextWebSocketHandler();
		this.registration.addHandler(handler, "/foo", "/bar");

		List<Mapping> mappings = this.registration.getMappings();
		assertThat(mappings.size()).isEqualTo(2);

		Mapping m1 = mappings.get(0);
		assertThat(m1.webSocketHandler).isEqualTo(handler);
		assertThat(m1.path).isEqualTo("/foo");
		assertThat(m1.interceptors).isNotNull();
		assertThat(m1.interceptors.length).isEqualTo(1);
		assertThat(m1.interceptors[0].getClass()).isEqualTo(OriginHandshakeInterceptor.class);

		Mapping m2 = mappings.get(1);
		assertThat(m2.webSocketHandler).isEqualTo(handler);
		assertThat(m2.path).isEqualTo("/bar");
		assertThat(m2.interceptors).isNotNull();
		assertThat(m2.interceptors.length).isEqualTo(1);
		assertThat(m2.interceptors[0].getClass()).isEqualTo(OriginHandshakeInterceptor.class);
	}

	@Test
	public void interceptors() {
		WebSocketHandler handler = new TextWebSocketHandler();
		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();

		this.registration.addHandler(handler, "/foo").addInterceptors(interceptor);

		List<Mapping> mappings = this.registration.getMappings();
		assertThat(mappings.size()).isEqualTo(1);

		Mapping mapping = mappings.get(0);
		assertThat(mapping.webSocketHandler).isEqualTo(handler);
		assertThat(mapping.path).isEqualTo("/foo");
		assertThat(mapping.interceptors).isNotNull();
		assertThat(mapping.interceptors.length).isEqualTo(2);
		assertThat(mapping.interceptors[0]).isEqualTo(interceptor);
		assertThat(mapping.interceptors[1].getClass()).isEqualTo(OriginHandshakeInterceptor.class);
	}

	@Test
	public void emptyAllowedOrigin() {
		WebSocketHandler handler = new TextWebSocketHandler();
		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();

		this.registration.addHandler(handler, "/foo").addInterceptors(interceptor).setAllowedOrigins();

		List<Mapping> mappings = this.registration.getMappings();
		assertThat(mappings.size()).isEqualTo(1);

		Mapping mapping = mappings.get(0);
		assertThat(mapping.webSocketHandler).isEqualTo(handler);
		assertThat(mapping.path).isEqualTo("/foo");
		assertThat(mapping.interceptors).isNotNull();
		assertThat(mapping.interceptors.length).isEqualTo(2);
		assertThat(mapping.interceptors[0]).isEqualTo(interceptor);
		assertThat(mapping.interceptors[1].getClass()).isEqualTo(OriginHandshakeInterceptor.class);
	}

	@Test
	public void interceptorsWithAllowedOrigins() {
		WebSocketHandler handler = new TextWebSocketHandler();
		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();

		this.registration.addHandler(handler, "/foo")
				.addInterceptors(interceptor)
				.setAllowedOrigins("https://mydomain1.example")
				.setAllowedOriginPatterns("https://*.abc.com");

		List<Mapping> mappings = this.registration.getMappings();
		assertThat(mappings.size()).isEqualTo(1);

		Mapping mapping = mappings.get(0);
		assertThat(mapping.webSocketHandler).isEqualTo(handler);
		assertThat(mapping.path).isEqualTo("/foo");
		assertThat(mapping.interceptors).isNotNull();
		assertThat(mapping.interceptors.length).isEqualTo(2);
		assertThat(mapping.interceptors[0]).isEqualTo(interceptor);

		OriginHandshakeInterceptor originInterceptor = (OriginHandshakeInterceptor) mapping.interceptors[1];
		assertThat(originInterceptor.getAllowedOrigins()).containsExactly("https://mydomain1.example");
		assertThat(originInterceptor.getAllowedOriginPatterns()).containsExactly("https://*.abc.com");
	}

	@Test
	public void interceptorsPassedToSockJsRegistration() {
		WebSocketHandler handler = new TextWebSocketHandler();
		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();

		this.registration.addHandler(handler, "/foo")
				.addInterceptors(interceptor)
				.setAllowedOrigins("https://mydomain1.example")
				.setAllowedOriginPatterns("https://*.abc.com")
				.withSockJS();

		this.registration.getSockJsServiceRegistration().setTaskScheduler(this.taskScheduler);

		List<Mapping> mappings = this.registration.getMappings();
		assertThat(mappings.size()).isEqualTo(1);

		Mapping mapping = mappings.get(0);
		assertThat(mapping.webSocketHandler).isEqualTo(handler);
		assertThat(mapping.path).isEqualTo("/foo/**");
		assertThat(mapping.sockJsService).isNotNull();
		assertThat(mapping.sockJsService.getAllowedOrigins().contains("https://mydomain1.example")).isTrue();
		List<HandshakeInterceptor> interceptors = mapping.sockJsService.getHandshakeInterceptors();
		assertThat(interceptors.get(0)).isEqualTo(interceptor);

		OriginHandshakeInterceptor originInterceptor = (OriginHandshakeInterceptor) interceptors.get(1);
		assertThat(originInterceptor.getAllowedOrigins()).containsExactly("https://mydomain1.example");
		assertThat(originInterceptor.getAllowedOriginPatterns()).containsExactly("https://*.abc.com");
	}

	@Test
	public void handshakeHandler() {
		WebSocketHandler handler = new TextWebSocketHandler();
		HandshakeHandler handshakeHandler = new DefaultHandshakeHandler();

		this.registration.addHandler(handler, "/foo").setHandshakeHandler(handshakeHandler);

		List<Mapping> mappings = this.registration.getMappings();
		assertThat(mappings.size()).isEqualTo(1);

		Mapping mapping = mappings.get(0);
		assertThat(mapping.webSocketHandler).isEqualTo(handler);
		assertThat(mapping.path).isEqualTo("/foo");
		assertThat(mapping.handshakeHandler).isSameAs(handshakeHandler);
	}

	@Test
	public void handshakeHandlerPassedToSockJsRegistration() {
		WebSocketHandler handler = new TextWebSocketHandler();
		HandshakeHandler handshakeHandler = new DefaultHandshakeHandler();

		this.registration.addHandler(handler, "/foo").setHandshakeHandler(handshakeHandler).withSockJS();
		this.registration.getSockJsServiceRegistration().setTaskScheduler(this.taskScheduler);

		List<Mapping> mappings = this.registration.getMappings();
		assertThat(mappings.size()).isEqualTo(1);

		Mapping mapping = mappings.get(0);
		assertThat(mapping.webSocketHandler).isEqualTo(handler);
		assertThat(mapping.path).isEqualTo("/foo/**");
		assertThat(mapping.sockJsService).isNotNull();

		WebSocketTransportHandler transportHandler =
				(WebSocketTransportHandler) mapping.sockJsService.getTransportHandlers().get(TransportType.WEBSOCKET);
		assertThat(transportHandler.getHandshakeHandler()).isSameAs(handshakeHandler);
	}


	private static class TestWebSocketHandlerRegistration extends AbstractWebSocketHandlerRegistration<List<Mapping>> {

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
		protected void addWebSocketHandlerMapping(List<Mapping> mappings, WebSocketHandler handler,
				HandshakeHandler handshakeHandler, HandshakeInterceptor[] interceptors, String path) {

			mappings.add(new Mapping(handler, path, handshakeHandler, interceptors));
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
