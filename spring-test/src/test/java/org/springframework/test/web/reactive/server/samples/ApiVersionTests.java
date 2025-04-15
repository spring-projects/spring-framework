/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.test.web.reactive.server.samples;

import java.net.URI;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.DefaultApiVersionInserter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link WebTestClient} tests for sending API versions.
 *
 * @author Rossen Stoyanchev
 */
public class ApiVersionTests {

	private static final String HEADER_NAME = "X-API-Version";


	@Test
	void header() {
		Map<String, String> result = performRequest(builder -> builder.fromHeader("X-API-Version"));
		assertThat(result.get(HEADER_NAME)).isEqualTo("1.2");
	}

	@Test
	void queryParam() {
		Map<String, String> result = performRequest(builder -> builder.fromQueryParam("api-version"));
		assertThat(result.get("query")).isEqualTo("api-version=1.2");
	}

	@Test
	void pathSegment() {
		Map<String, String> result = performRequest(builder -> builder.fromPathSegment(0));
		assertThat(result.get("path")).isEqualTo("/1.2/path");
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> performRequest(Consumer<DefaultApiVersionInserter.Builder> consumer) {
		DefaultApiVersionInserter.Builder builder = DefaultApiVersionInserter.builder();
		consumer.accept(builder);
		return WebTestClient.bindToController(new TestController())
				.configureClient()
				.baseUrl("/path")
				.apiVersionInserter(builder.build())
				.build()
				.get()
				.apiVersion(1.2)
				.exchange()
				.returnResult(Map.class)
				.getResponseBody()
				.blockFirst();
	}


	@RestController
	static class TestController {

		@GetMapping("/**")
		Map<String, String> handle(ServerHttpRequest request) {
			URI uri = request.getURI();
			String query = uri.getQuery();
			String header = request.getHeaders().getFirst(HEADER_NAME);
			return Map.of("path", uri.getRawPath(),
					"query", (query != null ? query : ""),
					HEADER_NAME, (header != null ? header : ""));
		}
	}

}
