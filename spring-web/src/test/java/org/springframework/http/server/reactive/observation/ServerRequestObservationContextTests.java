/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server.reactive.observation;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServerRequestObservationContext}.
 */
class ServerRequestObservationContextTests {


	@Test
	void shouldGetMultipleHeaderValues() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/")
				.header("test", "spring", "framework").build();
		MockServerWebExchange exchange = MockServerWebExchange.builder(request).build();

		ServerRequestObservationContext context = new ServerRequestObservationContext(request, exchange.getResponse(), Map.of());
		assertThat(context.getGetter().getAll(request, "test")).contains("spring", "framework");
	}

}
