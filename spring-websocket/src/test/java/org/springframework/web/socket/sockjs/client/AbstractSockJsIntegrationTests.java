/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.socket.sockjs.client;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.testfixture.EnabledForTestGroups;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.WebSocketTestServer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.RequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.core.testfixture.TestGroup.PERFORMANCE;

/**
 * Abstract base class for integration tests using the
 * {@link org.springframework.web.socket.sockjs.client.SockJsClient SockJsClient}
 * against actual SockJS server endpoints.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
@EnabledForTestGroups(PERFORMANCE)
public abstract class AbstractSockJsIntegrationTests {

	protected Log logger = LogFactory.getLog(getClass());


	private SockJsClient sockJsClient;

	private WebSocketTestServer server;

	private AnnotationConfigWebApplicationContext wac;

	private TestFilter testFilter;

	private String baseUrl;


	@BeforeEach
	public void setup(TestInfo testInfo) throws Exception {
		logger.debug("Setting up '" + testInfo.getTestMethod().get().getName() + "'");

		this.testFilter = new TestFilter();

		this.wac = new AnnotationConfigWebApplicationContext();
		this.wac.register(TestConfig.class, upgradeStrategyConfigClass());

		this.server = createWebSocketTestServer();
		this.server.setup();
		this.server.deployConfig(this.wac, this.testFilter);
		this.server.start();

		this.wac.setServletContext(this.server.getServletContext());
		this.wac.refresh();

		this.baseUrl = "http://localhost:" + this.server.getPort();
	}

	@AfterEach
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

	@Test
	public void echoWebSocket() throws Exception {
		testEcho(100, createWebSocketTransport(), null);
	}

	@Test
	public void echoXhrStreaming() throws Exception {
		testEcho(100, createXhrTransport(), null);
	}

	@Test
	public void echoXhr() throws Exception {
		AbstractXhrTransport xhrTransport = createXhrTransport();
		xhrTransport.setXhrStreamingDisabled(true);
		testEcho(100, xhrTransport, null);
	}

	// SPR-13254

	@Test
	public void echoXhrWithHeaders() throws Exception {
		AbstractXhrTransport xhrTransport = createXhrTransport();
		xhrTransport.setXhrStreamingDisabled(true);

		WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
		headers.add("auth", "123");
		testEcho(10, xhrTransport, headers);

		for (Map.Entry<String, HttpHeaders> entry : this.testFilter.requests.entrySet()) {
			HttpHeaders httpHeaders = entry.getValue();
			assertThat(httpHeaders.getFirst("auth")).as("No auth header for: " + entry.getKey()).isEqualTo("123");
		}
	}

	@Test
	public void receiveOneMessageWebSocket() throws Exception {
		testReceiveOneMessage(createWebSocketTransport(), null);
	}

	@Test
	public void receiveOneMessageXhrStreaming() throws Exception {
		testReceiveOneMessage(createXhrTransport(), null);
	}

	@Test
	public void receiveOneMessageXhr() throws Exception {
		AbstractXhrTransport xhrTransport = createXhrTransport();
		xhrTransport.setXhrStreamingDisabled(true);
		testReceiveOneMessage(xhrTransport, null);
	}

	@Test
	public void infoRequestFailure() throws Exception {
		TestClientHandler handler = new TestClientHandler();
		this.testFilter.sendErrorMap.put("/info", 500);
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
		assertThat(latch.await(5000, TimeUnit.MILLISECONDS)).isTrue();
	}

	@Test
	public void fallbackAfterTransportFailure() throws Exception {
		this.testFilter.sendErrorMap.put("/websocket", 200);
		this.testFilter.sendErrorMap.put("/xhr_streaming", 500);
		TestClientHandler handler = new TestClientHandler();
		initSockJsClient(createWebSocketTransport(), createXhrTransport());
		WebSocketSession session = this.sockJsClient.doHandshake(handler, this.baseUrl + "/echo").get();
		assertThat(session.getClass()).as("Fallback didn't occur").isEqualTo(XhrClientSockJsSession.class);
		TextMessage message = new TextMessage("message1");
		session.sendMessage(message);
		handler.awaitMessage(message, 5000);
	}

	@Test
	@Timeout(5)
	public void fallbackAfterConnectTimeout() throws Exception {
		TestClientHandler clientHandler = new TestClientHandler();
		this.testFilter.sleepDelayMap.put("/xhr_streaming", 10000L);
		this.testFilter.sendErrorMap.put("/xhr_streaming", 503);
		initSockJsClient(createXhrTransport());
		this.sockJsClient.setConnectTimeoutScheduler(this.wac.getBean(ThreadPoolTaskScheduler.class));
		WebSocketSession clientSession = sockJsClient.doHandshake(clientHandler, this.baseUrl + "/echo").get();
		assertThat(clientSession.getClass()).as("Fallback didn't occur").isEqualTo(XhrClientSockJsSession.class);
		TextMessage message = new TextMessage("message1");
		clientSession.sendMessage(message);
		clientHandler.awaitMessage(message, 5000);
		clientSession.close();
	}


	private void testEcho(int messageCount, Transport transport, WebSocketHttpHeaders headers) throws Exception {
		List<TextMessage> messages = new ArrayList<>();
		for (int i = 0; i < messageCount; i++) {
			messages.add(new TextMessage("m" + i));
		}
		TestClientHandler handler = new TestClientHandler();
		initSockJsClient(transport);
		URI url = new URI(this.baseUrl + "/echo");
		WebSocketSession session = this.sockJsClient.doHandshake(handler, headers, url).get();
		for (TextMessage message : messages) {
			session.sendMessage(message);
		}
		handler.awaitMessageCount(messageCount, 5000);
		for (TextMessage message : messages) {
			assertThat(handler.receivedMessages.remove(message)).as("Message not received: " + message).isTrue();
		}
		assertThat(handler.receivedMessages.size()).as("Remaining messages: " + handler.receivedMessages).isEqualTo(0);
		session.close();
	}

	private void testReceiveOneMessage(Transport transport, WebSocketHttpHeaders headers)
			throws Exception {

		TestClientHandler clientHandler = new TestClientHandler();
		initSockJsClient(transport);
		this.sockJsClient.doHandshake(clientHandler, headers, new URI(this.baseUrl + "/test")).get();
		TestServerHandler serverHandler = this.wac.getBean(TestServerHandler.class);

		assertThat(clientHandler.session).as("afterConnectionEstablished should have been called").isNotNull();
		serverHandler.awaitSession(5000);

		TextMessage message = new TextMessage("message1");
		serverHandler.session.sendMessage(message);
		clientHandler.awaitMessage(message, 5000);
	}

	private static void awaitEvent(BooleanSupplier condition, long timeToWait, String description) {
		long timeToSleep = 200;
		for (int i = 0 ; i < Math.floor(timeToWait / timeToSleep); i++) {
			if (condition.getAsBoolean()) {
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
				assertThat(actual).isEqualTo(expected);
			}
			else if (this.transportError != null) {
				throw new AssertionError("Transport error", this.transportError);
			}
			else {
				fail("Timed out waiting for [" + expected + "]");
			}
		}
	}

	private static class EchoHandler extends TextWebSocketHandler {

		@Override
		protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
			session.sendMessage(message);
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

	private static class TestFilter implements Filter {

		private final Map<String, HttpHeaders> requests = new HashMap<>();

		private final Map<String, Long> sleepDelayMap = new HashMap<>();

		private final Map<String, Integer> sendErrorMap = new HashMap<>();


		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {

			HttpServletRequest httpRequest = (HttpServletRequest) request;
			String uri = httpRequest.getRequestURI();
			HttpHeaders headers = new ServletServerHttpRequest(httpRequest).getHeaders();
			this.requests.put(uri, headers);

			for (String suffix : this.sleepDelayMap.keySet()) {
				if ((httpRequest).getRequestURI().endsWith(suffix)) {
					try {
						Thread.sleep(this.sleepDelayMap.get(suffix));
						break;
					}
					catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			for (String suffix : this.sendErrorMap.keySet()) {
				if ((httpRequest).getRequestURI().endsWith(suffix)) {
					((HttpServletResponse) response).sendError(this.sendErrorMap.get(suffix));
					return;
				}
			}
			chain.doFilter(request, response);
		}

		@Override
		public void init(FilterConfig filterConfig) {
		}

		@Override
		public void destroy() {
		}
	}

}
