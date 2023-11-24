/*
 * Copyright 2002-2023 the original author or authors.
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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebMvcStompWebSocketEndpointRegistration}.
 *
 * @author Rossen Stoyanchev
 */
class WebMvcStompWebSocketEndpointRegistrationTests {

	private final SubProtocolWebSocketHandler handler = new SubProtocolWebSocketHandler(mock(), mock());

	private final TaskScheduler scheduler = mock();


	@Test
	void minimalRegistration() {
		WebMvcStompWebSocketEndpointRegistration registration =
				new WebMvcStompWebSocketEndpointRegistration(new String[] {"/foo"}, this.handler, this.scheduler);

		MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
		assertThat(mappings).hasSize(1);

		Map.Entry<HttpRequestHandler, List<String>> entry = mappings.entrySet().iterator().next();
		assertThat(((WebSocketHttpRequestHandler) entry.getKey()).getWebSocketHandler()).isNotNull();
		assertThat(((WebSocketHttpRequestHandler) entry.getKey()).getHandshakeInterceptors()).hasSize(1);
		assertThat(entry.getValue()).containsExactly("/foo");
	}

	@Test
	void allowedOrigins() {
		WebMvcStompWebSocketEndpointRegistration registration =
				new WebMvcStompWebSocketEndpointRegistration(new String[] {"/foo"}, this.handler, this.scheduler);

		registration.setAllowedOrigins();

		MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
		assertThat(mappings).hasSize(1);
		HttpRequestHandler handler = mappings.entrySet().iterator().next().getKey();
		WebSocketHttpRequestHandler wsHandler = (WebSocketHttpRequestHandler) handler;
		assertThat(wsHandler.getWebSocketHandler()).isNotNull();
		assertThat(wsHandler.getHandshakeInterceptors()).hasSize(1);
		assertThat(wsHandler.getHandshakeInterceptors().get(0).getClass()).isEqualTo(OriginHandshakeInterceptor.class);
	}

	@Test
	void sameOrigin() {
		WebMvcStompWebSocketEndpointRegistration registration = new WebMvcStompWebSocketEndpointRegistration(
				new String[] {"/foo"}, this.handler, this.scheduler);

		registration.setAllowedOrigins();

		MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
		assertThat(mappings).hasSize(1);
		HttpRequestHandler handler = mappings.entrySet().iterator().next().getKey();
		WebSocketHttpRequestHandler wsHandler = (WebSocketHttpRequestHandler) handler;
		assertThat(wsHandler.getWebSocketHandler()).isNotNull();
		assertThat(wsHandler.getHandshakeInterceptors()).hasSize(1);
		assertThat(wsHandler.getHandshakeInterceptors().get(0).getClass()).isEqualTo(OriginHandshakeInterceptor.class);
	}

	@Test
	void allowedOriginsWithSockJsService() {
		WebMvcStompWebSocketEndpointRegistration registration =
				new WebMvcStompWebSocketEndpointRegistration(new String[] {"/foo"}, this.handler, this.scheduler);

		String origin = "https://mydomain.com";
		registration.setAllowedOrigins(origin).withSockJS();

		MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
		assertThat(mappings).hasSize(1);
		SockJsHttpRequestHandler requestHandler = (SockJsHttpRequestHandler)mappings.entrySet().iterator().next().getKey();
		assertThat(requestHandler.getSockJsService()).isNotNull();
		DefaultSockJsService sockJsService = (DefaultSockJsService)requestHandler.getSockJsService();
		assertThat(sockJsService.getAllowedOrigins().contains(origin)).isTrue();
		assertThat(sockJsService.shouldSuppressCors()).isFalse();

		registration =
				new WebMvcStompWebSocketEndpointRegistration(new String[] {"/foo"}, this.handler, this.scheduler);
		registration.withSockJS().setAllowedOrigins(origin);
		mappings = registration.getMappings();
		assertThat(mappings).hasSize(1);
		requestHandler = (SockJsHttpRequestHandler)mappings.entrySet().iterator().next().getKey();
		assertThat(requestHandler.getSockJsService()).isNotNull();
		sockJsService = (DefaultSockJsService)requestHandler.getSockJsService();
		assertThat(sockJsService.getAllowedOrigins().contains(origin)).isTrue();
		assertThat(sockJsService.shouldSuppressCors()).isFalse();
	}

	@Test
	void allowedOriginPatterns() {
		WebMvcStompWebSocketEndpointRegistration registration =
				new WebMvcStompWebSocketEndpointRegistration(new String[] {"/foo"}, this.handler, this.scheduler);

		String origin = "https://*.mydomain.com";
		registration.setAllowedOriginPatterns(origin).withSockJS();

		MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
		assertThat(mappings).hasSize(1);
		SockJsHttpRequestHandler requestHandler = (SockJsHttpRequestHandler)mappings.entrySet().iterator().next().getKey();
		assertThat(requestHandler.getSockJsService()).isNotNull();
		DefaultSockJsService sockJsService = (DefaultSockJsService)requestHandler.getSockJsService();
		assertThat(sockJsService.getAllowedOriginPatterns().contains(origin)).isTrue();

		registration =
				new WebMvcStompWebSocketEndpointRegistration(new String[] {"/foo"}, this.handler, this.scheduler);
		registration.withSockJS().setAllowedOriginPatterns(origin);
		mappings = registration.getMappings();
		assertThat(mappings).hasSize(1);
		requestHandler = (SockJsHttpRequestHandler)mappings.entrySet().iterator().next().getKey();
		assertThat(requestHandler.getSockJsService()).isNotNull();
		sockJsService = (DefaultSockJsService)requestHandler.getSockJsService();
		assertThat(sockJsService.getAllowedOriginPatterns().contains(origin)).isTrue();
	}

	@Test  // SPR-12283
	void disableCorsWithSockJsService() {
		WebMvcStompWebSocketEndpointRegistration registration =
				new WebMvcStompWebSocketEndpointRegistration(new String[] {"/foo"}, this.handler, this.scheduler);

		registration.withSockJS().setSuppressCors(true);

		MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
		assertThat(mappings).hasSize(1);
		SockJsHttpRequestHandler requestHandler = (SockJsHttpRequestHandler)mappings.entrySet().iterator().next().getKey();
		assertThat(requestHandler.getSockJsService()).isNotNull();
		DefaultSockJsService sockJsService = (DefaultSockJsService)requestHandler.getSockJsService();
		assertThat(sockJsService.shouldSuppressCors()).isTrue();
	}

	@Test
	void handshakeHandlerAndInterceptor() {
		WebMvcStompWebSocketEndpointRegistration registration =
				new WebMvcStompWebSocketEndpointRegistration(new String[] {"/foo"}, this.handler, this.scheduler);

		DefaultHandshakeHandler handshakeHandler = new DefaultHandshakeHandler();
		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();

		registration.setHandshakeHandler(handshakeHandler).addInterceptors(interceptor);

		MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
		assertThat(mappings).hasSize(1);

		Map.Entry<HttpRequestHandler, List<String>> entry = mappings.entrySet().iterator().next();
		assertThat(entry.getValue()).containsExactly("/foo");

		WebSocketHttpRequestHandler requestHandler = (WebSocketHttpRequestHandler) entry.getKey();
		assertThat(requestHandler.getWebSocketHandler()).isNotNull();
		assertThat(requestHandler.getHandshakeHandler()).isSameAs(handshakeHandler);
		assertThat(requestHandler.getHandshakeInterceptors()).hasSize(2);
		assertThat(requestHandler.getHandshakeInterceptors().get(0)).isEqualTo(interceptor);
		assertThat(requestHandler.getHandshakeInterceptors().get(1).getClass()).isEqualTo(OriginHandshakeInterceptor.class);
	}

	@Test
	void handshakeHandlerAndInterceptorWithAllowedOrigins() {
		WebMvcStompWebSocketEndpointRegistration registration =
				new WebMvcStompWebSocketEndpointRegistration(new String[] {"/foo"}, this.handler, this.scheduler);

		DefaultHandshakeHandler handshakeHandler = new DefaultHandshakeHandler();
		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();
		String origin = "https://mydomain.com";
		registration.setHandshakeHandler(handshakeHandler).addInterceptors(interceptor).setAllowedOrigins(origin);

		MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
		assertThat(mappings).hasSize(1);

		Map.Entry<HttpRequestHandler, List<String>> entry = mappings.entrySet().iterator().next();
		assertThat(entry.getValue()).containsExactly("/foo");

		WebSocketHttpRequestHandler requestHandler = (WebSocketHttpRequestHandler) entry.getKey();
		assertThat(requestHandler.getWebSocketHandler()).isNotNull();
		assertThat(requestHandler.getHandshakeHandler()).isSameAs(handshakeHandler);
		assertThat(requestHandler.getHandshakeInterceptors()).hasSize(2);
		assertThat(requestHandler.getHandshakeInterceptors().get(0)).isEqualTo(interceptor);
		assertThat(requestHandler.getHandshakeInterceptors().get(1).getClass()).isEqualTo(OriginHandshakeInterceptor.class);
	}

	@Test
	void handshakeHandlerInterceptorWithSockJsService() {
		WebMvcStompWebSocketEndpointRegistration registration =
				new WebMvcStompWebSocketEndpointRegistration(new String[] {"/foo"}, this.handler, this.scheduler);

		DefaultHandshakeHandler handshakeHandler = new DefaultHandshakeHandler();
		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();

		registration.setHandshakeHandler(handshakeHandler).addInterceptors(interceptor).withSockJS();

		MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
		assertThat(mappings).hasSize(1);

		Map.Entry<HttpRequestHandler, List<String>> entry = mappings.entrySet().iterator().next();
		assertThat(entry.getValue()).containsExactly("/foo/**");

		SockJsHttpRequestHandler requestHandler = (SockJsHttpRequestHandler) entry.getKey();
		assertThat(requestHandler.getWebSocketHandler()).isNotNull();

		DefaultSockJsService sockJsService = (DefaultSockJsService) requestHandler.getSockJsService();
		assertThat(sockJsService).isNotNull();

		Map<TransportType, TransportHandler> handlers = sockJsService.getTransportHandlers();
		WebSocketTransportHandler transportHandler = (WebSocketTransportHandler) handlers.get(TransportType.WEBSOCKET);
		assertThat(transportHandler.getHandshakeHandler()).isSameAs(handshakeHandler);
		assertThat(sockJsService.getHandshakeInterceptors()).hasSize(2);
		assertThat(sockJsService.getHandshakeInterceptors().get(0)).isEqualTo(interceptor);
		assertThat(sockJsService.getHandshakeInterceptors().get(1).getClass()).isEqualTo(OriginHandshakeInterceptor.class);
	}

	@Test
	void handshakeHandlerInterceptorWithSockJsServiceAndAllowedOrigins() {
		WebMvcStompWebSocketEndpointRegistration registration =
				new WebMvcStompWebSocketEndpointRegistration(new String[] {"/foo"}, this.handler, this.scheduler);

		DefaultHandshakeHandler handshakeHandler = new DefaultHandshakeHandler();
		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();
		String origin = "https://mydomain.com";

		registration.setHandshakeHandler(handshakeHandler)
				.addInterceptors(interceptor).setAllowedOrigins(origin).withSockJS();

		MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
		assertThat(mappings).hasSize(1);

		Map.Entry<HttpRequestHandler, List<String>> entry = mappings.entrySet().iterator().next();
		assertThat(entry.getValue()).containsExactly("/foo/**");

		SockJsHttpRequestHandler requestHandler = (SockJsHttpRequestHandler) entry.getKey();
		assertThat(requestHandler.getWebSocketHandler()).isNotNull();

		DefaultSockJsService sockJsService = (DefaultSockJsService) requestHandler.getSockJsService();
		assertThat(sockJsService).isNotNull();

		Map<TransportType, TransportHandler> handlers = sockJsService.getTransportHandlers();
		WebSocketTransportHandler transportHandler = (WebSocketTransportHandler) handlers.get(TransportType.WEBSOCKET);
		assertThat(transportHandler.getHandshakeHandler()).isSameAs(handshakeHandler);
		assertThat(sockJsService.getHandshakeInterceptors()).hasSize(2);
		assertThat(sockJsService.getHandshakeInterceptors().get(0)).isEqualTo(interceptor);
		assertThat(sockJsService.getHandshakeInterceptors().get(1).getClass()).isEqualTo(OriginHandshakeInterceptor.class);
		assertThat(sockJsService.getAllowedOrigins().contains(origin)).isTrue();
	}

}
