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
import org.springframework.util.Assert;

/**
 * Extension of {@code Cookie} with extra directives, as defined in
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
	 * Constructor with the cookie name and value.
	 * @param name  the name
	 * @param value the value
	 * @see Cookie#Cookie(String, String)
	 */
	public MockCookie(String name, String value) {
		super(name, value);
	}


	/**
	 * Add the "SameSite" attribute to the cookie.
	 * <p>This limits the scope of the cookie such that it will only be attached
	 * to same site requests if {@code "Strict"} or cross-site requests if
	 * {@code "Lax"}.
	 * @see <a href="https://tools.ietf.org/html/draft-ietf-httpbis-rfc6265bis#section-4.1.2.7">RFC6265 bis</a>
	 */
	public void setSameSite(@Nullable String sameSite) {
		this.sameSite = sameSite;
	}

	/**
	 * Return the "SameSite" attribute, or {@code null} if not set.
	 */
	@Nullable
	public String getSameSite() {
		return this.sameSite;
	}


	/**
	 * Factory method that parses the value of a "Set-Cookie" header.
	 * @param setCookieHeader the "Set-Cookie" value
	 * @return the created cookie
	 */
	public static MockCookie parse(String setCookieHeader) {
		String[] cookieParts = setCookieHeader.split("\\s*=\\s*", 2);
		Assert.isTrue(cookieParts.length == 2, "Invalid Set-Cookie header value");

		String name = cookieParts[0];
		String[] valueAndDirectives = cookieParts[1].split("\\s*;\\s*", 2);
		String value = valueAndDirectives[0];
		String[] directives = valueAndDirectives[1].split("\\s*;\\s*");

		MockCookie cookie = new MockCookie(name, value);
		for (String directive : directives) {
			if (directive.startsWith("Domain")) {
				cookie.setDomain(extractDirectiveValue(directive));
			}
			else if (directive.startsWith("Max-Age")) {
				cookie.setMaxAge(Integer.parseInt(extractDirectiveValue(directive)));
			}
			else if (directive.startsWith("Path")) {
				cookie.setPath(extractDirectiveValue(directive));
			}
			else if (directive.startsWith("Secure")) {
				cookie.setSecure(true);
			}
			else if (directive.startsWith("HttpOnly")) {
				cookie.setHttpOnly(true);
			}
			else if (directive.startsWith("SameSite")) {
				cookie.setSameSite(extractDirectiveValue(directive));
			}
		}
		return cookie;
	}

	private static String extractDirectiveValue(String directive) {
		String[] nameAndValue = directive.split("=");
		Assert.isTrue(nameAndValue.length == 2, () -> "No value in directive: '" + directive + "'");
		return nameAndValue[1];
	}

}
