/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
