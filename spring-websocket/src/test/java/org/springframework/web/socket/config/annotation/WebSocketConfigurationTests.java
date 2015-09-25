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

package org.springframework.web.socket.config.annotation;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.AbstractWebSocketIntegrationTests;
import org.springframework.web.socket.JettyWebSocketTestServer;
import org.springframework.web.socket.TomcatWebSocketTestServer;
import org.springframework.web.socket.UndertowTestServer;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.jetty.JettyWebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;

import static org.junit.Assert.*;

/**
 * Integration tests for WebSocket Java server-side configuration.
 *
 * @author Rossen Stoyanchev
 */
@RunWith(Parameterized.class)
public class WebSocketConfigurationTests extends AbstractWebSocketIntegrationTests {

	@Parameters(name = "server [{0}], client [{1}]")
	public static Iterable<Object[]> arguments() {
		return Arrays.asList(new Object[][] {
				{new JettyWebSocketTestServer(), new JettyWebSocketClient()},
				{new TomcatWebSocketTestServer(), new StandardWebSocketClient()},
				{new UndertowTestServer(), new StandardWebSocketClient()}
		});
	}


	@Override
	protected Class<?>[] getAnnotatedConfigClasses() {
		return new Class<?>[] { TestConfig.class };
	}

	@Test
	public void registerWebSocketHandler() throws Exception {
		WebSocketSession session = this.webSocketClient.doHandshake(
				new AbstractWebSocketHandler() {}, getWsBaseUrl() + "/ws").get();

		TestHandler serverHandler = this.wac.getBean(TestHandler.class);
		assertTrue(serverHandler.connectLatch.await(2, TimeUnit.SECONDS));

		session.close();
	}

	@Test
	public void registerWebSocketHandlerWithSockJS() throws Exception {
		WebSocketSession session = this.webSocketClient.doHandshake(
				new AbstractWebSocketHandler() {}, getWsBaseUrl() + "/sockjs/websocket").get();

		TestHandler serverHandler = this.wac.getBean(TestHandler.class);
		assertTrue(serverHandler.connectLatch.await(2, TimeUnit.SECONDS));

		session.close();
	}


	@Configuration
	@EnableWebSocket
	static class TestConfig implements WebSocketConfigurer {

		@Autowired
		private HandshakeHandler handshakeHandler; // can't rely on classpath for server detection

		@Override
		public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
			registry.addHandler(serverHandler(), "/ws")
				.setHandshakeHandler(this.handshakeHandler);
			registry.addHandler(serverHandler(), "/sockjs").withSockJS()
				.setTransportHandlerOverrides(new WebSocketTransportHandler(this.handshakeHandler));
		}

		@Bean
		public TestHandler serverHandler() {
			return new TestHandler();
		}
	}

	private static class TestHandler extends AbstractWebSocketHandler {

		private CountDownLatch connectLatch = new CountDownLatch(1);

		@Override
		public void afterConnectionEstablished(WebSocketSession session) throws Exception {
			this.connectLatch.countDown();
		}
	}

}
