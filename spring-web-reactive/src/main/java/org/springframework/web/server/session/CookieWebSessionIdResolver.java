/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Optional;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * Cookie-based {@link WebSessionIdResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class CookieWebSessionIdResolver implements WebSessionIdResolver {

	private String cookieName = "SESSION";

	private Duration cookieMaxAge = Duration.ofSeconds(-1);


	/**
	 * Set the name of the cookie to use for the session id.
	 * <p>By default set to "SESSION".
	 * @param cookieName the cookie name
	 */
	public void setCookieName(String cookieName) {
		Assert.hasText(cookieName, "'cookieName' must not be empty.");
		this.cookieName = cookieName;
	}

	/**
	 * Return the configured cookie name.
	 */
	public String getCookieName() {
		return this.cookieName;
	}

	/**
	 * Set the value for the "Max-Age" attribute of the cookie that holds the
	 * session id. For the range of values see {@link HttpCookie#getMaxAge()}.
	 * <p>By default set to -1.
	 * @param maxAge the maxAge duration value
	 */
	public void setCookieMaxAge(Duration maxAge) {
		this.cookieMaxAge = maxAge;
	}

	/**
	 * Return the configured "Max-Age" attribute value for the session cookie.
	 */
	public Duration getCookieMaxAge() {
		return this.cookieMaxAge;
	}


	@Override
	public Optional<String> resolveSessionId(ServerWebExchange exchange) {
		HttpHeaders headers = exchange.getRequest().getHeaders();
		List<HttpCookie> cookies = headers.getCookies().get(getCookieName());
		return (CollectionUtils.isEmpty(cookies) ?
				Optional.empty() : Optional.of(cookies.get(0).getValue()));
	}

	@Override
	public void setSessionId(ServerWebExchange exchange, String id) {
		Duration maxAge = (StringUtils.hasText(id) ? getCookieMaxAge() : Duration.ofSeconds(0));
		HttpCookie cookie = HttpCookie.serverCookie(getCookieName(), id).maxAge(maxAge).build();
		HttpHeaders headers = exchange.getResponse().getHeaders();
		headers.getCookies().put(getCookieName(), Collections.singletonList(cookie));
	}

}
