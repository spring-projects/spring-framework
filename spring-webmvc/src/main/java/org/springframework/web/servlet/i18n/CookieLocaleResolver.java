/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.servlet.i18n;

import java.time.Duration;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Function;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.util.WebUtils;

/**
 * {@link LocaleResolver} implementation that uses a cookie sent back to the user
 * in case of a custom setting, with a fallback to the configured default locale,
 * the request's {@code Accept-Language} header, or the default locale for the server.
 *
 * <p>This is particularly useful for stateless applications without user sessions.
 * The cookie may optionally contain an associated time zone value as well;
 * alternatively, you may specify a default time zone.
 *
 * <p>Custom controllers can override the user's locale and time zone by calling
 * {@code #setLocale(Context)} on the resolver, e.g. responding to a locale change
 * request. As a more convenient alternative, consider using
 * {@link org.springframework.web.servlet.support.RequestContext#changeLocale}.
 *
 * @author Juergen Hoeller
 * @author Jean-Pierre Pawlak
 * @author Vedran Pavic
 * @author Sam Brannen
 * @since 27.02.2003
 * @see #setDefaultLocale
 * @see #setDefaultTimeZone
 */
public class CookieLocaleResolver extends AbstractLocaleContextResolver {

	/**
	 * The name of the request attribute that holds the {@code Locale}.
	 * <p>Only used for overriding a cookie value if the locale has been
	 * changed in the course of the current request!
	 * <p>Use {@code RequestContext(Utils).getLocale()}
	 * to retrieve the current locale in controllers or views.
	 * @see org.springframework.web.servlet.support.RequestContext#getLocale
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocale
	 */
	public static final String LOCALE_REQUEST_ATTRIBUTE_NAME = CookieLocaleResolver.class.getName() + ".LOCALE";

	/**
	 * The name of the request attribute that holds the {@code TimeZone}.
	 * <p>Only used for overriding a cookie value if the locale has been
	 * changed in the course of the current request!
	 * <p>Use {@code RequestContext(Utils).getTimeZone()}
	 * to retrieve the current time zone in controllers or views.
	 * @see org.springframework.web.servlet.support.RequestContext#getTimeZone
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getTimeZone
	 */
	public static final String TIME_ZONE_REQUEST_ATTRIBUTE_NAME = CookieLocaleResolver.class.getName() + ".TIME_ZONE";

	/**
	 * The default cookie name used if none is explicitly set.
	 */
	public static final String DEFAULT_COOKIE_NAME = CookieLocaleResolver.class.getName() + ".LOCALE";

	private static final Log logger = LogFactory.getLog(CookieLocaleResolver.class);


	private ResponseCookie cookie;

	private boolean languageTagCompliant = true;

	private boolean rejectInvalidCookies = true;

	private Function<HttpServletRequest, Locale> defaultLocaleFunction = request -> {
		Locale defaultLocale = getDefaultLocale();
		return (defaultLocale != null ? defaultLocale : request.getLocale());
	};

	private Function<HttpServletRequest, TimeZone> defaultTimeZoneFunction = request -> getDefaultTimeZone();


	/**
	 * Constructor with a given cookie name.
	 * @since 6.0
	 */
	public CookieLocaleResolver(String cookieName) {
		Assert.notNull(cookieName, "'cookieName' must not be null");
		this.cookie = ResponseCookie.from(cookieName).path("/").sameSite("Lax").build();
	}

	/**
	 * Constructor with a {@linkplain #DEFAULT_COOKIE_NAME default cookie name}.
	 */
	public CookieLocaleResolver() {
		this(DEFAULT_COOKIE_NAME);
	}


	/**
	 * Set the name of cookie created by this resolver.
	 * @param cookieName the cookie name
	 * @deprecated as of 6.0 in favor of {@link #CookieLocaleResolver(String)}
	 */
	@Deprecated
	public void setCookieName(String cookieName) {
		Assert.notNull(cookieName, "cookieName must not be null");
		this.cookie = ResponseCookie.from(cookieName)
				.maxAge(this.cookie.getMaxAge())
				.domain(this.cookie.getDomain())
				.path(this.cookie.getPath())
				.secure(this.cookie.isSecure())
				.httpOnly(this.cookie.isHttpOnly())
				.sameSite(this.cookie.getSameSite())
				.build();

	}

	/**
	 * Set the cookie "Max-Age" attribute.
	 * <p>By default, this is set to -1 in which case the cookie persists until
	 * browser shutdown.
	 * @since 6.0
	 * @see org.springframework.http.ResponseCookie.ResponseCookieBuilder#maxAge(Duration)
	 */
	public void setCookieMaxAge(Duration cookieMaxAge) {
		Assert.notNull(cookieMaxAge, "'cookieMaxAge' must not be null");
		this.cookie = this.cookie.mutate().maxAge(cookieMaxAge).build();
	}

	/**
	 * Variant of {@link #setCookieMaxAge(Duration)} with a value in seconds.
	 * @deprecated as of 6.0 in favor of {@link #setCookieMaxAge(Duration)}
	 */
	@Deprecated
	public void setCookieMaxAge(@Nullable Integer cookieMaxAge) {
		setCookieMaxAge(Duration.ofSeconds((cookieMaxAge != null) ? cookieMaxAge : -1));
	}

	/**
	 * Set the cookie "Path" attribute.
	 * <p>By default, this is set to {@code "/"}.
	 * @see org.springframework.http.ResponseCookie.ResponseCookieBuilder#path(String)
	 */
	public void setCookiePath(@Nullable String cookiePath) {
		this.cookie = this.cookie.mutate().path(cookiePath).build();
	}

	/**
	 * Set the cookie "Domain" attribute.
	 * @see org.springframework.http.ResponseCookie.ResponseCookieBuilder#domain(String)
	 */
	public void setCookieDomain(@Nullable String cookieDomain) {
		this.cookie = this.cookie.mutate().domain(cookieDomain).build();
	}

	/**
	 * Add the "Secure" attribute to the cookie.
	 * @see org.springframework.http.ResponseCookie.ResponseCookieBuilder#secure(boolean)
	 */
	public void setCookieSecure(boolean cookieSecure) {
		this.cookie = this.cookie.mutate().secure(cookieSecure).build();
	}

	/**
	 * Add the "HttpOnly" attribute to the cookie.
	 * @see org.springframework.http.ResponseCookie.ResponseCookieBuilder#httpOnly(boolean)
	 */
	public void setCookieHttpOnly(boolean cookieHttpOnly) {
		this.cookie = this.cookie.mutate().httpOnly(cookieHttpOnly).build();
	}

	/**
	 * Add the "SameSite" attribute to the cookie.
	 * <p>By default, this is set to {@code "Lax"}.
	 * @since 6.0
	 * @see org.springframework.http.ResponseCookie.ResponseCookieBuilder#sameSite(String)
	 */
	public void setCookieSameSite(String cookieSameSite) {
		Assert.notNull(cookieSameSite, "cookieSameSite must not be null");
		this.cookie = this.cookie.mutate().sameSite(cookieSameSite).build();
	}

	/**
	 * Specify whether this resolver's cookies should be compliant with BCP 47
	 * language tags instead of Java's legacy locale specification format.
	 * <p>The default is {@code true}, as of 5.1. Switch this to {@code false}
	 * for rendering Java's legacy locale specification format. For parsing,
	 * this resolver leniently accepts the legacy {@link Locale#toString}
	 * format as well as BCP 47 language tags in any case.
	 * @since 4.3
	 * @see #parseLocaleValue(String)
	 * @see #toLocaleValue(Locale)
	 * @see Locale#forLanguageTag(String)
	 * @see Locale#toLanguageTag()
	 */
	public void setLanguageTagCompliant(boolean languageTagCompliant) {
		this.languageTagCompliant = languageTagCompliant;
	}

	/**
	 * Return whether this resolver's cookies should be compliant with BCP 47
	 * language tags instead of Java's legacy locale specification format.
	 * @since 4.3
	 */
	public boolean isLanguageTagCompliant() {
		return this.languageTagCompliant;
	}

	/**
	 * Specify whether to reject cookies with invalid content (e.g. invalid format).
	 * <p>The default is {@code true}. Turn this off for lenient handling of parse
	 * failures, falling back to the default locale and time zone in such a case.
	 * @since 5.1.7
	 * @see #setDefaultLocale
	 * @see #setDefaultTimeZone
	 * @see #setDefaultLocaleFunction(Function)
	 * @see #setDefaultTimeZoneFunction(Function)
	 */
	public void setRejectInvalidCookies(boolean rejectInvalidCookies) {
		this.rejectInvalidCookies = rejectInvalidCookies;
	}

	/**
	 * Return whether to reject cookies with invalid content (e.g. invalid format).
	 * @since 5.1.7
	 */
	public boolean isRejectInvalidCookies() {
		return this.rejectInvalidCookies;
	}

	/**
	 * Set the function used to determine the default locale for the given request,
	 * called if no locale cookie has been found.
	 * <p>The default implementation returns the configured
	 * {@linkplain #setDefaultLocale(Locale) default locale}, if any, and otherwise
	 * falls back to the request's {@code Accept-Language} header locale or the
	 * default locale for the server.
	 * @param defaultLocaleFunction the function used to determine the default locale
	 * @since 6.0
	 * @see #setDefaultLocale
	 * @see jakarta.servlet.http.HttpServletRequest#getLocale()
	 */
	public void setDefaultLocaleFunction(Function<HttpServletRequest, Locale> defaultLocaleFunction) {
		Assert.notNull(defaultLocaleFunction, "defaultLocaleFunction must not be null");
		this.defaultLocaleFunction = defaultLocaleFunction;
	}

	/**
	 * Set the function used to determine the default time zone for the given request,
	 * called if no locale cookie has been found.
	 * <p>The default implementation returns the configured default time zone,
	 * if any, or {@code null} otherwise.
	 * @param defaultTimeZoneFunction the function used to determine the default time zone
	 * @since 6.0
	 * @see #setDefaultTimeZone
	 */
	public void setDefaultTimeZoneFunction(Function<HttpServletRequest, TimeZone> defaultTimeZoneFunction) {
		Assert.notNull(defaultTimeZoneFunction, "defaultTimeZoneFunction must not be null");
		this.defaultTimeZoneFunction = defaultTimeZoneFunction;
	}


	@Override
	public Locale resolveLocale(HttpServletRequest request) {
		parseLocaleCookieIfNecessary(request);
		return (Locale) request.getAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME);
	}

	@Override
	public LocaleContext resolveLocaleContext(final HttpServletRequest request) {
		parseLocaleCookieIfNecessary(request);
		return new TimeZoneAwareLocaleContext() {
			@Override
			@Nullable
			public Locale getLocale() {
				return (Locale) request.getAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME);
			}
			@Override
			@Nullable
			public TimeZone getTimeZone() {
				return (TimeZone) request.getAttribute(TIME_ZONE_REQUEST_ATTRIBUTE_NAME);
			}
		};
	}

	private void parseLocaleCookieIfNecessary(HttpServletRequest request) {
		if (request.getAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME) == null) {
			Locale locale = null;
			TimeZone timeZone = null;

			// Retrieve and parse cookie value.
			Cookie cookie = WebUtils.getCookie(request, this.cookie.getName());
			if (cookie != null) {
				String value = cookie.getValue();
				String localePart = value;
				String timeZonePart = null;
				int separatorIndex = localePart.indexOf('/');
				if (separatorIndex == -1) {
					// Leniently accept older cookies separated by a space...
					separatorIndex = localePart.indexOf(' ');
				}
				if (separatorIndex >= 0) {
					localePart = value.substring(0, separatorIndex);
					timeZonePart = value.substring(separatorIndex + 1);
				}
				try {
					locale = (!"-".equals(localePart) ? parseLocaleValue(localePart) : null);
					if (timeZonePart != null) {
						timeZone = StringUtils.parseTimeZoneString(timeZonePart);
					}
				}
				catch (IllegalArgumentException ex) {
					if (isRejectInvalidCookies() &&
							request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE) == null) {
						throw new IllegalStateException("Encountered invalid locale cookie '" +
								this.cookie.getName() + "': [" + value + "] due to: " + ex.getMessage());
					}
					else {
						// Lenient handling (e.g. error dispatch): ignore locale/timezone parse exceptions
						if (logger.isDebugEnabled()) {
							logger.debug("Ignoring invalid locale cookie '" + this.cookie.getName() +
									"': [" + value + "] due to: " + ex.getMessage());
						}
					}
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Parsed cookie value [" + cookie.getValue() + "] into locale '" + locale +
							"'" + (timeZone != null ? " and time zone '" + timeZone.getID() + "'" : ""));
				}
			}

			request.setAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME,
					(locale != null ? locale : this.defaultLocaleFunction.apply(request)));
			request.setAttribute(TIME_ZONE_REQUEST_ATTRIBUTE_NAME,
					(timeZone != null ? timeZone : this.defaultTimeZoneFunction.apply(request)));
		}
	}

	@Override
	public void setLocaleContext(HttpServletRequest request, @Nullable HttpServletResponse response,
			@Nullable LocaleContext localeContext) {

		Assert.notNull(response, "HttpServletResponse is required for CookieLocaleResolver");

		Locale locale = null;
		TimeZone zone = null;
		if (localeContext != null) {
			locale = localeContext.getLocale();
			if (localeContext instanceof TimeZoneAwareLocaleContext timeZoneAwareLocaleContext) {
				zone = timeZoneAwareLocaleContext.getTimeZone();
			}
			String value = (locale != null ? toLocaleValue(locale) : "-") + (zone != null ? '/' + zone.getID() : "");
			this.cookie = this.cookie.mutate().value(value).build();
		}
		response.addHeader(HttpHeaders.SET_COOKIE, this.cookie.toString());
		request.setAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME,
				(locale != null ? locale : this.defaultLocaleFunction.apply(request)));
		request.setAttribute(TIME_ZONE_REQUEST_ATTRIBUTE_NAME,
				(zone != null ? zone : this.defaultTimeZoneFunction.apply(request)));
	}


	/**
	 * Parse the given locale value coming from an incoming cookie.
	 * <p>The default implementation calls {@link StringUtils#parseLocale(String)},
	 * accepting the {@link Locale#toString} format as well as BCP 47 language tags.
	 * @param localeValue the locale value to parse
	 * @return the corresponding {@code Locale} instance
	 * @since 4.3
	 * @see StringUtils#parseLocale(String)
	 */
	@Nullable
	protected Locale parseLocaleValue(String localeValue) {
		return StringUtils.parseLocale(localeValue);
	}

	/**
	 * Render the given locale as a text value for inclusion in a cookie.
	 * <p>The default implementation calls {@link Locale#toString()}
	 * or {@link Locale#toLanguageTag()}, depending on the
	 * {@link #setLanguageTagCompliant "languageTagCompliant"} configuration property.
	 * @param locale the locale to convert to a string
	 * @return a String representation for the given locale
	 * @since 4.3
	 * @see #isLanguageTagCompliant()
	 */
	protected String toLocaleValue(Locale locale) {
		return (isLanguageTagCompliant() ? locale.toLanguageTag() : locale.toString());
	}

	/**
	 * Determine the default locale for the given request, called if no locale
	 * cookie has been found.
	 * <p>The default implementation returns the configured default locale, if any,
	 * and otherwise falls back to the request's {@code Accept-Language} header
	 * locale or the default locale for the server.
	 * @param request the request to resolve the locale for
	 * @return the default locale (never {@code null})
	 * @see #setDefaultLocale
	 * @see jakarta.servlet.http.HttpServletRequest#getLocale()
	 * @deprecated as of 6.0, in favor of {@link #setDefaultLocaleFunction(Function)}
	 */
	@Deprecated(since = "6.0")
	protected Locale determineDefaultLocale(HttpServletRequest request) {
		return this.defaultLocaleFunction.apply(request);
	}

	/**
	 * Determine the default time zone for the given request, called if no locale
	 * cookie has been found.
	 * <p>The default implementation returns the configured default time zone,
	 * if any, or {@code null} otherwise.
	 * @param request the request to resolve the time zone for
	 * @return the default time zone (or {@code null} if none defined)
	 * @see #setDefaultTimeZone
	 * @deprecated as of 6.0, in favor of {@link #setDefaultTimeZoneFunction(Function)}
	 */
	@Deprecated(since = "6.0")
	@Nullable
	protected TimeZone determineDefaultTimeZone(HttpServletRequest request) {
		return this.defaultTimeZoneFunction.apply(request);
	}

}
