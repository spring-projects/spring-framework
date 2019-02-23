/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.socket.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.junit.Test;

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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Test fixture for HandlersBeanDefinitionParser.
 * See test configuration files websocket-config-handlers-*.xml.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 */
public class HandlersBeanDefinitionParserTests {

	private final GenericWebApplicationContext appContext = new GenericWebApplicationContext();


	@Test
	public void webSocketHandlers() {
		loadBeanDefinitions("websocket-config-handlers.xml");

		Map<String, HandlerMapping> handlersMap = this.appContext.getBeansOfType(HandlerMapping.class);
		assertNotNull(handlersMap);
		assertThat(handlersMap.values(), hasSize(2));

		for (HandlerMapping hm : handlersMap.values()) {
			assertTrue(hm instanceof SimpleUrlHandlerMapping);
			SimpleUrlHandlerMapping shm = (SimpleUrlHandlerMapping) hm;

			if (shm.getUrlMap().keySet().contains("/foo")) {
				assertThat(shm.getUrlMap().keySet(), contains("/foo", "/bar"));
				WebSocketHttpRequestHandler handler = (WebSocketHttpRequestHandler) shm.getUrlMap().get("/foo");
				assertNotNull(handler);
				unwrapAndCheckDecoratedHandlerType(handler.getWebSocketHandler(), FooWebSocketHandler.class);
				HandshakeHandler handshakeHandler = handler.getHandshakeHandler();
				assertNotNull(handshakeHandler);
				assertTrue(handshakeHandler instanceof DefaultHandshakeHandler);
				assertFalse(handler.getHandshakeInterceptors().isEmpty());
				assertTrue(handler.getHandshakeInterceptors().get(0) instanceof OriginHandshakeInterceptor);
			}
			else {
				assertThat(shm.getUrlMap().keySet(), contains("/test"));
				WebSocketHttpRequestHandler handler = (WebSocketHttpRequestHandler) shm.getUrlMap().get("/test");
				assertNotNull(handler);
				unwrapAndCheckDecoratedHandlerType(handler.getWebSocketHandler(), TestWebSocketHandler.class);
				HandshakeHandler handshakeHandler = handler.getHandshakeHandler();
				assertNotNull(handshakeHandler);
				assertTrue(handshakeHandler instanceof DefaultHandshakeHandler);
				assertFalse(handler.getHandshakeInterceptors().isEmpty());
				assertTrue(handler.getHandshakeInterceptors().get(0) instanceof OriginHandshakeInterceptor);
			}
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void webSocketHandlersAttributes() {
		loadBeanDefinitions("websocket-config-handlers-attributes.xml");

		HandlerMapping handlerMapping = this.appContext.getBean(HandlerMapping.class);
		assertNotNull(handlerMapping);
		assertTrue(handlerMapping instanceof SimpleUrlHandlerMapping);

		SimpleUrlHandlerMapping urlHandlerMapping = (SimpleUrlHandlerMapping) handlerMapping;
		assertEquals(2, urlHandlerMapping.getOrder());

		WebSocketHttpRequestHandler handler = (WebSocketHttpRequestHandler) urlHandlerMapping.getUrlMap().get("/foo");
		assertNotNull(handler);
		unwrapAndCheckDecoratedHandlerType(handler.getWebSocketHandler(), FooWebSocketHandler.class);
		HandshakeHandler handshakeHandler = handler.getHandshakeHandler();
		assertNotNull(handshakeHandler);
		assertTrue(handshakeHandler instanceof TestHandshakeHandler);
		List<HandshakeInterceptor> interceptors = handler.getHandshakeInterceptors();
		assertThat(interceptors, contains(instanceOf(FooTestInterceptor.class),
				instanceOf(BarTestInterceptor.class), instanceOf(OriginHandshakeInterceptor.class)));

		handler = (WebSocketHttpRequestHandler) urlHandlerMapping.getUrlMap().get("/test");
		assertNotNull(handler);
		unwrapAndCheckDecoratedHandlerType(handler.getWebSocketHandler(), TestWebSocketHandler.class);
		handshakeHandler = handler.getHandshakeHandler();
		assertNotNull(handshakeHandler);
		assertTrue(handshakeHandler instanceof TestHandshakeHandler);
		interceptors = handler.getHandshakeInterceptors();
		assertThat(interceptors, contains(instanceOf(FooTestInterceptor.class),
				instanceOf(BarTestInterceptor.class), instanceOf(OriginHandshakeInterceptor.class)));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void sockJs() {
		loadBeanDefinitions("websocket-config-handlers-sockjs.xml");

		SimpleUrlHandlerMapping handlerMapping = this.appContext.getBean(SimpleUrlHandlerMapping.class);
		assertNotNull(handlerMapping);

		SockJsHttpRequestHandler testHandler = (SockJsHttpRequestHandler) handlerMapping.getUrlMap().get("/test/**");
		assertNotNull(testHandler);
		unwrapAndCheckDecoratedHandlerType(testHandler.getWebSocketHandler(), TestWebSocketHandler.class);
		SockJsService testSockJsService = testHandler.getSockJsService();

		SockJsHttpRequestHandler fooHandler = (SockJsHttpRequestHandler) handlerMapping.getUrlMap().get("/foo/**");
		assertNotNull(fooHandler);
		unwrapAndCheckDecoratedHandlerType(fooHandler.getWebSocketHandler(), FooWebSocketHandler.class);
		SockJsService sockJsService = fooHandler.getSockJsService();
		assertNotNull(sockJsService);

		assertSame(testSockJsService, sockJsService);

		assertThat(sockJsService, instanceOf(DefaultSockJsService.class));
		DefaultSockJsService defaultSockJsService = (DefaultSockJsService) sockJsService;
		assertThat(defaultSockJsService.getTaskScheduler(), instanceOf(ThreadPoolTaskScheduler.class));
		assertFalse(defaultSockJsService.shouldSuppressCors());

		Map<TransportType, TransportHandler> handlerMap = defaultSockJsService.getTransportHandlers();
		assertThat(handlerMap.values(),
				containsInAnyOrder(
						instanceOf(XhrPollingTransportHandler.class),
						instanceOf(XhrReceivingTransportHandler.class),
						instanceOf(XhrStreamingTransportHandler.class),
						instanceOf(EventSourceTransportHandler.class),
						instanceOf(HtmlFileTransportHandler.class),
						instanceOf(WebSocketTransportHandler.class)));

		WebSocketTransportHandler handler = (WebSocketTransportHandler) handlerMap.get(TransportType.WEBSOCKET);
		assertEquals(TestHandshakeHandler.class, handler.getHandshakeHandler().getClass());

		List<HandshakeInterceptor> interceptors = defaultSockJsService.getHandshakeInterceptors();
		assertThat(interceptors, contains(instanceOf(FooTestInterceptor.class),
				instanceOf(BarTestInterceptor.class), instanceOf(OriginHandshakeInterceptor.class)));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void sockJsAttributes() {
		loadBeanDefinitions("websocket-config-handlers-sockjs-attributes.xml");

		SimpleUrlHandlerMapping handlerMapping = appContext.getBean(SimpleUrlHandlerMapping.class);
		assertNotNull(handlerMapping);

		SockJsHttpRequestHandler handler = (SockJsHttpRequestHandler) handlerMapping.getUrlMap().get("/test/**");
		assertNotNull(handler);
		unwrapAndCheckDecoratedHandlerType(handler.getWebSocketHandler(), TestWebSocketHandler.class);

		SockJsService sockJsService = handler.getSockJsService();
		assertNotNull(sockJsService);
		assertThat(sockJsService, instanceOf(TransportHandlingSockJsService.class));
		TransportHandlingSockJsService transportService = (TransportHandlingSockJsService) sockJsService;
		assertThat(transportService.getTaskScheduler(), instanceOf(TestTaskScheduler.class));
		assertThat(transportService.getTransportHandlers().values(),
				containsInAnyOrder(
						instanceOf(XhrPollingTransportHandler.class),
						instanceOf(XhrStreamingTransportHandler.class)));

		assertEquals("testSockJsService", transportService.getName());
		assertFalse(transportService.isWebSocketEnabled());
		assertFalse(transportService.isSessionCookieNeeded());
		assertEquals(2048, transportService.getStreamBytesLimit());
		assertEquals(256, transportService.getDisconnectDelay());
		assertEquals(1024, transportService.getHttpMessageCacheSize());
		assertEquals(20, transportService.getHeartbeatTime());
		assertEquals("/js/sockjs.min.js", transportService.getSockJsClientLibraryUrl());
		assertEquals(TestMessageCodec.class, transportService.getMessageCodec().getClass());

		List<HandshakeInterceptor> interceptors = transportService.getHandshakeInterceptors();
		assertThat(interceptors, contains(instanceOf(OriginHandshakeInterceptor.class)));
		assertTrue(transportService.shouldSuppressCors());
		assertTrue(transportService.getAllowedOrigins().contains("http://mydomain1.com"));
		assertTrue(transportService.getAllowedOrigins().contains("http://mydomain2.com"));
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
		assertTrue(handlerClass.isInstance(handler));
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


@SuppressWarnings({ "unchecked", "rawtypes" })
class TestTaskScheduler implements TaskScheduler {

	@Override
	public ScheduledFuture schedule(Runnable task, Trigger trigger) {
		return null;
	}

	@Override
	public ScheduledFuture schedule(Runnable task, Date startTime) {
		return null;
	}

	@Override
	public ScheduledFuture scheduleAtFixedRate(Runnable task, Date startTime, long period) {
		return null;
	}

	@Override
	public ScheduledFuture scheduleAtFixedRate(Runnable task, long period) {
		return null;
	}

	@Override
	public ScheduledFuture scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
		return null;
	}

	@Override
	public ScheduledFuture scheduleWithFixedDelay(Runnable task, long delay) {
		return null;
	}
}


class TestMessageCodec implements SockJsMessageCodec {

	@Override
	public String encode(String... messages) {
		return null;
	}

	@Override
	public String[] decode(String content) throws IOException {
		return new String[0];
	}

	@Override
	public String[] decodeInputStream(InputStream content) throws IOException {
		return new String[0];
	}
}
