/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.socket.server.support;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;
import org.springframework.web.testfixture.server.MockWebSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HandshakeWebSocketService}.
 *
 * @author Rossen Stoyanchev
 */
class HandshakeWebSocketServiceTests {

	@Test
	void sessionAttributePredicate() {
		MockWebSession session = new MockWebSession();
		session.getAttributes().put("a1", "v1");
		session.getAttributes().put("a2", "v2");
		session.getAttributes().put("a3", "v3");
		session.getAttributes().put("a4", "v4");
		session.getAttributes().put("a5", "v5");

		MockServerHttpRequest request = initHandshakeRequest();
		MockServerWebExchange exchange = MockServerWebExchange.builder(request).session(session).build();

		TestRequestUpgradeStrategy upgradeStrategy = new TestRequestUpgradeStrategy();
		HandshakeWebSocketService service = new HandshakeWebSocketService(upgradeStrategy);
		service.setSessionAttributePredicate(name -> Arrays.asList("a1", "a3", "a5").contains(name));

		service.handleRequest(exchange, mock()).block();

		HandshakeInfo info = upgradeStrategy.handshakeInfo;
		assertThat(info).isNotNull();

		Map<String, Object> attributes = info.getAttributes();
		assertThat(attributes)
				.hasSize(3)
				.containsEntry("a1", "v1")
				.containsEntry("a3", "v3")
				.containsEntry("a5", "v5");
	}

	private MockServerHttpRequest initHandshakeRequest() {
		return MockServerHttpRequest.get("/")
					.header("upgrade", "websocket")
					.header("connection", "upgrade")
					.header("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ==")
					.header("Sec-WebSocket-Version", "13")
					.build();
	}


	private static class TestRequestUpgradeStrategy implements RequestUpgradeStrategy {

		HandshakeInfo handshakeInfo;

		@Override
		public Mono<Void> upgrade(ServerWebExchange exchange, WebSocketHandler webSocketHandler,
				@Nullable String subProtocol, Supplier<HandshakeInfo> handshakeInfoFactory) {

			this.handshakeInfo = handshakeInfoFactory.get();
			return Mono.empty();
		}
	}

}
