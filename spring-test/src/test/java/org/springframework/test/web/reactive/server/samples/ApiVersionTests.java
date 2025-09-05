/*
 * Copyright 2002-present the original author or authors.
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
import org.springframework.web.client.ApiVersionInserter;
import org.springframework.web.reactive.config.ApiVersionConfigurer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link WebTestClient} tests for sending API versions.
 *
 * @author Rossen Stoyanchev
 */
public class ApiVersionTests {

	@Test
	void header() {
		String header = "X-API-Version";

		Map<String, String> result = performRequest(
				configurer -> configurer.useRequestHeader(header),
				ApiVersionInserter.useHeader(header));

		assertThat(result.get(header)).isEqualTo("1.2");
	}

	@Test
	void queryParam() {
		String param = "api-version";

		Map<String, String> result = performRequest(
				configurer -> configurer.useQueryParam(param),
				ApiVersionInserter.useQueryParam(param));

		assertThat(result.get("query")).isEqualTo(param + "=1.2");
	}

	@Test
	void pathSegment() {
		Map<String, String> result = performRequest(
				configurer -> configurer.usePathSegment(0),
				ApiVersionInserter.usePathSegment(0));

		assertThat(result.get("path")).isEqualTo("/1.2/path");
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> performRequest(
			Consumer<ApiVersionConfigurer> versionConfigurer, ApiVersionInserter inserter) {

		WebTestClient client = WebTestClient.bindToController(new TestController())
				.apiVersioning(versionConfigurer)
				.configureClient()
				.baseUrl("/path")
				.apiVersionInserter(inserter)
				.build();

		return client.get()
				.apiVersion(1.2)
				.exchange()
				.returnResult(Map.class)
				.getResponseBody()
				.blockFirst();
	}


	@RestController
	static class TestController {

		private static final String HEADER = "X-API-Version";

		@GetMapping(path = "/**", version = "1.2")
		Map<String, String> handle(ServerHttpRequest request) {
			URI uri = request.getURI();
			String query = uri.getQuery();
			String versionHeader = request.getHeaders().getFirst(HEADER);
			return Map.of("path", uri.getRawPath(),
					"query", (query != null ? query : ""),
					HEADER, (versionHeader != null ? versionHeader : ""));
		}
	}

}
