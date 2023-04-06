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

package org.springframework.web.testfixture.servlet;

import java.time.DateTimeException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.servlet.http.Cookie;

import org.springframework.core.style.ToStringCreator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Extension of {@code Cookie} with extra attributes, as defined in
 * <a href="https://tools.ietf.org/html/rfc6265">RFC 6265</a>.
 *
 * <p>As of Spring 6.0, this set of mocks is designed on a Servlet 6.0 baseline.
 *
 * @author Vedran Pavic
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 5.1
 */
@SuppressWarnings("removal")
public class MockCookie extends Cookie {

	private static final long serialVersionUID = 4312531139502726325L;

	private static final String SAME_SITE = "SameSite";
	private static final String EXPIRES = "Expires";

	@Nullable
	private ZonedDateTime expires;


	/**
	 * Construct a new {@link MockCookie} with the supplied name and value.
	 * @param name the name
	 * @param value the value
	 * @see Cookie#Cookie(String, String)
	 */
	public MockCookie(String name, String value) {
		super(name, value);
	}

	/**
	 * Set the "Expires" attribute for this cookie.
	 * @since 5.1.11
	 */
	public void setExpires(@Nullable ZonedDateTime expires) {
		setAttribute(EXPIRES, (expires != null ? expires.format(DateTimeFormatter.RFC_1123_DATE_TIME) : null));
	}

	/**
	 * Get the "Expires" attribute for this cookie.
	 * @return the "Expires" attribute for this cookie, or {@code null} if not set
	 * @since 5.1.11
	 */
	@Nullable
	public ZonedDateTime getExpires() {
		return this.expires;
	}

	/**
	 * Set the "SameSite" attribute for this cookie.
	 * <p>This limits the scope of the cookie such that it will only be attached
	 * to same-site requests if the supplied value is {@code "Strict"} or cross-site
	 * requests if the supplied value is {@code "Lax"}.
	 * @see <a href="https://tools.ietf.org/html/draft-ietf-httpbis-rfc6265bis#section-4.1.2.7">RFC6265 bis</a>
	 */
	public void setSameSite(@Nullable String sameSite) {
		setAttribute(SAME_SITE, sameSite);
	}

	/**
	 * Get the "SameSite" attribute for this cookie.
	 * @return the "SameSite" attribute for this cookie, or {@code null} if not set
	 */
	@Nullable
	public String getSameSite() {
		return getAttribute(SAME_SITE);
	}

	/**
	 * Factory method that parses the value of the supplied "Set-Cookie" header.
	 * @param setCookieHeader the "Set-Cookie" value; never {@code null} or empty
	 * @return the created cookie
	 */
	public static MockCookie parse(String setCookieHeader) {
		Assert.notNull(setCookieHeader, "Set-Cookie header must not be null");
		String[] cookieParts = setCookieHeader.split("\\s*=\\s*", 2);
		Assert.isTrue(cookieParts.length == 2, () -> "Invalid Set-Cookie header '" + setCookieHeader + "'");

		String name = cookieParts[0];
		String[] valueAndAttributes = cookieParts[1].split("\\s*;\\s*", 2);
		String value = valueAndAttributes[0];
		String[] attributes =
				(valueAndAttributes.length > 1 ? valueAndAttributes[1].split("\\s*;\\s*") : new String[0]);

		MockCookie cookie = new MockCookie(name, value);
		for (String attribute : attributes) {
			if (StringUtils.startsWithIgnoreCase(attribute, "Domain")) {
				cookie.setDomain(extractAttributeValue(attribute, setCookieHeader));
			}
			else if (StringUtils.startsWithIgnoreCase(attribute, "Max-Age")) {
				cookie.setMaxAge(Integer.parseInt(extractAttributeValue(attribute, setCookieHeader)));
			}
			else if (StringUtils.startsWithIgnoreCase(attribute, EXPIRES)) {
				try {
					cookie.setExpires(ZonedDateTime.parse(extractAttributeValue(attribute, setCookieHeader),
							DateTimeFormatter.RFC_1123_DATE_TIME));
				}
				catch (DateTimeException ex) {
					// ignore invalid date formats
				}
			}
			else if (StringUtils.startsWithIgnoreCase(attribute, "Path")) {
				cookie.setPath(extractAttributeValue(attribute, setCookieHeader));
			}
			else if (StringUtils.startsWithIgnoreCase(attribute, "Secure")) {
				cookie.setSecure(true);
			}
			else if (StringUtils.startsWithIgnoreCase(attribute, "HttpOnly")) {
				cookie.setHttpOnly(true);
			}
			else if (StringUtils.startsWithIgnoreCase(attribute, SAME_SITE)) {
				cookie.setSameSite(extractAttributeValue(attribute, setCookieHeader));
			}
			else if (StringUtils.startsWithIgnoreCase(attribute, "Comment")) {
				cookie.setComment(extractAttributeValue(attribute, setCookieHeader));
			}
		}
		return cookie;
	}

	private static String extractAttributeValue(String attribute, String header) {
		String[] nameAndValue = attribute.split("=");
		Assert.isTrue(nameAndValue.length == 2,
				() -> "No value in attribute '" + nameAndValue[0] + "' for Set-Cookie header '" + header + "'");
		return nameAndValue[1];
	}

	@Override
	public void setAttribute(String name, @Nullable String value) {
		if (EXPIRES.equalsIgnoreCase(name)) {
			this.expires = (value != null ? ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME) : null);
		}
		super.setAttribute(name, value);
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("name", getName())
				.append("value", getValue())
				.append("Path", getPath())
				.append("Domain", getDomain())
				.append("Version", getVersion())
				.append("Comment", getComment())
				.append("Secure", getSecure())
				.append("HttpOnly", isHttpOnly())
				.append(SAME_SITE, getSameSite())
				.append("Max-Age", getMaxAge())
				.append(EXPIRES, getAttribute(EXPIRES))
				.toString();
	}

}
