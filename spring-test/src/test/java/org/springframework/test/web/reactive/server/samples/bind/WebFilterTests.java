/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.web.reactive.server.samples.bind;

import java.nio.charset.StandardCharsets;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.WebFilter;

/**
 * Tests for a {@link WebFilter}.
 * @author Rossen Stoyanchev
 */
public class WebFilterTests {

	@Test
	public void testWebFilter() throws Exception {

		WebFilter filter = (exchange, chain) -> {
			DataBuffer buffer = new DefaultDataBufferFactory().allocateBuffer();
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
