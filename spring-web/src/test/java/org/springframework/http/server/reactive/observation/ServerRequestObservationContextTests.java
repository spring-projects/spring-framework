/*
 * Copyright 2025-present the original author or authors.
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
