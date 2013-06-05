/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.servlet.i18n;

import java.util.TimeZone;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.TimeZoneResolver;
import org.springframework.web.util.CookieGenerator;
import org.springframework.web.util.WebUtils;

/**
 * {@link TimeZoneResolver} implementation that uses a cookie sent back to the user
 * in case of a custom setting, with a fallback to the specified default time zone.
 *
 * <p>This is particularly useful for stateless applications without user sessions.
 *
 * <p>Custom controllers can thus override the user's time zone by calling
 * {@link #setTimeZone}, for example responding to a time zone change request.
 *
 * @author Nicholas Williams
 * @since 4.0
 * @see #setDefaultTimeZone
 * @see #setTimeZone
 */
public class CookieTimeZoneResolver extends CookieGenerator implements TimeZoneResolver {

	/**
	 * The name of the request attribute that holds the time zone.
	 * <p>Only used for overriding a cookie value if the time zone has been
	 * changed in the course of the current request! Use
	 * {@link org.springframework.web.servlet.support.RequestContext#getTimeZone}
	 * to retrieve the current time zone in controllers or views.
	 * @see org.springframework.web.servlet.support.RequestContext#getTimeZone
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getTimeZone
	 */
	public static final String TIME_ZONE_REQUEST_ATTRIBUTE_NAME =
			CookieTimeZoneResolver.class.getName() + ".TIME_ZONE";

	/**
	 * The default cookie name used if none is explicitly set.
	 */
	public static final String DEFAULT_COOKIE_NAME =
			CookieTimeZoneResolver.class.getName() + ".TIME_ZONE";


	private TimeZone defaultTimeZone = AbstractTimeZoneResolver.ORIGINAL_DEFAULT_TIME_ZONE;


	/**
	 * Creates a new instance of the {@code CookieTimeZoneResolver} class
	 * using the {@link #DEFAULT_COOKIE_NAME default cookie name}.
	 */
	public CookieTimeZoneResolver() {
		this.setCookieName(DEFAULT_COOKIE_NAME);
	}


	/**
	 * Set a default time zone that this resolver will fall back to if no other time zone
	 * is found.
	 *
	 * @param defaultTimeZone the default time zone to fall back to.
	 */
	public void setDefaultTimeZone(TimeZone defaultTimeZone) {
		Assert.notNull(defaultTimeZone, "The default time zone cannot be null.");
		this.defaultTimeZone = defaultTimeZone;
	}

	/**
	 * Return the default time zone that this resolver is supposed to fall back to, if
	 * any.
	 */
	protected TimeZone getDefaultTimeZone() {
		return this.defaultTimeZone;
	}

	@Override
	public TimeZone resolveTimeZone(HttpServletRequest request) {
		// Check request for pre-parsed or preset time zone.
		TimeZone timeZone = (TimeZone) request.getAttribute(TIME_ZONE_REQUEST_ATTRIBUTE_NAME);
		if (timeZone != null) {
			return timeZone;
		}

		// Retrieve and parse cookie value.
		Cookie cookie = WebUtils.getCookie(request, getCookieName());
		if (cookie != null) {
			timeZone = StringUtils.parseTimeZoneString(cookie.getValue());
			if (logger.isDebugEnabled()) {
				logger.debug("Parsed cookie value [" + cookie.getValue() +
						"] into time zone '" + timeZone.getID() + "'");
			}
			if (timeZone != null) {
				request.setAttribute(TIME_ZONE_REQUEST_ATTRIBUTE_NAME, timeZone);
				return timeZone;
			}
		}

		return this.getDefaultTimeZone();
	}

	@Override
	public void setTimeZone(HttpServletRequest request, HttpServletResponse response, TimeZone timeZone) {
		if (timeZone != null) {
			// Set request attribute and add cookie.
			request.setAttribute(TIME_ZONE_REQUEST_ATTRIBUTE_NAME, timeZone);
			addCookie(response, timeZone.getID());
		}
		else {
			// Set request attribute to fallback time zone and remove cookie.
			request.setAttribute(TIME_ZONE_REQUEST_ATTRIBUTE_NAME, this.getDefaultTimeZone());
			removeCookie(response);
		}
	}

}
