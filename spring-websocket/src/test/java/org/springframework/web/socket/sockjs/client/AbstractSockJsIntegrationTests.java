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

package org.springframework.web.socket.sockjs.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.WebSocketTestServer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.RequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import static org.junit.Assert.*;

/**
 * Integration tests using the
 * {@link org.springframework.web.socket.sockjs.client.SockJsClient}.
 * against actual SockJS server endpoints.
 *
 * @author Rossen Stoyanchev
 */
public abstract class AbstractSockJsIntegrationTests {

	@Rule
	public final TestName testName = new TestName();

	protected Log logger = LogFactory.getLog(getClass());


	private SockJsClient sockJsClient;

	private WebSocketTestServer server;

	private AnnotationConfigWebApplicationContext wac;

	private ErrorFilter errorFilter;

	private String baseUrl;


	@Before
	public void setup() throws Exception {
		logger.debug("Setting up '" + this.testName.getMethodName() + "'");
		this.errorFilter = new ErrorFilter();
		this.wac = new AnnotationConfigWebApplicationContext();
		this.wac.register(TestConfig.class, upgradeStrategyConfigClass());
		this.wac.refresh();
		this.server = createWebSocketTestServer();
		this.server.setup();
		this.server.deployConfig(this.wac, this.errorFilter);
		this.server.start();
		this.baseUrl = "http://localhost:" + this.server.getPort();
	}

	@After
	public void teardown() throws Exception {
		try {
			this.sockJsClient.stop();
		}
		catch (Throwable ex) {
			logger.error("Failed to stop SockJsClient", ex);
		}
		try {
			this.server.undeployConfig();
		}
		catch (Throwable t) {
			logger.error("Failed to undeploy application config", t);
		}
		try {
			this.server.stop();
		}
		catch (Throwable t) {
			logger.error("Failed to stop server", t);
		}
		try {
			this.wac.close();
		}
		catch (Throwable t) {
			logger.error("Failed to close WebApplicationContext", t);
		}
	}

	protected abstract Class<?> upgradeStrategyConfigClass();

	protected abstract WebSocketTestServer createWebSocketTestServer();

	protected abstract Transport createWebSocketTransport();

	protected abstract AbstractXhrTransport createXhrTransport();

	protected void initSockJsClient(Transport... transports) {
		this.sockJsClient = new SockJsClient(Arrays.asList(transports));
		this.sockJsClient.start();
	}

	// Temporarily @Ignore failures caused by suspected Jetty bug

	@Test
	public void echoWebSocket() throws Exception {
		testEcho(100, createWebSocketTransport());
	}

	@Test
	public void echoXhrStreaming() throws Exception {
		testEcho(100, createXhrTransport());
	}

	@Test
	public void echoXhr() throws Exception {
		AbstractXhrTransport xhrTransport = createXhrTransport();
		xhrTransport.setXhrStreamingDisabled(true);
		testEcho(100, xhrTransport);
	}

	@Test
	public void receiveOneMessageWebSocket() throws Exception {
		testReceiveOneMessage(createWebSocketTransport());
	}

	@Test
	public void receiveOneMessageXhrStreaming() throws Exception {
		testReceiveOneMessage(createXhrTransport());
	}

	@Test
	public void receiveOneMessageXhr() throws Exception {
		AbstractXhrTransport xhrTransport = createXhrTransport();
		xhrTransport.setXhrStreamingDisabled(true);
		testReceiveOneMessage(xhrTransport);
	}

	@Test
	public void infoRequestFailure() throws Exception {
		TestClientHandler handler = new TestClientHandler();
		this.errorFilter.responseStatusMap.put("/info", 500);
		CountDownLatch latch = new CountDownLatch(1);
		initSockJsClient(createWebSocketTransport());
		this.sockJsClient.doHandshake(handler, this.baseUrl + "/echo").addCallback(
				new ListenableFutureCallback<WebSocketSession>() {
					@Override
					public void onSuccess(WebSocketSession result) {
					}
					@Override
					public void onFailure(Throwable ex) {
						latch.countDown();
					}
				}
		);
		assertTrue(latch.await(5000, TimeUnit.MILLISECONDS));
	}

	@Test
	public void fallbackAfterTransportFailure() throws Exception {
		this.errorFilter.responseStatusMap.put("/websocket", 200);
		this.errorFilter.responseStatusMap.put("/xhr_streaming", 500);
		TestClientHandler handler = new TestClientHandler();
		initSockJsClient(createWebSocketTransport(), createXhrTransport());
		WebSocketSession session = this.sockJsClient.doHandshake(handler, this.baseUrl + "/echo").get();
		assertEquals("Fallback didn't occur", XhrClientSockJsSession.class, session.getClass());
		TextMessage message = new TextMessage("message1");
		session.sendMessage(message);
		handler.awaitMessage(message, 5000);
	}

	@Test(timeout = 5000)
	public void fallbackAfterConnectTimeout() throws Exception {
		TestClientHandler clientHandler = new TestClientHandler();
		this.errorFilter.sleepDelayMap.put("/xhr_streaming", 10000L);
		this.errorFilter.responseStatusMap.put("/xhr_streaming", 503);
		initSockJsClient(createXhrTransport());
		this.sockJsClient.setConnectTimeoutScheduler(this.wac.getBean(ThreadPoolTaskScheduler.class));
		WebSocketSession clientSession = sockJsClient.doHandshake(clientHandler, this.baseUrl + "/echo").get();
		assertEquals("Fallback didn't occur", XhrClientSockJsSession.class, clientSession.getClass());
		TextMessage message = new TextMessage("message1");
		clientSession.sendMessage(message);
		clientHandler.awaitMessage(message, 5000);
		clientSession.close();
	}


	private void testEcho(int messageCount, Transport transport) throws Exception {
		List<TextMessage> messages = new ArrayList<>();
		for (int i = 0; i < messageCount; i++) {
			messages.add(new TextMessage("m" + i));
		}
		TestClientHandler handler = new TestClientHandler();
		initSockJsClient(transport);
		WebSocketSession session = this.sockJsClient.doHandshake(handler, this.baseUrl + "/echo").get();
		for (TextMessage message : messages) {
			session.sendMessage(message);
		}
		handler.awaitMessageCount(messageCount, 5000);
		for (TextMessage message : messages) {
			assertTrue("Message not received: " + message, handler.receivedMessages.remove(message));
		}
		assertEquals("Remaining messages: " + handler.receivedMessages, 0, handler.receivedMessages.size());
		session.close();
	}

	private void testReceiveOneMessage(Transport transport) throws Exception {
		TestClientHandler clientHandler = new TestClientHandler();
		initSockJsClient(transport);
		this.sockJsClient.doHandshake(clientHandler, this.baseUrl + "/test").get();
		TestServerHandler serverHandler = this.wac.getBean(TestServerHandler.class);

		assertNotNull("afterConnectionEstablished should have been called", clientHandler.session);
		serverHandler.awaitSession(5000);

		TextMessage message = new TextMessage("message1");
		serverHandler.session.sendMessage(message);
		clientHandler.awaitMessage(message, 5000);
	}


	@Configuration
	@EnableWebSocket
	static class TestConfig implements WebSocketConfigurer {

		@Autowired
		private RequestUpgradeStrategy upgradeStrategy;

		@Override
		public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
			HandshakeHandler handshakeHandler = new DefaultHandshakeHandler(this.upgradeStrategy);
			registry.addHandler(new EchoHandler(), "/echo").setHandshakeHandler(handshakeHandler).withSockJS();
			registry.addHandler(testServerHandler(), "/test").setHandshakeHandler(handshakeHandler).withSockJS();
		}

		@Bean
		public TestServerHandler testServerHandler() {
			return new TestServerHandler();
		}
	}

	private static interface Condition {
		boolean match();
	}

	private static void awaitEvent(Condition condition, long timeToWait, String description) {
		long timeToSleep = 200;
		for (int i = 0 ; i < Math.floor(timeToWait / timeToSleep); i++) {
			if (condition.match()) {
				return;
			}
			try {
				Thread.sleep(timeToSleep);
			}
			catch (InterruptedException e) {
				throw new IllegalStateException("Interrupted while waiting for " + description, e);
			}
		}
		throw new IllegalStateException("Timed out waiting for " + description);
	}

	private static class TestClientHandler extends TextWebSocketHandler {

		private final BlockingQueue<TextMessage> receivedMessages = new LinkedBlockingQueue<>();

		private volatile WebSocketSession session;

		private volatile Throwable transportError;


		@Override
		public void afterConnectionEstablished(WebSocketSession session) throws Exception {
			this.session = session;
		}

		@Override
		protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
			this.receivedMessages.add(message);
		}

		@Override
		public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
			this.transportError = exception;
		}

		public void awaitMessageCount(final int count, long timeToWait) throws Exception {
			awaitEvent(() -> receivedMessages.size() >= count, timeToWait,
					count + " number of messages. Received so far: " + this.receivedMessages);
		}

		public void awaitMessage(TextMessage expected, long timeToWait) throws InterruptedException {
			TextMessage actual = this.receivedMessages.poll(timeToWait, TimeUnit.MILLISECONDS);
			if (actual != null) {
				assertEquals(expected, actual);
			}
			else if (this.transportError != null) {
				throw new AssertionError("Transport error", this.transportError);
			}
			else {
				fail("Timed out waiting for [" + expected + "]");
			}
		}
	}

	private static class TestServerHandler extends TextWebSocketHandler {

		private WebSocketSession session;

		@Override
		public void afterConnectionEstablished(WebSocketSession session) throws Exception {
			this.session = session;
		}

		public WebSocketSession awaitSession(long timeToWait) throws InterruptedException {
			awaitEvent(() -> this.session != null, timeToWait, " session");
			return this.session;
		}
	}

	private static class EchoHandler extends TextWebSocketHandler {

		@Override
		protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
			session.sendMessage(message);
		}
	}

	private static class ErrorFilter implements Filter {

		private final Map<String, Integer> responseStatusMap = new HashMap<>();

		private final Map<String, Long> sleepDelayMap = new HashMap<>();

		@Override
		public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
			for (String suffix : this.sleepDelayMap.keySet()) {
				if (((HttpServletRequest) req).getRequestURI().endsWith(suffix)) {
					try {
						Thread.sleep(this.sleepDelayMap.get(suffix));
						break;
					}
					catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			for (String suffix : this.responseStatusMap.keySet()) {
				if (((HttpServletRequest) req).getRequestURI().endsWith(suffix)) {
					((HttpServletResponse) resp).sendError(this.responseStatusMap.get(suffix));
					return;
				}
			}
			chain.doFilter(req, resp);
		}

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
		}

		@Override
		public void destroy() {
		}
	}

}
