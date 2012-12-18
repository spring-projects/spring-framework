/*
 * Copyright 2002-2007 the original author or authors.
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

import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.util.CookieGenerator;
import org.springframework.web.util.WebUtils;

/**
 * {@link LocaleResolver} implementation that uses a cookie sent back to the user
 * in case of a custom setting, with a fallback to the specified default locale
 * or the request's accept-header locale.
 *
 * <p>This is particularly useful for stateless applications without user sessions.
 *
 * <p>Custom controllers can thus override the user's locale by calling
 * {@link #setLocale(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.util.Locale)},
 * for example responding to a certain locale change request.
 *
 * @author Juergen Hoeller
 * @author Jean-Pierre Pawlak
 * @since 27.02.2003
 * @see #setDefaultLocale
 * @see #setLocale
 */
public class CookieLocaleResolver extends CookieGenerator implements LocaleResolver {

	/**
	 * The name of the request attribute that holds the locale.
	 * <p>Only used for overriding a cookie value if the locale has been
	 * changed in the course of the current request! Use
	 * {@link org.springframework.web.servlet.support.RequestContext#getLocale}
	 * to retrieve the current locale in controllers or views.
	 * @see org.springframework.web.servlet.support.RequestContext#getLocale
	 */
	public static final String LOCALE_REQUEST_ATTRIBUTE_NAME = CookieLocaleResolver.class.getName() + ".LOCALE";

	/**
	 * The default cookie name used if none is explicitly set.
	 */
	public static final String DEFAULT_COOKIE_NAME = CookieLocaleResolver.class.getName() + ".LOCALE";


	private Locale defaultLocale;


	/**
	 * Creates a new instance of the {@link CookieLocaleResolver} class
	 * using the {@link #DEFAULT_COOKIE_NAME default cookie name}.
	 */
	public CookieLocaleResolver() {
		setCookieName(DEFAULT_COOKIE_NAME);
	}

	/**
	 * Set a fixed Locale that this resolver will return if no cookie found.
	 */
	public void setDefaultLocale(Locale defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

	/**
	 * Return the fixed Locale that this resolver will return if no cookie found,
	 * if any.
	 */
	protected Locale getDefaultLocale() {
		return this.defaultLocale;
	}


	public Locale resolveLocale(HttpServletRequest request) {
		// Check request for pre-parsed or preset locale.
		Locale locale = (Locale) request.getAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME);
		if (locale != null) {
			return locale;
		}

		// Retrieve and parse cookie value.
		Cookie cookie = WebUtils.getCookie(request, getCookieName());
		if (cookie != null) {
			locale = StringUtils.parseLocaleString(cookie.getValue());
			if (logger.isDebugEnabled()) {
				logger.debug("Parsed cookie value [" + cookie.getValue() + "] into locale '" + locale + "'");
			}
			if (locale != null) {
				request.setAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME, locale);
				return locale;
			}
		}

		return determineDefaultLocale(request);
	}

	public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
		if (locale != null) {
			// Set request attribute and add cookie.
			request.setAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME, locale);
			addCookie(response, locale.toString());
		}
		else {
			// Set request attribute to fallback locale and remove cookie.
			request.setAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME, determineDefaultLocale(request));
			removeCookie(response);
		}
	}

	/**
	 * Determine the default locale for the given request,
	 * Called if no locale cookie has been found.
	 * <p>The default implementation returns the specified default locale,
	 * if any, else falls back to the request's accept-header locale.
	 * @param request the request to resolve the locale for
	 * @return the default locale (never {@code null})
	 * @see #setDefaultLocale
	 * @see javax.servlet.http.HttpServletRequest#getLocale()
	 */
	protected Locale determineDefaultLocale(HttpServletRequest request) {
		Locale defaultLocale = getDefaultLocale();
		if (defaultLocale == null) {
			defaultLocale = request.getLocale();
		}
		return defaultLocale;
	}

}
