/*
 * Copyright 2002-2019 the original author or authors.
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
package org.springframework.web.server.session;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests using {@link HeaderWebSessionIdResolver}.
 *
 * @author Greg Turnquist
 * @author Rob Winch
 */
public class HeaderWebSessionIdResolverTests {
	private HeaderWebSessionIdResolver idResolver;

	private ServerWebExchange exchange;

	@BeforeEach
	public void setUp() {
		this.idResolver = new HeaderWebSessionIdResolver();
		this.exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path"));
	}

	@Test
	public void expireWhenValidThenSetsEmptyHeader() {
		this.idResolver.expireSession(this.exchange);

		assertThat(this.exchange.getResponse().getHeaders().get(HeaderWebSessionIdResolver.DEFAULT_HEADER_NAME)).isEqualTo(Arrays.asList(""));
	}

	@Test
	public void expireWhenMultipleInvocationThenSetsSingleEmptyHeader() {
		this.idResolver.expireSession(this.exchange);

		this.idResolver.expireSession(this.exchange);

		assertThat(this.exchange.getResponse().getHeaders().get(HeaderWebSessionIdResolver.DEFAULT_HEADER_NAME)).isEqualTo(Arrays.asList(""));
	}

	@Test
	public void expireWhenAfterSetSessionIdThenSetsEmptyHeader() {
		this.idResolver.setSessionId(this.exchange, "123");

		this.idResolver.expireSession(this.exchange);

		assertThat(this.exchange.getResponse().getHeaders().get(HeaderWebSessionIdResolver.DEFAULT_HEADER_NAME)).isEqualTo(Arrays.asList(""));
	}

	@Test
	public void setSessionIdWhenValidThenSetsHeader() {
		String id = "123";

		this.idResolver.setSessionId(this.exchange, id);

		assertThat(this.exchange.getResponse().getHeaders().get(HeaderWebSessionIdResolver.DEFAULT_HEADER_NAME)).isEqualTo(Arrays.asList(id));
	}

	@Test
	public void setSessionIdWhenMultipleThenSetsSingleHeader() {
		String id = "123";
		this.idResolver.setSessionId(this.exchange, "overriddenByNextInvocation");

		this.idResolver.setSessionId(this.exchange, id);

		assertThat(this.exchange.getResponse().getHeaders().get(HeaderWebSessionIdResolver.DEFAULT_HEADER_NAME)).isEqualTo(Arrays.asList(id));
	}

	@Test
	public void setSessionIdWhenCustomHeaderNameThenSetsHeader() {
		String headerName = "x-auth";
		String id = "123";
		this.idResolver.setHeaderName(headerName);

		this.idResolver.setSessionId(this.exchange, id);

		assertThat(this.exchange.getResponse().getHeaders().get(headerName)).isEqualTo(Arrays.asList(id));
	}

	@Test
	public void setSessionIdWhenNullIdThenIllegalArgumentException() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				this.idResolver.setSessionId(this.exchange, (String) null));
	}

	@Test
	public void resolveSessionIdsWhenNoIdsThenEmpty() {
		List<String> ids = this.idResolver.resolveSessionIds(this.exchange);

		assertThat(ids.isEmpty()).isTrue();
	}

	@Test
	public void resolveSessionIdsWhenIdThenIdFound() {
		String id = "123";
		this.exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path")
				.header(HeaderWebSessionIdResolver.DEFAULT_HEADER_NAME, id));

		List<String> ids = this.idResolver.resolveSessionIds(this.exchange);

		assertThat(ids).isEqualTo(Arrays.asList(id));
	}

	@Test
	public void resolveSessionIdsWhenMultipleIdsThenIdsFound() {
		String id1 = "123";
		String id2 = "abc";
		this.exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/path")
						.header(HeaderWebSessionIdResolver.DEFAULT_HEADER_NAME, id1, id2));

		List<String> ids = this.idResolver.resolveSessionIds(this.exchange);

		assertThat(ids).isEqualTo(Arrays.asList(id1, id2));
	}
}
