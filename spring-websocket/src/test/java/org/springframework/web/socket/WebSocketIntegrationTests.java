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

package org.springframework.web.socket;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.adapter.TextWebSocketHandlerAdapter;
import org.springframework.web.socket.adapter.WebSocketHandlerAdapter;
import org.springframework.web.socket.client.endpoint.StandardWebSocketClient;
import org.springframework.web.socket.client.jetty.JettyWebSocketClient;
import org.springframework.web.socket.server.DefaultHandshakeHandler;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.RequestUpgradeStrategy;
import org.springframework.web.socket.server.config.EnableWebSocket;
import org.springframework.web.socket.server.config.WebSocketConfigurer;
import org.springframework.web.socket.server.config.WebSocketHandlerRegistry;
import org.springframework.web.socket.support.WebSocketExtension;
import org.springframework.web.socket.support.WebSocketHttpHeaders;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

/**
 * Client and server-side WebSocket integration tests.
 *
 * @author Rossen Stoyanchev
 */
@RunWith(Parameterized.class)
public class WebSocketIntegrationTests extends  AbstractWebSocketIntegrationTests {

	@Parameterized.Parameters
	public static Iterable<Object[]> arguments() {
		return Arrays.asList(new Object[][]{
				{new JettyWebSocketTestServer(), new JettyWebSocketClient()},
				{new TomcatWebSocketTestServer(), new StandardWebSocketClient()}
		});
	};


	@Override
	protected Class<?>[] getAnnotatedConfigClasses() {
		return new Class<?>[] { TestWebSocketConfigurer.class };
	}

	@Test
	public void subProtocolNegotiation() throws Exception {

		WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
		headers.setSecWebSocketProtocol("foo");

		WebSocketSession session = this.webSocketClient.doHandshake(
				new WebSocketHandlerAdapter(), headers, new URI(getWsBaseUrl() + "/ws")).get();

		assertEquals("foo", session.getAcceptedProtocol());
	}


	@Configuration
	@EnableWebSocket
	static class TestWebSocketConfigurer implements WebSocketConfigurer {

		@Autowired
		private DefaultHandshakeHandler handshakeHandler; // can't rely on classpath for server detection

		@Override
		public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
			this.handshakeHandler.setSupportedProtocols("foo", "bar", "baz");
			registry.addHandler(serverHandler(), "/ws").setHandshakeHandler(this.handshakeHandler);
		}

		@Bean
		public TestServerWebSocketHandler serverHandler() {
			return new TestServerWebSocketHandler();
		}
	}

	private static class TestClientWebSocketHandler extends TextWebSocketHandlerAdapter {

		private final TextMessage[] messagesToSend;

		private final int expected;

		private final List<TextMessage> actual = new CopyOnWriteArrayList<TextMessage>();

		private final CountDownLatch latch;


		public TestClientWebSocketHandler(int expectedNumberOfMessages, TextMessage... messagesToSend) {
			this.messagesToSend = messagesToSend;
			this.expected = expectedNumberOfMessages;
			this.latch = new CountDownLatch(this.expected);
		}

		@Override
		public void afterConnectionEstablished(WebSocketSession session) throws Exception {
			for (TextMessage message : this.messagesToSend) {
				session.sendMessage(message);
			}
		}

		@Override
		protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
			this.actual.add(message);
			this.latch.countDown();
		}
	}

	private static class TestServerWebSocketHandler extends TextWebSocketHandlerAdapter {

	}

	private static class TestRequestUpgradeStrategy implements RequestUpgradeStrategy {

		private final RequestUpgradeStrategy delegate;

		private List<WebSocketExtension> extensions= new ArrayList<WebSocketExtension>();


		private TestRequestUpgradeStrategy(RequestUpgradeStrategy delegate, String... supportedExtensions) {
			this.delegate = delegate;
			for (String name : supportedExtensions) {
				this.extensions.add(new WebSocketExtension(name));
			}
		}

		@Override
		public String[] getSupportedVersions() {
			return this.delegate.getSupportedVersions();
		}

		@Override
		public List<WebSocketExtension> getSupportedExtensions(ServerHttpRequest request) {
			return this.extensions;
		}

		@Override
		public void upgrade(ServerHttpRequest request, ServerHttpResponse response,
				String selectedProtocol, List<WebSocketExtension> selectedExtensions,
				WebSocketHandler wsHandler, Map<String, Object> attributes) throws HandshakeFailureException {

			this.delegate.upgrade(request, response, selectedProtocol, selectedExtensions, wsHandler, attributes);
		}
	}

}
