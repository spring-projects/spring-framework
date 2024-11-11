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

package org.springframework.web.reactive.socket.server.support;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.lang.Nullable;
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
