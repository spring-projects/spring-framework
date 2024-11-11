/*
 * Copyright 2002-2024 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.http.ResponseCookie;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CookieWebSessionIdResolver}.
 *
 * @author Rossen Stoyanchev
 */
class CookieWebSessionIdResolverTests {

	private final CookieWebSessionIdResolver resolver = new CookieWebSessionIdResolver();

	private final ServerWebExchange exchange =
			MockServerWebExchange.from(MockServerHttpRequest.get("https://example.org/path"));


	@Test
	void setSessionId() {
		this.resolver.setSessionId(this.exchange, "123");
		assertCookieValue("SESSION=123; Path=/; Secure; HttpOnly; SameSite=Lax");
	}

	@Test
	void cookieInitializer() {
		this.resolver.addCookieInitializer(builder -> builder.domain("example.org"));
		this.resolver.addCookieInitializer(builder -> builder.sameSite("Strict"));
		this.resolver.addCookieInitializer(builder -> builder.secure(false));
		this.resolver.setSessionId(this.exchange, "123");

		assertCookieValue("SESSION=123; Path=/; Domain=example.org; HttpOnly; SameSite=Strict");
	}

	@Test
	void expireSessionWhenMaxAgeSetViaInitializer() {
		this.resolver.addCookieInitializer(builder -> builder.maxAge(600));
		this.resolver.expireSession(this.exchange);

		assertCookieValue("SESSION=; Path=/; Max-Age=0; " +
				"Expires=Thu, 01 Jan 1970 00:00:00 GMT; Secure; HttpOnly; SameSite=Lax");
	}

	private void assertCookieValue(String expected) {
		MultiValueMap<String, ResponseCookie> cookies = this.exchange.getResponse().getCookies();
		assertThat(cookies).hasSize(1);
		ResponseCookie cookie = cookies.getFirst(this.resolver.getCookieName());
		assertThat(cookie).isNotNull();
		assertThat(cookie.toString()).isEqualTo(expected);
	}

}
