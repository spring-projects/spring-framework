/*
 * Copyright 2002-2023 the original author or authors.
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

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

/**
 * Cookie-based {@link WebSessionIdResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public class CookieWebSessionIdResolver implements WebSessionIdResolver {

	private String cookieName = "SESSION";

	private Duration cookieMaxAge = Duration.ofSeconds(-1);

	@Nullable
	private Consumer<ResponseCookie.ResponseCookieBuilder> initializer = null;


	/**
	 * Set the name for the session id cookie.
	 * <p>By default set to "SESSION".
	 * @param cookieName the cookie name
	 */
	public void setCookieName(String cookieName) {
		Assert.hasText(cookieName, "'cookieName' must not be empty");
		this.cookieName = cookieName;
	}

	/**
	 * Get the configured cookie name.
	 */
	public String getCookieName() {
		return this.cookieName;
	}

	/**
	 * Set the "Max-Age" attribute for the session id cookie.
	 * <p>By default set to -1 in which case the cookie is removed when the
	 * browser is closed.
	 * @param maxAge the maxAge duration value
	 * @see ResponseCookie#getMaxAge()
	 */
	public void setCookieMaxAge(Duration maxAge) {
		this.cookieMaxAge = maxAge;
	}

	/**
	 * Get the configured "Max-Age" for the session id cookie.
	 */
	public Duration getCookieMaxAge() {
		return this.cookieMaxAge;
	}

	/**
	 * Add a {@link Consumer} to further initialize the session id cookie
	 * after {@link #getCookieName()} and {@link #getCookieMaxAge()} are applied.
	 * @param initializer consumer to initialize the cookie with
	 * @since 5.1
	 */
	public void addCookieInitializer(Consumer<ResponseCookie.ResponseCookieBuilder> initializer) {
		this.initializer = this.initializer != null ?
				this.initializer.andThen(initializer) : initializer;
	}


	@Override
	public List<String> resolveSessionIds(ServerWebExchange exchange) {
		MultiValueMap<String, HttpCookie> cookieMap = exchange.getRequest().getCookies();
		List<HttpCookie> cookies = cookieMap.get(getCookieName());
		if (cookies == null) {
			return Collections.emptyList();
		}
		return cookies.stream().map(HttpCookie::getValue).toList();
	}

	@Override
	public void setSessionId(ServerWebExchange exchange, String id) {
		Assert.notNull(id, "'id' is required");
		ResponseCookie cookie = initCookie(exchange, id).build();
		exchange.getResponse().getCookies().set(this.cookieName, cookie);
	}

	@Override
	public void expireSession(ServerWebExchange exchange) {
		ResponseCookie cookie = initCookie(exchange, "").maxAge(0).build();
		exchange.getResponse().getCookies().set(this.cookieName, cookie);
	}

	private ResponseCookie.ResponseCookieBuilder initCookie(ServerWebExchange exchange, String id) {
		ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(this.cookieName, id)
				.path(exchange.getRequest().getPath().contextPath().value() + "/")
				.maxAge(getCookieMaxAge())
				.httpOnly(true)
				.secure("https".equalsIgnoreCase(exchange.getRequest().getURI().getScheme()))
				.sameSite("Lax");

		if (this.initializer != null) {
			this.initializer.accept(builder);
		}

		return builder;
	}

}
