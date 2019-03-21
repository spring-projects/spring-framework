/*
 * Copyright 2002-2018 the original author or authors.
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

import org.hamcrest.Matchers;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.lang.Nullable;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.mock.web.test.server.MockWebSession;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link HandshakeWebSocketService}.
 *
 * @author Rossen Stoyanchev
 */
public class HandshakeWebSocketServiceTests {

	@Test
	public void sessionAttributePredicate() {
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

		service.handleRequest(exchange, mock(WebSocketHandler.class)).block();

		HandshakeInfo info = upgradeStrategy.handshakeInfo;
		assertNotNull(info);

		Map<String, Object> attributes = info.getAttributes();
		assertEquals(3, attributes.size());
		assertThat(attributes, Matchers.hasEntry("a1", "v1"));
		assertThat(attributes, Matchers.hasEntry("a3", "v3"));
		assertThat(attributes, Matchers.hasEntry("a5", "v5"));
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
				@Nullable  String subProtocol, Supplier<HandshakeInfo> handshakeInfoFactory) {

			this.handshakeInfo = handshakeInfoFactory.get();
			return Mono.empty();
		}
	}

}
