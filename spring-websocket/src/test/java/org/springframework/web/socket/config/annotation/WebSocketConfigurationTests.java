/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.config.annotation;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.TestInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.AbstractWebSocketIntegrationTests;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.WebSocketTestServer;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for WebSocket Java server-side configuration.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class WebSocketConfigurationTests extends AbstractWebSocketIntegrationTests {

	@Override
	protected Class<?>[] getAnnotatedConfigClasses() {
		return new Class<?>[] {TestConfig.class};
	}


	@ParameterizedWebSocketTest
	void registerWebSocketHandler(
			WebSocketTestServer server, WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {

		super.setup(server, webSocketClient, testInfo);

		WebSocketSession session = this.webSocketClient.execute(
				new AbstractWebSocketHandler() {}, getWsBaseUrl() + "/ws").get();

		TestHandler serverHandler = this.wac.getBean(TestHandler.class);
		assertThat(serverHandler.connectLatch.await(2, TimeUnit.SECONDS)).isTrue();

		session.close();
	}

	@ParameterizedWebSocketTest
	void registerWebSocketHandlerWithSockJS(
			WebSocketTestServer server, WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {

		super.setup(server, webSocketClient, testInfo);

		WebSocketSession session = this.webSocketClient.execute(
				new AbstractWebSocketHandler() {}, getWsBaseUrl() + "/sockjs/websocket").get();

		TestHandler serverHandler = this.wac.getBean(TestHandler.class);
		assertThat(serverHandler.connectLatch.await(2, TimeUnit.SECONDS)).isTrue();

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
		TestHandler serverHandler() {
			return new TestHandler();
		}
	}


	private static class TestHandler extends AbstractWebSocketHandler {

		private final CountDownLatch connectLatch = new CountDownLatch(1);

		@Override
		public void afterConnectionEstablished(WebSocketSession session) {
			this.connectLatch.countDown();
		}
	}

}
