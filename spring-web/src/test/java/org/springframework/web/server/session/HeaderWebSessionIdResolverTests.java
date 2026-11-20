/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.server.session;

import org.junit.jupiter.api.Test;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests using {@link HeaderWebSessionIdResolver}.
 *
 * @author Greg Turnquist
 * @author Rob Winch
 */
class HeaderWebSessionIdResolverTests {

	private final HeaderWebSessionIdResolver idResolver = new HeaderWebSessionIdResolver();

	private ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path"));


	@Test
	void expireWhenValidThenSetsEmptyHeader() {
		this.idResolver.expireSession(this.exchange);

		assertThat(this.exchange.getResponse().getHeaders().get(HeaderWebSessionIdResolver.DEFAULT_HEADER_NAME)).containsExactly("");
	}

	@Test
	void expireWhenMultipleInvocationThenSetsSingleEmptyHeader() {
		this.idResolver.expireSession(this.exchange);
		this.idResolver.expireSession(this.exchange);

		assertThat(this.exchange.getResponse().getHeaders().get(HeaderWebSessionIdResolver.DEFAULT_HEADER_NAME)).containsExactly("");
	}

	@Test
	void expireWhenAfterSetSessionIdThenSetsEmptyHeader() {
		this.idResolver.setSessionId(this.exchange, "123");
		this.idResolver.expireSession(this.exchange);

		assertThat(this.exchange.getResponse().getHeaders().get(HeaderWebSessionIdResolver.DEFAULT_HEADER_NAME)).containsExactly("");
	}

	@Test
	void setSessionIdWhenValidThenSetsHeader() {
		String id = "123";
		this.idResolver.setSessionId(this.exchange, id);

		assertThat(this.exchange.getResponse().getHeaders().get(HeaderWebSessionIdResolver.DEFAULT_HEADER_NAME)).containsExactly(id);
	}

	@Test
	void setSessionIdWhenMultipleThenSetsSingleHeader() {
		String id = "123";
		this.idResolver.setSessionId(this.exchange, "overriddenByNextInvocation");
		this.idResolver.setSessionId(this.exchange, id);

		assertThat(this.exchange.getResponse().getHeaders().get(HeaderWebSessionIdResolver.DEFAULT_HEADER_NAME)).containsExactly(id);
	}

	@Test
	void setSessionIdWhenCustomHeaderNameThenSetsHeader() {
		String headerName = "x-auth";
		String id = "123";
		this.idResolver.setHeaderName(headerName);
		this.idResolver.setSessionId(this.exchange, id);

		assertThat(this.exchange.getResponse().getHeaders().get(headerName)).containsExactly(id);
	}

	@Test
	void setSessionIdWhenNullIdThenIllegalArgumentException() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				this.idResolver.setSessionId(this.exchange, null));
	}

	@Test
	void resolveSessionIdsWhenNoIdsThenEmpty() {
		assertThat(this.idResolver.resolveSessionIds(this.exchange)).isEmpty();
	}

	@Test
	void resolveSessionIdsWhenIdThenIdFound() {
		String id = "123";
		this.exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path")
				.header(HeaderWebSessionIdResolver.DEFAULT_HEADER_NAME, id));

		assertThat(this.idResolver.resolveSessionIds(this.exchange)).containsExactly(id);
	}

	@Test
	void resolveSessionIdsWhenMultipleIdsThenIdsFound() {
		String id1 = "123";
		String id2 = "abc";
		this.exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/path")
						.header(HeaderWebSessionIdResolver.DEFAULT_HEADER_NAME, id1, id2));

		assertThat(this.idResolver.resolveSessionIds(this.exchange)).containsExactly(id1, id2);
	}

}
