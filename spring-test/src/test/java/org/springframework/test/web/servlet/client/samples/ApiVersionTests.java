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

package org.springframework.test.web.servlet.client.samples;


import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.accept.ApiVersionResolver;
import org.springframework.web.accept.DefaultApiVersionStrategy;
import org.springframework.web.accept.PathApiVersionResolver;
import org.springframework.web.accept.SemanticApiVersionParser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ApiVersionInserter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RestTestClient} tests for sending API versions.
 *
 * @author Rossen Stoyanchev
 */
public class ApiVersionTests {

	@Test
	void header() {
		String header = "X-API-Version";

		Map<String, String> result = performRequest(
				request -> request.getHeader(header), ApiVersionInserter.useHeader(header));

		assertThat(result.get(header)).isEqualTo("1.2");
	}

	@Test
	void queryParam() {
		String param = "api-version";

		Map<String, String> result = performRequest(
				request -> request.getParameter(param), ApiVersionInserter.useQueryParam(param));

		assertThat(result.get("query")).isEqualTo(param + "=1.2");
	}

	@Test
	void pathSegment() {
		Map<String, String> result = performRequest(
				new PathApiVersionResolver(0), ApiVersionInserter.usePathSegment(0));

		assertThat(result.get("path")).isEqualTo("/1.2/path");
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> performRequest(
			ApiVersionResolver versionResolver, ApiVersionInserter inserter) {

		DefaultApiVersionStrategy versionStrategy = new DefaultApiVersionStrategy(
				List.of(versionResolver), new SemanticApiVersionParser(),
				true, null, true, null, null);

		RestTestClient client = RestTestClient.bindToController(new TestController())
				.configureServer(mockMvcBuilder -> mockMvcBuilder.setApiVersionStrategy(versionStrategy))
				.baseUrl("/path")
				.apiVersionInserter(inserter)
				.build();

		return client.get()
				.accept(MediaType.APPLICATION_JSON)
				.apiVersion(1.2)
				.exchange()
				.returnResult(Map.class)
				.getResponseBody();
	}


	@RestController
	private static class TestController {

		private static final String HEADER = "X-API-Version";

		@GetMapping(path = "/**", version = "1.2")
		Map<String, String> handle(HttpServletRequest request) {
			String query = request.getQueryString();
			String versionHeader = request.getHeader(HEADER);
			return Map.of("path", request.getRequestURI(),
					"query", (query != null ? query : ""),
					HEADER, (versionHeader != null ? versionHeader : ""));
		}
	}
}
