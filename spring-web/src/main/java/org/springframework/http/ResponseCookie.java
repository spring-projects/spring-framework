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

package org.springframework.http;

import java.time.Duration;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * An {@code HttpCookie} subclass with the additional attributes allowed in
 * the "Set-Cookie" response header. To build an instance use the {@link #from}
 * static method.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 * @see <a href="https://tools.ietf.org/html/rfc6265">RFC 6265</a>
 */
public final class ResponseCookie extends HttpCookie {

	private final Duration maxAge;

	@Nullable
	private final String domain;

	@Nullable
	private final String path;

	private final boolean secure;

	private final boolean httpOnly;

	@Nullable
	private final String sameSite;


	/**
	 * Private constructor. See {@link #from(String, String)}.
	 */
	private ResponseCookie(String name, String value, Duration maxAge, @Nullable String domain,
			@Nullable String path, boolean secure, boolean httpOnly, @Nullable String sameSite) {

		super(name, value);
		Assert.notNull(maxAge, "Max age must not be null");

		this.maxAge = maxAge;
		this.domain = domain;
		this.path = path;
		this.secure = secure;
		this.httpOnly = httpOnly;
		this.sameSite = sameSite;

		Rfc6265Utils.validateCookieName(name);
		Rfc6265Utils.validateCookieValue(value);
		Rfc6265Utils.validateDomain(domain);
		Rfc6265Utils.validatePath(path);
	}


	/**
	 * Return the cookie "Max-Age" attribute in seconds.
	 * <p>A positive value indicates when the cookie expires relative to the
	 * current time. A value of 0 means the cookie should expire immediately.
	 * A negative value means no "Max-Age" attribute in which case the cookie
	 * is removed when the browser is closed.
	 */
	public Duration getMaxAge() {
		return this.maxAge;
	}

	/**
	 * Return the cookie "Domain" attribute, or {@code null} if not set.
	 */
	@Nullable
	public String getDomain() {
		return this.domain;
	}

	/**
	 * Return the cookie "Path" attribute, or {@code null} if not set.
	 */
	@Nullable
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
	 * @see <a href="https://www.owasp.org/index.php/HTTPOnly">https://www.owasp.org/index.php/HTTPOnly</a>
	 */
	public boolean isHttpOnly() {
		return this.httpOnly;
	}

	/**
	 * Return the cookie "SameSite" attribute, or {@code null} if not set.
	 * <p>This limits the scope of the cookie such that it will only be attached to
	 * same site requests if {@code "Strict"} or cross-site requests if {@code "Lax"}.
	 * @see <a href="https://tools.ietf.org/html/draft-ietf-httpbis-rfc6265bis#section-4.1.2.7">RFC6265 bis</a>
	 * @since 5.1
	 */
	@Nullable
	public String getSameSite() {
		return this.sameSite;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ResponseCookie)) {
			return false;
		}
		ResponseCookie otherCookie = (ResponseCookie) other;
		return (getName().equalsIgnoreCase(otherCookie.getName()) &&
				ObjectUtils.nullSafeEquals(this.path, otherCookie.getPath()) &&
				ObjectUtils.nullSafeEquals(this.domain, otherCookie.getDomain()));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.domain);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.path);
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append('=').append(getValue());
		if (StringUtils.hasText(getPath())) {
			sb.append("; Path=").append(getPath());
		}
		if (StringUtils.hasText(this.domain)) {
			sb.append("; Domain=").append(this.domain);
		}
		if (!this.maxAge.isNegative()) {
			sb.append("; Max-Age=").append(this.maxAge.getSeconds());
			sb.append("; Expires=");
			long millis = this.maxAge.getSeconds() > 0 ? System.currentTimeMillis() + this.maxAge.toMillis() : 0;
			sb.append(HttpHeaders.formatDate(millis));
		}
		if (this.secure) {
			sb.append("; Secure");
		}
		if (this.httpOnly) {
			sb.append("; HttpOnly");
		}
		if (StringUtils.hasText(this.sameSite)) {
			sb.append("; SameSite=").append(this.sameSite);
		}
		return sb.toString();
	}


	/**
	 * Factory method to obtain a builder for a server-defined cookie that starts
	 * with a name-value pair and may also include attributes.
	 * @param name the cookie name
	 * @param value the cookie value
	 * @return the created cookie instance
	 */
	public static ResponseCookieBuilder from(final String name, final String value) {

		return new ResponseCookieBuilder() {

			private Duration maxAge = Duration.ofSeconds(-1);

			@Nullable
			private String domain;

			@Nullable
			private String path;

			private boolean secure;

			private boolean httpOnly;

			@Nullable
			private String sameSite;

			@Override
			public ResponseCookieBuilder maxAge(Duration maxAge) {
				this.maxAge = maxAge;
				return this;
			}

			@Override
			public ResponseCookieBuilder maxAge(long maxAgeSeconds) {
				this.maxAge = maxAgeSeconds >= 0 ? Duration.ofSeconds(maxAgeSeconds) : Duration.ofSeconds(-1);
				return this;
			}

			@Override
			public ResponseCookieBuilder domain(String domain) {
				this.domain = domain;
				return this;
			}

			@Override
			public ResponseCookieBuilder path(String path) {
				this.path = path;
				return this;
			}

			@Override
			public ResponseCookieBuilder secure(boolean secure) {
				this.secure = secure;
				return this;
			}

			@Override
			public ResponseCookieBuilder httpOnly(boolean httpOnly) {
				this.httpOnly = httpOnly;
				return this;
			}

			@Override
			public ResponseCookieBuilder sameSite(@Nullable String sameSite) {
				this.sameSite = sameSite;
				return this;
			}

			@Override
			public ResponseCookie build() {
				return new ResponseCookie(name, value, this.maxAge, this.domain, this.path,
						this.secure, this.httpOnly, this.sameSite);
			}
		};
	}


	/**
	 * A builder for a server-defined HttpCookie with attributes.
	 */
	public interface ResponseCookieBuilder {

		/**
		 * Set the cookie "Max-Age" attribute.
		 *
		 * <p>A positive value indicates when the cookie should expire relative
		 * to the current time. A value of 0 means the cookie should expire
		 * immediately. A negative value results in no "Max-Age" attribute in
		 * which case the cookie is removed when the browser is closed.
		 */
		ResponseCookieBuilder maxAge(Duration maxAge);

		/**
		 * Variant of {@link #maxAge(Duration)} accepting a value in seconds.
		 */
		ResponseCookieBuilder maxAge(long maxAgeSeconds);

		/**
		 * Set the cookie "Path" attribute.
		 */
		ResponseCookieBuilder path(String path);

		/**
		 * Set the cookie "Domain" attribute.
		 */
		ResponseCookieBuilder domain(String domain);

		/**
		 * Add the "Secure" attribute to the cookie.
		 */
		ResponseCookieBuilder secure(boolean secure);

		/**
		 * Add the "HttpOnly" attribute to the cookie.
		 * @see <a href="https://www.owasp.org/index.php/HTTPOnly">https://www.owasp.org/index.php/HTTPOnly</a>
		 */
		ResponseCookieBuilder httpOnly(boolean httpOnly);

		/**
		 * Add the "SameSite" attribute to the cookie.
		 * <p>This limits the scope of the cookie such that it will only be
		 * attached to same site requests if {@code "Strict"} or cross-site
		 * requests if {@code "Lax"}.
		 * @since 5.1
		 * @see <a href="https://tools.ietf.org/html/draft-ietf-httpbis-rfc6265bis#section-4.1.2.7">RFC6265 bis</a>
		 */
		ResponseCookieBuilder sameSite(@Nullable String sameSite);

		/**
		 * Create the HttpCookie.
		 */
		ResponseCookie build();
	}


	private static class Rfc6265Utils {

		private static final String SEPARATOR_CHARS = new String(new char[] {
				'(', ')', '<', '>', '@', ',', ';', ':', '\\', '"', '/', '[', ']', '?', '=', '{', '}', ' '
		});

		private static final String DOMAIN_CHARS =
				"0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ.-";


		public static void validateCookieName(String name) {
			for (int i = 0; i < name.length(); i++) {
				char c = name.charAt(i);
				// CTL = <US-ASCII control chars (octets 0 - 31) and DEL (127)>
				if (c <= 0x1F || c == 0x7F) {
					throw new IllegalArgumentException(
							name + ": RFC2616 token cannot have control chars");
				}
				if (SEPARATOR_CHARS.indexOf(c) >= 0) {
					throw new IllegalArgumentException(
							name + ": RFC2616 token cannot have separator chars such as '" + c + "'");
				}
				if (c >= 0x80) {
					throw new IllegalArgumentException(
							name + ": RFC2616 token can only have US-ASCII: 0x" + Integer.toHexString(c));
				}
			}
		}

		public static void validateCookieValue(@Nullable String value) {
			if (value == null) {
				return;
			}
			int start = 0;
			int end = value.length();
			if (end > 1 && value.charAt(0) == '"' && value.charAt(end - 1) == '"') {
				start = 1;
				end--;
			}
			char[] chars = value.toCharArray();
			for (int i = start; i < end; i++) {
				char c = chars[i];
				if (c < 0x21 || c == 0x22 || c == 0x2c || c == 0x3b || c == 0x5c || c == 0x7f) {
					throw new IllegalArgumentException(
							"RFC2616 cookie value cannot have '" + c + "'");
				}
				if (c >= 0x80) {
					throw new IllegalArgumentException(
							"RFC2616 cookie value can only have US-ASCII chars: 0x" + Integer.toHexString(c));
				}
			}
		}

		public static void validateDomain(@Nullable String domain) {
			if (!StringUtils.hasLength(domain)) {
				return;
			}
			int char1 = domain.charAt(0);
			int charN = domain.charAt(domain.length() - 1);
			if (char1 == '-' || charN == '.' || charN == '-') {
				throw new IllegalArgumentException("Invalid first/last char in cookie domain: " + domain);
			}
			for (int i = 0, c = -1; i < domain.length(); i++) {
				int p = c;
				c = domain.charAt(i);
				if (DOMAIN_CHARS.indexOf(c) == -1 || (p == '.' && (c == '.' || c == '-')) || (p == '-' && c == '.')) {
					throw new IllegalArgumentException(domain + ": invalid cookie domain char '" + c + "'");
				}
			}
		}

		public static void validatePath(@Nullable String path) {
			if (path == null) {
				return;
			}
			for (int i = 0; i < path.length(); i++) {
				char c = path.charAt(i);
				if (c < 0x20 || c > 0x7E || c == ';') {
					throw new IllegalArgumentException(path + ": Invalid cookie path char '" + c + "'");
				}
			}
		}
	}

}
