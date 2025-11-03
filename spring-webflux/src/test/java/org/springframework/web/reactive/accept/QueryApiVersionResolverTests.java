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

package org.springframework.web.reactive.accept;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link QueryApiVersionResolver}.
 *
 * @author Rossen Stoyanchev
 */
class QueryApiVersionResolverTests {

	private final String queryParamName = "api-version";

	private final QueryApiVersionResolver resolver = new QueryApiVersionResolver(queryParamName);


	@Test
	void resolve() {
		ServerWebExchange exchange = initExchange("q=foo&" + queryParamName + "=1.2");
		String version = resolver.resolveVersion(exchange);
		assertThat(version).isEqualTo("1.2");
	}

	@Test
	void noQueryString() {
		ServerWebExchange exchange = initExchange(null);
		String version = resolver.resolveVersion(exchange);
		assertThat(version).isNull();
	}

	@Test
	void noQueryParam() {
		ServerWebExchange exchange = initExchange("q=foo");
		String version = resolver.resolveVersion(exchange);
		assertThat(version).isNull();
	}

	private static ServerWebExchange initExchange(@Nullable String queryString) {
		return MockServerWebExchange.from(MockServerHttpRequest.get("/path?" + queryString));
	}

}
