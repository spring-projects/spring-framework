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

/**
 * Representation for an HTTP Cookie.
 *
 * @author Rossen Stoyanchev
 * @see <a href="https://tools.ietf.org/html/rfc6265">RFC 6265</a>
 */
public class HttpCookie {

	private final String name;

	private final String value;

	private String domain;

	private String path;

	private long maxAge = Long.MIN_VALUE;

	private boolean secure;

	private boolean httpOnly;


	public HttpCookie(String name, String value) {
		this.name = name;
		this.value = value;
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

	public HttpCookie setPath(String path) {
		this.path = path;
		return this;
	}

	/**
	 * Return the domain attribute of the cookie.
	 */
	public String getDomain() {
		return this.domain;
	}

	public HttpCookie setDomain(String domain) {
		this.domain = domain;
		return this;
	}

	/**
	 * Return the path attribute of the cookie.
	 */
	public String getPath() {
		return this.path;
	}

	public HttpCookie setMaxAge(long maxAge) {
		this.maxAge = maxAge;
		return this;
	}

	/**
	 * Return the maximum age attribute of the cookie in seconds or
	 * {@link Long#MIN_VALUE} if not set.
	 */
	public long getMaxAge() {
		return this.maxAge;
	}

	public HttpCookie setSecure(boolean secure) {
		this.secure = secure;
		return this;
	}

	/**
	 * Return true if the "Secure" attribute of the cookie is present.
	 */
	public boolean isSecure() {
		return this.secure;
	}

	public HttpCookie setHttpOnly(boolean httpOnly) {
		this.httpOnly = httpOnly;
		return this;
	}

	/**
	 * Return true if the "HttpOnly" attribute of the cookie is present.
	 * @see <a href="http://www.owasp.org/index.php/HTTPOnly">http://www.owasp.org/index.php/HTTPOnly</a>
	 */
	public boolean isHttpOnly() {
		return this.httpOnly;
	}

}
