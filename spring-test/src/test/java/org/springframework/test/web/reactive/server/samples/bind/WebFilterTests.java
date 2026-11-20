/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.reactive.server.samples.bind;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.WebFilter;

/**
 * Tests for a {@link WebFilter}.
 * @author Rossen Stoyanchev
 */
class WebFilterTests {

	@Test
	void webFilter() {

		WebFilter filter = (exchange, chain) -> {
			DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.allocateBuffer(256);
			buffer.write("It works!".getBytes(StandardCharsets.UTF_8));
			return exchange.getResponse().writeWith(Mono.just(buffer));
		};

		WebTestClient client = WebTestClient.bindToWebHandler(exchange -> Mono.empty())
				.webFilter(filter)
				.build();

		client.get().uri("/")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("It works!");
	}

}
