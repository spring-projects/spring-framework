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

package org.springframework.web.socket.config;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
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
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.socket.sockjs.support.SockJsHttpRequestHandler;
import org.springframework.web.socket.sockjs.transport.TransportHandlingSockJsService;
import org.springframework.web.socket.sockjs.transport.handler.DefaultSockJsService;
import org.springframework.web.socket.sockjs.transport.handler.EventSourceTransportHandler;
import org.springframework.web.socket.sockjs.transport.handler.HtmlFileTransportHandler;
import org.springframework.web.socket.sockjs.transport.handler.JsonpPollingTransportHandler;
import org.springframework.web.socket.sockjs.transport.handler.JsonpReceivingTransportHandler;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;
import org.springframework.web.socket.sockjs.transport.handler.XhrPollingTransportHandler;
import org.springframework.web.socket.sockjs.transport.handler.XhrReceivingTransportHandler;
import org.springframework.web.socket.sockjs.transport.handler.XhrStreamingTransportHandler;

import static org.junit.Assert.*;

/**
 * Test fixture for HandlersBeanDefinitionParser.
 * See test configuration files websocket-config-handlers-*.xml.
 *
 * @author Brian Clozel
 */
public class HandlersBeanDefinitionParserTests {

	private GenericWebApplicationContext appContext;

	@Before
	public void setup() {
		appContext = new GenericWebApplicationContext();
	}

	@Test
	public void webSocketHandlers() {
		loadBeanDefinitions("websocket-config-handlers.xml");
		Map<String, HandlerMapping> handlersMap = appContext.getBeansOfType(HandlerMapping.class);
		assertNotNull(handlersMap);
		assertThat(handlersMap.values(), Matchers.hasSize(2));

		for(HandlerMapping handlerMapping : handlersMap.values()) {
			assertTrue(handlerMapping instanceof SimpleUrlHandlerMapping);
			SimpleUrlHandlerMapping urlHandlerMapping = (SimpleUrlHandlerMapping) handlerMapping;

			if(urlHandlerMapping.getUrlMap().keySet().contains("/foo")) {
				assertThat(urlHandlerMapping.getUrlMap().keySet(),Matchers.contains("/foo","/bar"));
				WebSocketHttpRequestHandler handler = (WebSocketHttpRequestHandler)
						urlHandlerMapping.getUrlMap().get("/foo");
				assertNotNull(handler);
				checkDelegateHandlerType(handler.getWebSocketHandler(), FooWebSocketHandler.class);
				HandshakeHandler handshakeHandler = (HandshakeHandler)
						new DirectFieldAccessor(handler).getPropertyValue("handshakeHandler");
				assertNotNull(handshakeHandler);
				assertTrue(handshakeHandler instanceof DefaultHandshakeHandler);
			}
			else {
				assertThat(urlHandlerMapping.getUrlMap().keySet(),Matchers.contains("/test"));
				WebSocketHttpRequestHandler handler = (WebSocketHttpRequestHandler)
						urlHandlerMapping.getUrlMap().get("/test");
				assertNotNull(handler);
				checkDelegateHandlerType(handler.getWebSocketHandler(), TestWebSocketHandler.class);
				HandshakeHandler handshakeHandler = (HandshakeHandler)
						new DirectFieldAccessor(handler).getPropertyValue("handshakeHandler");
				assertNotNull(handshakeHandler);
				assertTrue(handshakeHandler instanceof DefaultHandshakeHandler);
			}
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void websocketHandlersAttributes() {
		loadBeanDefinitions("websocket-config-handlers-attributes.xml");
		HandlerMapping handlerMapping = appContext.getBean(HandlerMapping.class);
		assertNotNull(handlerMapping);
		assertTrue(handlerMapping instanceof SimpleUrlHandlerMapping);

		SimpleUrlHandlerMapping urlHandlerMapping = (SimpleUrlHandlerMapping) handlerMapping;
		assertEquals(2, urlHandlerMapping.getOrder());

		WebSocketHttpRequestHandler handler = (WebSocketHttpRequestHandler) urlHandlerMapping.getUrlMap().get("/foo");
		assertNotNull(handler);
		checkDelegateHandlerType(handler.getWebSocketHandler(), FooWebSocketHandler.class);
		HandshakeHandler handshakeHandler = (HandshakeHandler)
				new DirectFieldAccessor(handler).getPropertyValue("handshakeHandler");
		assertNotNull(handshakeHandler);
		assertTrue(handshakeHandler instanceof TestHandshakeHandler);
		List<HandshakeInterceptor> handshakeInterceptorList = (List<HandshakeInterceptor>)
				new DirectFieldAccessor(handler).getPropertyValue("interceptors");
		assertNotNull(handshakeInterceptorList);
		assertThat(handshakeInterceptorList, Matchers.contains(
				Matchers.instanceOf(FooTestInterceptor.class), Matchers.instanceOf(BarTestInterceptor.class)));

		handler = (WebSocketHttpRequestHandler) urlHandlerMapping.getUrlMap().get("/test");
		assertNotNull(handler);
		checkDelegateHandlerType(handler.getWebSocketHandler(), TestWebSocketHandler.class);
		handshakeHandler = (HandshakeHandler) new DirectFieldAccessor(handler).getPropertyValue("handshakeHandler");
		assertNotNull(handshakeHandler);
		assertTrue(handshakeHandler instanceof TestHandshakeHandler);
		handshakeInterceptorList = (List<HandshakeInterceptor>)
				new DirectFieldAccessor(handler).getPropertyValue("interceptors");
		assertNotNull(handshakeInterceptorList);
		assertThat(handshakeInterceptorList, Matchers.contains(
				Matchers.instanceOf(FooTestInterceptor.class), Matchers.instanceOf(BarTestInterceptor.class)));

	}

	@Test
	public void sockJsSupport() {
		loadBeanDefinitions("websocket-config-handlers-sockjs.xml");
		SimpleUrlHandlerMapping handlerMapping = appContext.getBean(SimpleUrlHandlerMapping.class);
		assertNotNull(handlerMapping);
		SockJsHttpRequestHandler testHandler = (SockJsHttpRequestHandler) handlerMapping.getUrlMap().get("/test/**");
		assertNotNull(testHandler);
		checkDelegateHandlerType(testHandler.getWebSocketHandler(), TestWebSocketHandler.class);
		SockJsService testSockJsService = testHandler.getSockJsService();
		SockJsHttpRequestHandler fooHandler = (SockJsHttpRequestHandler) handlerMapping.getUrlMap().get("/foo/**");
		assertNotNull(fooHandler);
		checkDelegateHandlerType(fooHandler.getWebSocketHandler(), FooWebSocketHandler.class);

		SockJsService sockJsService = fooHandler.getSockJsService();
		assertNotNull(sockJsService);
		assertEquals(testSockJsService, sockJsService);

		assertThat(sockJsService, Matchers.instanceOf(DefaultSockJsService.class));
		DefaultSockJsService defaultSockJsService = (DefaultSockJsService) sockJsService;
		assertThat(defaultSockJsService.getTaskScheduler(), Matchers.instanceOf(ThreadPoolTaskScheduler.class));
		assertThat(defaultSockJsService.getTransportHandlers().values(), Matchers.containsInAnyOrder(
				Matchers.instanceOf(XhrPollingTransportHandler.class),
				Matchers.instanceOf(XhrReceivingTransportHandler.class),
				Matchers.instanceOf(JsonpPollingTransportHandler.class),
				Matchers.instanceOf(JsonpReceivingTransportHandler.class),
				Matchers.instanceOf(XhrStreamingTransportHandler.class),
				Matchers.instanceOf(EventSourceTransportHandler.class),
				Matchers.instanceOf(HtmlFileTransportHandler.class),
				Matchers.instanceOf(WebSocketTransportHandler.class)));

	}

	@Test
	public void sockJsAttributesSupport() {
		loadBeanDefinitions("websocket-config-handlers-sockjs-attributes.xml");
		SimpleUrlHandlerMapping handlerMapping = appContext.getBean(SimpleUrlHandlerMapping.class);
		assertNotNull(handlerMapping);
		SockJsHttpRequestHandler handler = (SockJsHttpRequestHandler) handlerMapping.getUrlMap().get("/test/**");
		assertNotNull(handler);
		checkDelegateHandlerType(handler.getWebSocketHandler(), TestWebSocketHandler.class);
		SockJsService sockJsService = handler.getSockJsService();
		assertNotNull(sockJsService);
		assertThat(sockJsService, Matchers.instanceOf(TransportHandlingSockJsService.class));
		TransportHandlingSockJsService defaultSockJsService = (TransportHandlingSockJsService) sockJsService;
		assertThat(defaultSockJsService.getTaskScheduler(), Matchers.instanceOf(TestTaskScheduler.class));
		assertThat(defaultSockJsService.getTransportHandlers().values(), Matchers.containsInAnyOrder(
				Matchers.instanceOf(XhrPollingTransportHandler.class),
				Matchers.instanceOf(XhrStreamingTransportHandler.class)));

		assertEquals("testSockJsService", defaultSockJsService.getName());
		assertFalse(defaultSockJsService.isWebSocketEnabled());
		assertFalse(defaultSockJsService.isSessionCookieNeeded());
		assertEquals(2048, defaultSockJsService.getStreamBytesLimit());
		assertEquals(256, defaultSockJsService.getDisconnectDelay());
		assertEquals(1024, defaultSockJsService.getHttpMessageCacheSize());
		assertEquals(20, defaultSockJsService.getHeartbeatTime());
	}

	private void loadBeanDefinitions(String fileName) {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(appContext);
		ClassPathResource resource = new ClassPathResource(fileName, HandlersBeanDefinitionParserTests.class);
		reader.loadBeanDefinitions(resource);
		appContext.refresh();
	}

	private void checkDelegateHandlerType(WebSocketHandler handler, Class<?> handlerClass) {
		do {
			handler = (WebSocketHandler) new DirectFieldAccessor(handler).getPropertyValue("delegate");
		}
		while (new DirectFieldAccessor(handler).isReadableProperty("delegate"));
		assertTrue(handlerClass.isInstance(handler));
	}

}

class TestWebSocketHandler implements WebSocketHandler {

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {}

	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {}

	@Override
	public boolean supportsPartialMessages() { return false; }
}

class FooWebSocketHandler extends TestWebSocketHandler { }

class TestHandshakeHandler implements HandshakeHandler {
	@Override
	public boolean doHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Map<String, Object> attributes) throws HandshakeFailureException {
		return false;
	}
}

class TestChannelInterceptor extends ChannelInterceptorAdapter { }

class FooTestInterceptor implements HandshakeInterceptor {
	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
		return false;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Exception exception) {
	}
}

class BarTestInterceptor extends FooTestInterceptor {}

class TestTaskScheduler implements TaskScheduler {
	@Override
	public ScheduledFuture schedule(Runnable task, Trigger trigger) { return null; }

	@Override
	public ScheduledFuture schedule(Runnable task, Date startTime) { return null; }

	@Override
	public ScheduledFuture scheduleAtFixedRate(Runnable task, Date startTime, long period) { return null; }

	@Override
	public ScheduledFuture scheduleAtFixedRate(Runnable task, long period) { return null; }

	@Override
	public ScheduledFuture scheduleWithFixedDelay(Runnable task, Date startTime, long delay) { return null; }

	@Override
	public ScheduledFuture scheduleWithFixedDelay(Runnable task, long delay) { return null; }
}