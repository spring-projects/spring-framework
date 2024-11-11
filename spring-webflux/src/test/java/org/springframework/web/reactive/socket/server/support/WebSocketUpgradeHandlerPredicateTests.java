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

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for and related to the use of {@link WebSocketUpgradeHandlerPredicate}.
 *
 * @author Rossen Stoyanchev
 */
class WebSocketUpgradeHandlerPredicateTests {

	private final WebSocketUpgradeHandlerPredicate predicate = new WebSocketUpgradeHandlerPredicate();

	private final WebSocketHandler webSocketHandler = mock();

	ServerWebExchange httpGetExchange =
			MockServerWebExchange.from(MockServerHttpRequest.get("/path"));

	ServerWebExchange httpPostExchange =
			MockServerWebExchange.from(MockServerHttpRequest.post("/path"));

	ServerWebExchange webSocketExchange =
			MockServerWebExchange.from(MockServerHttpRequest.get("/path").header(HttpHeaders.UPGRADE, "websocket"));


	@Test
	void match() {
		assertThat(this.predicate.test(this.webSocketHandler, this.webSocketExchange))
				.as("Should match WebSocketHandler to WebSocket upgrade")
				.isTrue();

		assertThat(this.predicate.test(new Object(), this.httpGetExchange))
				.as("Should match non-WebSocketHandler to any request")
				.isTrue();
	}

	@Test
	void noMatch() {
		assertThat(this.predicate.test(this.webSocketHandler, this.httpGetExchange))
				.as("Should not match WebSocket handler to HTTP GET")
				.isFalse();

		assertThat(this.predicate.test(this.webSocketHandler, this.httpPostExchange))
				.as("Should not match WebSocket handler to HTTP POST")
				.isFalse();
	}

	@Test
	void simpleUrlHandlerMapping() {
		SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
		mapping.setUrlMap(Collections.singletonMap("/path", this.webSocketHandler));
		mapping.setApplicationContext(new StaticWebApplicationContext());

		Object actual = mapping.getHandler(httpGetExchange).block();
		assertThat(actual).as("Should match HTTP GET by URL path").isSameAs(this.webSocketHandler);

		mapping.setHandlerPredicate(new WebSocketUpgradeHandlerPredicate());

		actual = mapping.getHandler(this.httpGetExchange).block();
		assertThat(actual).as("Should not match if not a WebSocket upgrade").isNull();

		actual = mapping.getHandler(this.httpPostExchange).block();
		assertThat(actual).as("Should not match if not a WebSocket upgrade").isNull();

		actual = mapping.getHandler(this.webSocketExchange).block();
		assertThat(actual).as("Should match WebSocket upgrade").isSameAs(this.webSocketHandler);
	}

}
