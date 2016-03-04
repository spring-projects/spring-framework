/*
 * Copyright 2002-2015 the original author or authors.
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
package org.springframework.http;

import org.springframework.util.Assert;

/**
 * Represents an HTTP Cookie with a name and value.
 *
 * <p>The {@link ServerHttpCookie} sub-class exposes the extra attributes that
 * a server can include in a Set-Cookie response header.
 *
 * @author Rossen Stoyanchev
 * @see <a href="https://tools.ietf.org/html/rfc6265">RFC 6265</a>
 */
public class HttpCookie {

	private final String name;

	private final String value;


	public HttpCookie(String name, String value) {
		Assert.hasLength(name, "'name' is required and must not be empty.");
		this.name = name;
		this.value = (value != null ? value : "");
	}

	/**
	 * Return the cookie name.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the cookie value or an empty string, never {@code null}.
	 */
	public String getValue() {
		return this.value;
	}


	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof HttpCookie)) {
			return false;
		}
		HttpCookie otherCookie = (HttpCookie) other;
		return (this.name.equalsIgnoreCase(otherCookie.getName()));
	}

}
