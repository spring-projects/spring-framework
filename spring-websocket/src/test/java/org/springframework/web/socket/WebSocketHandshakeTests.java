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

package org.springframework.web.socket;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.TestInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Client and server-side WebSocket integration tests.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class WebSocketHandshakeTests extends AbstractWebSocketIntegrationTests {

	@Override
	protected Class<?>[] getAnnotatedConfigClasses() {
		return new Class<?>[] {TestConfig.class};
	}


	@ParameterizedWebSocketTest
	void subProtocolNegotiation(
			WebSocketTestServer server, WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {

		super.setup(server, webSocketClient, testInfo);

		WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
		headers.setSecWebSocketProtocol("foo");
		URI url = URI.create(getWsBaseUrl() + "/ws");
		WebSocketSession session = this.webSocketClient.execute(new TextWebSocketHandler(), headers, url).get();
		assertThat(session.getAcceptedProtocol()).isEqualTo("foo");
		session.close();
	}

	@ParameterizedWebSocketTest  // SPR-12727
	void unsolicitedPongWithEmptyPayload(
			WebSocketTestServer server, WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {

		super.setup(server, webSocketClient, testInfo);

		String url = getWsBaseUrl() + "/ws";
		WebSocketSession session = this.webSocketClient.execute(new AbstractWebSocketHandler() {}, url).get();

		TestWebSocketHandler serverHandler = this.wac.getBean(TestWebSocketHandler.class);
		serverHandler.setWaitMessageCount(1);

		session.sendMessage(new PongMessage());

		serverHandler.await();
		assertThat(serverHandler.getTransportError()).isNull();
		assertThat(serverHandler.getReceivedMessages()).hasSize(1);
		assertThat(serverHandler.getReceivedMessages().get(0).getClass()).isEqualTo(PongMessage.class);
	}


	@Configuration
	@EnableWebSocket
	static class TestConfig implements WebSocketConfigurer {

		@Autowired
		private DefaultHandshakeHandler handshakeHandler;  // can't rely on classpath for server detection

		@Override
		public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
			this.handshakeHandler.setSupportedProtocols("foo", "bar", "baz");
			registry.addHandler(handler(), "/ws").setHandshakeHandler(this.handshakeHandler);
		}

		@Bean
		TestWebSocketHandler handler() {
			return new TestWebSocketHandler();
		}

	}

	@SuppressWarnings("rawtypes")
	private static class TestWebSocketHandler extends AbstractWebSocketHandler {

		private List<WebSocketMessage> receivedMessages = new ArrayList<>();

		private int waitMessageCount;

		private final CountDownLatch latch = new CountDownLatch(1);

		private Throwable transportError;

		public void setWaitMessageCount(int waitMessageCount) {
			this.waitMessageCount = waitMessageCount;
		}

		public List<WebSocketMessage> getReceivedMessages() {
			return this.receivedMessages;
		}

		public Throwable getTransportError() {
			return this.transportError;
		}

		@Override
		public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
			this.receivedMessages.add(message);
			if (this.receivedMessages.size() >= this.waitMessageCount) {
				this.latch.countDown();
			}
		}

		@Override
		public void handleTransportError(WebSocketSession session, Throwable exception) {
			this.transportError = exception;
			this.latch.countDown();
		}

		public void await() throws InterruptedException {
			this.latch.await(5, TimeUnit.SECONDS);
		}
	}

}
