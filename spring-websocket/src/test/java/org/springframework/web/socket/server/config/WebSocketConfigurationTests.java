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

package org.springframework.web.socket.server.config;

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
import org.springframework.web.socket.JettyTestServer;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.WebSocketHandlerAdapter;
import org.springframework.web.socket.client.jetty.JettyWebSocketClient;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;

import static org.junit.Assert.*;


/**
 * Test fixture for WebSocket Java config support.
 *
 * @author Rossen Stoyanchev
 */
@RunWith(Parameterized.class)
public class WebSocketConfigurationTests extends AbstractWebSocketIntegrationTests {

	@Parameters
	public static Iterable<Object[]> arguments() {
		return Arrays.asList(new Object[][] {
				{ new JettyTestServer(), new JettyWebSocketClient()} });
	};


	@Override
	protected Class<?>[] getAnnotatedConfigClasses() {
		return new Class<?>[] { TestWebSocketConfigurer.class };
	}

	@Test
	public void registerWebSocketHandler() throws Exception {

		this.webSocketClient.doHandshake(new WebSocketHandlerAdapter(), getWsBaseUrl() + "/ws");

		TestWebSocketHandler serverHandler = this.wac.getBean(TestWebSocketHandler.class);
		assertTrue(serverHandler.latch.await(2, TimeUnit.SECONDS));
	}

	@Test
	public void registerWebSocketHandlerWithSockJS() throws Exception {

		this.webSocketClient.doHandshake(new WebSocketHandlerAdapter(), getWsBaseUrl() + "/sockjs/websocket");

		TestWebSocketHandler serverHandler = this.wac.getBean(TestWebSocketHandler.class);
		assertTrue(serverHandler.latch.await(2, TimeUnit.SECONDS));
	}


	@Configuration
	@EnableWebSocket
	static class TestWebSocketConfigurer implements WebSocketConfigurer {

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
		public TestWebSocketHandler serverHandler() {
			return new TestWebSocketHandler();
		}
	}

	private static class TestWebSocketHandler extends WebSocketHandlerAdapter {

		private CountDownLatch latch = new CountDownLatch(1);

		@Override
		public void afterConnectionEstablished(WebSocketSession session) throws Exception {
			this.latch.countDown();
		}
	}

}
