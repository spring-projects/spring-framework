/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
