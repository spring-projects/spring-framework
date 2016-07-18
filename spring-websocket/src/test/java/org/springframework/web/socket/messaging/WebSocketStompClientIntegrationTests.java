/*
 * Copyright 2002-2015 the original author or authors.
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
package org.springframework.web.socket.messaging;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.socket.TomcatWebSocketTestServer;
import org.springframework.web.socket.WebSocketTestServer;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurationSupport;
import org.springframework.web.socket.server.RequestUpgradeStrategy;
import org.springframework.web.socket.server.standard.TomcatRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;


/**
 * Integration tests for {@link WebSocketStompClient}.
 * @author Rossen Stoyanchev
 */
public class WebSocketStompClientIntegrationTests {

	private static final Log logger = LogFactory.getLog(WebSocketStompClientIntegrationTests.class);

	@Rule
	public final TestName testName = new TestName();

	private WebSocketStompClient stompClient;

	private WebSocketTestServer server;

	private AnnotationConfigWebApplicationContext wac;


	@Before
	public void setUp() throws Exception {

		logger.debug("Setting up before '" + this.testName.getMethodName() + "'");

		this.wac = new AnnotationConfigWebApplicationContext();
		this.wac.register(TestConfig.class);
		this.wac.refresh();

		this.server = new TomcatWebSocketTestServer();
		this.server.setup();
		this.server.deployConfig(this.wac);
		this.server.start();

		WebSocketClient webSocketClient = new StandardWebSocketClient();
		this.stompClient = new WebSocketStompClient(webSocketClient);
		this.stompClient.setMessageConverter(new StringMessageConverter());
	}

	@After
	public void tearDown() throws Exception {
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


	@Test
	public void publishSubscribe() throws Exception {

		String url = "ws://127.0.0.1:" + this.server.getPort() + "/stomp";

		TestHandler testHandler = new TestHandler("/topic/foo", "payload");
		this.stompClient.connect(url, testHandler);

		assertTrue(testHandler.awaitForMessageCount(1, 5000));
		assertThat(testHandler.getReceived(), containsInAnyOrder("payload"));
	}


	@Configuration
	static class TestConfig extends WebSocketMessageBrokerConfigurationSupport {

		@Override
		protected void registerStompEndpoints(StompEndpointRegistry registry) {
			// Can't rely on classpath detection
			RequestUpgradeStrategy upgradeStrategy = new TomcatRequestUpgradeStrategy();
			registry.addEndpoint("/stomp")
					.setHandshakeHandler(new DefaultHandshakeHandler(upgradeStrategy))
					.setAllowedOrigins("*");
		}

		@Override
		public void configureMessageBroker(MessageBrokerRegistry configurer) {
			configurer.setApplicationDestinationPrefixes("/app");
			configurer.enableSimpleBroker("/topic", "/queue");
		}
	}


	private static class TestHandler implements StompSessionHandler {

		private final String topic;

		private final Object payload;

		private final List<String> received = new ArrayList<>();


		public TestHandler(String topic, Object payload) {
			this.topic = topic;
			this.payload = payload;
		}


		public List<String> getReceived() {
			return this.received;
		}


		@Override
		public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
			session.subscribe(this.topic, new StompFrameHandler() {
				@Override
				public Type getPayloadType(StompHeaders headers) {
					return String.class;
				}
				@Override
				public void handleFrame(StompHeaders headers, Object payload) {
					received.add((String) payload);
				}
			});
			try {
				// Delay send since server processes concurrently
				// Ideally order should be preserved or receipts supported (simple broker)
				Thread.sleep(500);
			}
			catch (InterruptedException ex) {
				logger.error(ex);
			}
			session.send(this.topic, this.payload);
		}

		public boolean awaitForMessageCount(int expected, long millisToWait) throws InterruptedException {
			if (logger.isDebugEnabled()) {
				logger.debug("Awaiting for message count: " + expected);
			}
			long startTime = System.currentTimeMillis();
			while (this.received.size() < expected) {
				Thread.sleep(500);
				if ((System.currentTimeMillis() - startTime) > millisToWait) {
					return false;
				}
			}
			return true;
		}

		@Override
		public void handleException(StompSession session, StompCommand command,
				StompHeaders headers, byte[] payload, Throwable ex) {

			logger.error(command + " " + headers, ex);
		}

		@Override
		public void handleFrame(StompHeaders headers, Object payload) {
			logger.error("STOMP error frame " + headers + " payload=" + payload);
		}

		@Override
		public void handleTransportError(StompSession session, Throwable exception) {
			logger.error(exception);
		}
	}

}