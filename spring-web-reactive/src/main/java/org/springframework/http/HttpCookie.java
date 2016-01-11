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
import org.springframework.util.ObjectUtils;

/**
 * Representation for an HTTP Cookie.
 *
 * <p>Use the {@link #clientCookie} factory method to create a client-to-server,
 * name-value pair cookie and the {@link #serverCookie} factory method to build
 * a server-to-client cookie with additional attributes.
 *
 * @author Rossen Stoyanchev
 * @see <a href="https://tools.ietf.org/html/rfc6265">RFC 6265</a>
 */
public final class HttpCookie {

	private final String name;

	private final String value;

	private final int maxAge;

	private final String domain;

	private final String path;

	private final boolean secure;

	private final boolean httpOnly;


	private HttpCookie(String name, String value) {
		this(name, value, -1, null, null, false, false);
	}

	private HttpCookie(String name, String value, int maxAge, String domain, String path,
			boolean secure, boolean httpOnly) {

		Assert.hasLength(name, "'name' is required and must not be empty.");
		Assert.hasLength(value, "'value' is required and must not be empty.");
		this.name = name;
		this.value = value;
		this.maxAge = (maxAge > -1 ? maxAge : -1);
		this.domain = domain;
		this.path = path;
		this.secure = secure;
		this.httpOnly = httpOnly;
	}

	/**
	 * Return the cookie name.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the cookie value.
	 */
	public String getValue() {
		return this.value;
	}

	/**
	 * Return the cookie "Max-Age" attribute in seconds.
	 *
	 * <p>A positive value indicates when the cookie expires relative to the
	 * current time. A value of 0 means the cookie should expire immediately.
	 * A negative value means no "Max-Age" attribute in which case the cookie
	 * is removed when the browser is closed.
	 */
	public int getMaxAge() {
		return this.maxAge;
	}

	/**
	 * Return the cookie "Domain" attribute.
	 */
	public String getDomain() {
		return this.domain;
	}

	/**
	 * Return the cookie "Path" attribute.
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * Return {@code true} if the cookie has the "Secure" attribute.
	 */
	public boolean isSecure() {
		return this.secure;
	}

	/**
	 * Return {@code true} if the cookie has the "HttpOnly" attribute.
	 * @see <a href="http://www.owasp.org/index.php/HTTPOnly">http://www.owasp.org/index.php/HTTPOnly</a>
	 */
	public boolean isHttpOnly() {
		return this.httpOnly;
	}

	@Override
	public int hashCode() {
		int result = this.name.hashCode();
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.domain);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.path);
		return result;
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
		return (this.name.equalsIgnoreCase(otherCookie.getName()) &&
				ObjectUtils.nullSafeEquals(this.path, otherCookie.getPath()) &&
				ObjectUtils.nullSafeEquals(this.domain, otherCookie.getDomain()));
	}

	/**
	 * Factory method to create a cookie sent from a client to a server.
	 * Client cookies are name-value pairs only without attributes.
	 * @param name the cookie name
	 * @param value the cookie value
	 * @return the created cookie instance
	 */
	public static HttpCookie clientCookie(String name, String value) {
		return new HttpCookie(name, value);
	}

	/**
	 * Factory method to obtain a builder for a server-defined cookie that starts
	 * with a name-value pair and may also include attributes.
	 * @param name the cookie name
	 * @param value the cookie value
	 * @return the created cookie instance
	 */
	public static HttpCookieBuilder serverCookie(final String name, final String value) {

		return new HttpCookieBuilder() {

			private int maxAge = -1;

			private String domain;

			private String path;

			private boolean secure;

			private boolean httpOnly;


			@Override
			public HttpCookieBuilder maxAge(int maxAge) {
				this.maxAge = maxAge;
				return this;
			}

			@Override
			public HttpCookieBuilder domain(String domain) {
				this.domain = domain;
				return this;
			}

			@Override
			public HttpCookieBuilder path(String path) {
				this.path = path;
				return this;
			}

			@Override
			public HttpCookieBuilder secure() {
				this.secure = true;
				return this;
			}

			@Override
			public HttpCookieBuilder httpOnly() {
				this.httpOnly = true;
				return this;
			}

			@Override
			public HttpCookie build() {
				return new HttpCookie(name, value, this.maxAge, this.domain, this.path,
						this.secure, this.httpOnly);
			}
		};
	}

	/**
	 * A builder for a server-defined HttpCookie with attributes.
	 */
	public interface HttpCookieBuilder {

		/**
		 * Set the cookie "Max-Age" attribute in seconds.
		 *
		 * <p>A positive value indicates when the cookie should expire relative
		 * to the current time. A value of 0 means the cookie should expire
		 * immediately. A negative value results in no "Max-Age" attribute in
		 * which case the cookie is removed when the browser is closed.
		 */
		HttpCookieBuilder maxAge(int maxAge);

		/**
		 * Set the cookie "Path" attribute.
		 */
		HttpCookieBuilder path(String path);

		/**
		 * Set the cookie "Domain" attribute.
		 */
		HttpCookieBuilder domain(String domain);

		/**
		 * Add the "Secure" attribute to the cookie.
		 */
		HttpCookieBuilder secure();

		/**
		 * Add the "HttpOnly" attribute to the cookie.
		 * @see <a href="http://www.owasp.org/index.php/HTTPOnly">http://www.owasp.org/index.php/HTTPOnly</a>
		 */
		HttpCookieBuilder httpOnly();

		/**
		 * Create the HttpCookie.
		 */
		HttpCookie build();
	}

}
