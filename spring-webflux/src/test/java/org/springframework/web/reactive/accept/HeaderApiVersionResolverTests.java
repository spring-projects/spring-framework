/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.accept;

import org.junit.jupiter.api.Test;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.get;

/**
 * Unit tests for {@link HeaderApiVersionResolver}.
 *
 * @author Rossen Stoyanchev
 */
class HeaderApiVersionResolverTests {

	private final String headerName = "Api-Version";

	private final HeaderApiVersionResolver resolver = new HeaderApiVersionResolver(headerName);


	@Test
	void resolve() {
		String version = "1.2";
		ServerWebExchange exchange = MockServerWebExchange.from(get("/").header(headerName, version));
		String actual = resolver.resolveVersion(exchange);
		assertThat(actual).isEqualTo(version);
	}

	@Test
	void noHeader() {
		ServerWebExchange exchange = MockServerWebExchange.from(get("/"));
		String version = resolver.resolveVersion(exchange);
		assertThat(version).isNull();
	}

}
