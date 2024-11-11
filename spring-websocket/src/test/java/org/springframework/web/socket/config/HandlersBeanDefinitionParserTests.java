/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.socket.config;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.OriginHandshakeInterceptor;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.support.SockJsHttpRequestHandler;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import org.springframework.web.socket.sockjs.transport.TransportHandlingSockJsService;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.socket.sockjs.transport.handler.DefaultSockJsService;
import org.springframework.web.socket.sockjs.transport.handler.EventSourceTransportHandler;
import org.springframework.web.socket.sockjs.transport.handler.HtmlFileTransportHandler;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;
import org.springframework.web.socket.sockjs.transport.handler.XhrPollingTransportHandler;
import org.springframework.web.socket.sockjs.transport.handler.XhrReceivingTransportHandler;
import org.springframework.web.socket.sockjs.transport.handler.XhrStreamingTransportHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for HandlersBeanDefinitionParser.
 * See test configuration files websocket-config-handlers-*.xml.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 */
class HandlersBeanDefinitionParserTests {

	private final GenericWebApplicationContext appContext = new GenericWebApplicationContext();


	@Test
	void webSocketHandlers() {
		loadBeanDefinitions("websocket-config-handlers.xml");

		Map<String, HandlerMapping> handlersMap = this.appContext.getBeansOfType(HandlerMapping.class);
		assertThat(handlersMap).isNotNull();
		assertThat(handlersMap).hasSize(2);

		for (HandlerMapping hm : handlersMap.values()) {
			boolean condition2 = hm instanceof SimpleUrlHandlerMapping;
			assertThat(condition2).isTrue();
			SimpleUrlHandlerMapping shm = (SimpleUrlHandlerMapping) hm;

			if (shm.getUrlMap().containsKey("/foo")) {
				assertThat(shm.getUrlMap()).containsOnlyKeys("/foo", "/bar");
				WebSocketHttpRequestHandler handler = (WebSocketHttpRequestHandler) shm.getUrlMap().get("/foo");
				assertThat(handler).isNotNull();
				unwrapAndCheckDecoratedHandlerType(handler.getWebSocketHandler(), FooWebSocketHandler.class);
				HandshakeHandler handshakeHandler = handler.getHandshakeHandler();
				assertThat(handshakeHandler).isNotNull();
				boolean condition1 = handshakeHandler instanceof DefaultHandshakeHandler;
				assertThat(condition1).isTrue();
				assertThat(handler.getHandshakeInterceptors()).isNotEmpty();
				boolean condition = handler.getHandshakeInterceptors().get(0) instanceof OriginHandshakeInterceptor;
				assertThat(condition).isTrue();
			}
			else {
				assertThat(shm.getUrlMap()).containsOnlyKeys("/test");
				WebSocketHttpRequestHandler handler = (WebSocketHttpRequestHandler) shm.getUrlMap().get("/test");
				assertThat(handler).isNotNull();
				unwrapAndCheckDecoratedHandlerType(handler.getWebSocketHandler(), TestWebSocketHandler.class);
				HandshakeHandler handshakeHandler = handler.getHandshakeHandler();
				assertThat(handshakeHandler).isNotNull();
				boolean condition1 = handshakeHandler instanceof DefaultHandshakeHandler;
				assertThat(condition1).isTrue();
				assertThat(handler.getHandshakeInterceptors()).isNotEmpty();
				boolean condition = handler.getHandshakeInterceptors().get(0) instanceof OriginHandshakeInterceptor;
				assertThat(condition).isTrue();
			}
		}
	}

	@Test
	void webSocketHandlersAttributes() {
		loadBeanDefinitions("websocket-config-handlers-attributes.xml");

		HandlerMapping handlerMapping = this.appContext.getBean(HandlerMapping.class);
		assertThat(handlerMapping).isNotNull();
		boolean condition2 = handlerMapping instanceof SimpleUrlHandlerMapping;
		assertThat(condition2).isTrue();

		SimpleUrlHandlerMapping urlHandlerMapping = (SimpleUrlHandlerMapping) handlerMapping;
		assertThat(urlHandlerMapping.getOrder()).isEqualTo(2);

		WebSocketHttpRequestHandler handler = (WebSocketHttpRequestHandler) urlHandlerMapping.getUrlMap().get("/foo");
		assertThat(handler).isNotNull();
		unwrapAndCheckDecoratedHandlerType(handler.getWebSocketHandler(), FooWebSocketHandler.class);
		HandshakeHandler handshakeHandler = handler.getHandshakeHandler();
		assertThat(handshakeHandler).isNotNull();
		boolean condition1 = handshakeHandler instanceof TestHandshakeHandler;
		assertThat(condition1).isTrue();
		List<HandshakeInterceptor> interceptors = handler.getHandshakeInterceptors();
		assertThat(interceptors).extracting("class")
				.containsExactlyInAnyOrder(FooTestInterceptor.class, BarTestInterceptor.class, OriginHandshakeInterceptor.class);

		handler = (WebSocketHttpRequestHandler) urlHandlerMapping.getUrlMap().get("/test");
		assertThat(handler).isNotNull();
		unwrapAndCheckDecoratedHandlerType(handler.getWebSocketHandler(), TestWebSocketHandler.class);
		handshakeHandler = handler.getHandshakeHandler();
		assertThat(handshakeHandler).isNotNull();
		boolean condition = handshakeHandler instanceof TestHandshakeHandler;
		assertThat(condition).isTrue();
		interceptors = handler.getHandshakeInterceptors();
		assertThat(interceptors).extracting("class")
				.containsExactlyInAnyOrder(FooTestInterceptor.class, BarTestInterceptor.class, OriginHandshakeInterceptor.class);
	}

	@Test
	void sockJs() {
		loadBeanDefinitions("websocket-config-handlers-sockjs.xml");

		SimpleUrlHandlerMapping handlerMapping = this.appContext.getBean(SimpleUrlHandlerMapping.class);
		assertThat(handlerMapping).isNotNull();

		SockJsHttpRequestHandler testHandler = (SockJsHttpRequestHandler) handlerMapping.getUrlMap().get("/test/**");
		assertThat(testHandler).isNotNull();
		unwrapAndCheckDecoratedHandlerType(testHandler.getWebSocketHandler(), TestWebSocketHandler.class);
		SockJsService testSockJsService = testHandler.getSockJsService();

		SockJsHttpRequestHandler fooHandler = (SockJsHttpRequestHandler) handlerMapping.getUrlMap().get("/foo/**");
		assertThat(fooHandler).isNotNull();
		unwrapAndCheckDecoratedHandlerType(fooHandler.getWebSocketHandler(), FooWebSocketHandler.class);
		SockJsService sockJsService = fooHandler.getSockJsService();
		assertThat(sockJsService).isNotNull();

		assertThat(sockJsService).isSameAs(testSockJsService);

		assertThat(sockJsService).isInstanceOf(DefaultSockJsService.class);
		DefaultSockJsService defaultSockJsService = (DefaultSockJsService) sockJsService;
		assertThat(defaultSockJsService.getTaskScheduler()).isInstanceOf(ThreadPoolTaskScheduler.class);
		assertThat(defaultSockJsService.shouldSuppressCors()).isFalse();

		Map<TransportType, TransportHandler> handlerMap = defaultSockJsService.getTransportHandlers();
		assertThat(handlerMap.values()).extracting("class")
				.containsExactlyInAnyOrder(
						XhrPollingTransportHandler.class,
						XhrReceivingTransportHandler.class,
						XhrStreamingTransportHandler.class,
						EventSourceTransportHandler.class,
						HtmlFileTransportHandler.class,
						WebSocketTransportHandler.class);

		WebSocketTransportHandler handler = (WebSocketTransportHandler) handlerMap.get(TransportType.WEBSOCKET);
		assertThat(handler.getHandshakeHandler().getClass()).isEqualTo(TestHandshakeHandler.class);

		List<HandshakeInterceptor> interceptors = defaultSockJsService.getHandshakeInterceptors();
		assertThat(interceptors).extracting("class")
				.containsExactlyInAnyOrder(FooTestInterceptor.class, BarTestInterceptor.class, OriginHandshakeInterceptor.class);
	}

	@Test
	void sockJsAttributes() {
		loadBeanDefinitions("websocket-config-handlers-sockjs-attributes.xml");

		SimpleUrlHandlerMapping handlerMapping = appContext.getBean(SimpleUrlHandlerMapping.class);
		assertThat(handlerMapping).isNotNull();

		SockJsHttpRequestHandler handler = (SockJsHttpRequestHandler) handlerMapping.getUrlMap().get("/test/**");
		assertThat(handler).isNotNull();
		unwrapAndCheckDecoratedHandlerType(handler.getWebSocketHandler(), TestWebSocketHandler.class);

		SockJsService sockJsService = handler.getSockJsService();
		assertThat(sockJsService).isNotNull();
		assertThat(sockJsService).isInstanceOf(TransportHandlingSockJsService.class);
		TransportHandlingSockJsService transportService = (TransportHandlingSockJsService) sockJsService;
		assertThat(transportService.getTaskScheduler()).isInstanceOf(TestTaskScheduler.class);
		assertThat(transportService.getTransportHandlers().values()).extracting("class")
				.containsExactlyInAnyOrder(XhrPollingTransportHandler.class, XhrStreamingTransportHandler.class);

		assertThat(transportService.getName()).isEqualTo("testSockJsService");
		assertThat(transportService.isWebSocketEnabled()).isFalse();
		assertThat(transportService.isSessionCookieNeeded()).isFalse();
		assertThat(transportService.getStreamBytesLimit()).isEqualTo(2048);
		assertThat(transportService.getDisconnectDelay()).isEqualTo(256);
		assertThat(transportService.getHttpMessageCacheSize()).isEqualTo(1024);
		assertThat(transportService.getHeartbeatTime()).isEqualTo(20);
		assertThat(transportService.getSockJsClientLibraryUrl()).isEqualTo("/js/sockjs.min.js");
		assertThat(transportService.getMessageCodec().getClass()).isEqualTo(TestMessageCodec.class);

		List<HandshakeInterceptor> interceptors = transportService.getHandshakeInterceptors();
		assertThat(interceptors).extracting("class").containsExactly(OriginHandshakeInterceptor.class);
		assertThat(transportService.shouldSuppressCors()).isTrue();
		assertThat(transportService.getAllowedOrigins()).containsExactly("https://mydomain1.example", "https://mydomain2.example");
		assertThat(transportService.getAllowedOriginPatterns()).containsExactly("https://*.mydomain.example");
	}


	private void loadBeanDefinitions(String fileName) {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.appContext);
		ClassPathResource resource = new ClassPathResource(fileName, HandlersBeanDefinitionParserTests.class);
		reader.loadBeanDefinitions(resource);
		this.appContext.refresh();
	}

	private static void unwrapAndCheckDecoratedHandlerType(WebSocketHandler handler, Class<?> handlerClass) {
		if (handler instanceof WebSocketHandlerDecorator) {
			handler = ((WebSocketHandlerDecorator) handler).getLastHandler();
		}
		assertThat(handler).isInstanceOf(handlerClass);
	}
}


class TestWebSocketHandler implements WebSocketHandler {

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
	}

	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
	}

	@Override
	public boolean supportsPartialMessages() {
		return false;
	}
}


class FooWebSocketHandler extends TestWebSocketHandler {
}


class TestHandshakeHandler implements HandshakeHandler {

	@Override
	public boolean doHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Map<String, Object> attributes) {

		return false;
	}
}


class TestChannelInterceptor implements ChannelInterceptor {
}


class FooTestInterceptor implements HandshakeInterceptor {

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Map<String, Object> attributes) {

		return false;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Exception exception) {
	}
}


class BarTestInterceptor extends FooTestInterceptor {
}


@SuppressWarnings("rawtypes")
class TestTaskScheduler implements TaskScheduler {

	@Override
	public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
		return null;
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
		return null;
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
		return null;
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
		return null;
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
		return null;
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
		return null;
	}
}


class TestMessageCodec implements SockJsMessageCodec {

	@Override
	public String encode(String... messages) {
		return null;
	}

	@Override
	public String[] decode(String content) {
		return new String[0];
	}

	@Override
	public String[] decodeInputStream(InputStream content) {
		return new String[0];
	}
}
