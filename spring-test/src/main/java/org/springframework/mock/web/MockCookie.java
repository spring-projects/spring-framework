/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.mock.web;

import javax.servlet.http.Cookie;

import org.springframework.lang.Nullable;

/**
 * A {@code Cookie} subclass with the additional cookie directives as defined in the
 * <a href="https://tools.ietf.org/html/rfc6265">RFC 6265</a>.
 *
 * @author Vedran Pavic
 * @since 5.1
 */
public class MockCookie extends Cookie {

	private static final long serialVersionUID = 4312531139502726325L;

	@Nullable
	private String sameSite;

	/**
	 * Constructs a {@code MockCookie} instance with the specified name and value.
	 *
	 * @param name  the cookie name
	 * @param value the cookie value
	 * @see Cookie#Cookie(String, String)
	 */
	public MockCookie(String name, String value) {
		super(name, value);
	}

	/**
	 * Factory method create {@code MockCookie} instance from Set-Cookie header value.
	 *
	 * @param setCookieHeader the Set-Cookie header value
	 * @return the created cookie instance
	 */
	public static MockCookie parse(String setCookieHeader) {
		String[] cookieParts = setCookieHeader.split("\\s*=\\s*", 2);
		if (cookieParts.length != 2) {
			throw new IllegalArgumentException("Invalid Set-Cookie header value");
		}
		String name = cookieParts[0];
		String[] valueAndDirectives = cookieParts[1].split("\\s*;\\s*", 2);
		String value = valueAndDirectives[0];
		String[] directives = valueAndDirectives[1].split("\\s*;\\s*");
		String domain = null;
		int maxAge = -1;
		String path = null;
		boolean secure = false;
		boolean httpOnly = false;
		String sameSite = null;
		for (String directive : directives) {
			if (directive.startsWith("Domain")) {
				domain = directive.split("=")[1];
			}
			else if (directive.startsWith("Max-Age")) {
				maxAge = Integer.parseInt(directive.split("=")[1]);
			}
			else if (directive.startsWith("Path")) {
				path = directive.split("=")[1];
			}
			else if (directive.startsWith("Secure")) {
				secure = true;
			}
			else if (directive.startsWith("HttpOnly")) {
				httpOnly = true;
			}
			else if (directive.startsWith("SameSite")) {
				sameSite = directive.split("=")[1];
			}
		}
		MockCookie cookie = new MockCookie(name, value);
		if (domain != null) {
			cookie.setDomain(domain);
		}
		cookie.setMaxAge(maxAge);
		cookie.setPath(path);
		cookie.setSecure(secure);
		cookie.setHttpOnly(httpOnly);
		cookie.setSameSite(sameSite);
		return cookie;
	}

	/**
	 * Return the cookie "SameSite" attribute, or {@code null} if not set.
	 * <p>
	 * This limits the scope of the cookie such that it will only be attached to same site
	 * requests if {@code "Strict"} or cross-site requests if {@code "Lax"}.
	 *
	 * @see <a href="https://tools.ietf.org/html/draft-ietf-httpbis-rfc6265bis#section-4.1.2.7">RFC6265 bis</a>
	 */
	@Nullable
	public String getSameSite() {
		return this.sameSite;
	}

	/**
	 * Add the "SameSite" attribute to the cookie.
	 * <p>
	 * This limits the scope of the cookie such that it will only be attached to same site
	 * requests if {@code "Strict"} or cross-site requests if {@code "Lax"}.
	 *
	 * @see <a href="https://tools.ietf.org/html/draft-ietf-httpbis-rfc6265bis#section-4.1.2.7">RFC6265 bis</a>
	 */
	public void setSameSite(@Nullable String sameSite) {
		this.sameSite = sameSite;
	}

}
