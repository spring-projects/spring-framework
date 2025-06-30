/*
 * Copyright 2002-present the original author or authors.
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

import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Function;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.util.Assert;
import org.springframework.web.util.WebUtils;

/**
 * {@link org.springframework.web.servlet.LocaleResolver} implementation that
 * uses a locale attribute in the user's session in case of a custom setting,
 * with a fallback to the configured default locale, the request's
 * {@code Accept-Language} header, or the default locale for the server.
 *
 * <p>This is most appropriate if the application needs user sessions anyway,
 * i.e. when the {@code HttpSession} does not have to be created just for storing
 * the user's locale. The session may optionally contain an associated time zone
 * attribute as well; alternatively, you may specify a default time zone.
 *
 * <p>Custom controllers can override the user's locale and time zone by calling
 * {@code #setLocale(Context)} on the resolver, for example, responding to a locale change
 * request. As a more convenient alternative, consider using
 * {@link org.springframework.web.servlet.support.RequestContext#changeLocale}.
 *
 * <p>In contrast to {@link CookieLocaleResolver}, this strategy stores locally
 * chosen locale settings in the Servlet container's {@code HttpSession}. As a
 * consequence, those settings are just temporary for each session and therefore
 * lost when each session terminates.
 *
 * <p>Note that there is no direct relationship with external session management
 * mechanisms such as the "Spring Session" project. This {@code LocaleResolver}
 * will simply evaluate and modify corresponding {@code HttpSession} attributes
 * against the current {@code HttpServletRequest}.
 *
 * @author Juergen Hoeller
 * @author Vedran Pavic
 * @since 27.02.2003
 * @see #setDefaultLocale
 * @see #setDefaultTimeZone
 */
public class SessionLocaleResolver extends AbstractLocaleContextResolver {

	/**
	 * Default name of the session attribute that holds the Locale.
	 * <p>Only used internally by this implementation.
	 * <p>Use {@code RequestContext(Utils).getLocale()}
	 * to retrieve the current locale in controllers or views.
	 * @see org.springframework.web.servlet.support.RequestContext#getLocale
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocale
	 */
	public static final String LOCALE_SESSION_ATTRIBUTE_NAME = SessionLocaleResolver.class.getName() + ".LOCALE";

	/**
	 * Default name of the session attribute that holds the TimeZone.
	 * <p>Only used internally by this implementation.
	 * <p>Use {@code RequestContext(Utils).getTimeZone()}
	 * to retrieve the current time zone in controllers or views.
	 * @see org.springframework.web.servlet.support.RequestContext#getTimeZone
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getTimeZone
	 */
	public static final String TIME_ZONE_SESSION_ATTRIBUTE_NAME = SessionLocaleResolver.class.getName() + ".TIME_ZONE";


	private String localeAttributeName = LOCALE_SESSION_ATTRIBUTE_NAME;

	private String timeZoneAttributeName = TIME_ZONE_SESSION_ATTRIBUTE_NAME;

	private Function<HttpServletRequest, Locale> defaultLocaleFunction = request -> {
		Locale defaultLocale = getDefaultLocale();
		return (defaultLocale != null ? defaultLocale : request.getLocale());
	};

	private Function<HttpServletRequest, @Nullable TimeZone> defaultTimeZoneFunction = request -> getDefaultTimeZone();

	/**
	 * Specify the name of the corresponding attribute in the {@code HttpSession},
	 * holding the current {@link Locale} value.
	 * <p>The default is an internal {@link #LOCALE_SESSION_ATTRIBUTE_NAME}.
	 * @since 4.3.8
	 */
	public void setLocaleAttributeName(String localeAttributeName) {
		this.localeAttributeName = localeAttributeName;
	}

	/**
	 * Specify the name of the corresponding attribute in the {@code HttpSession},
	 * holding the current {@link TimeZone} value.
	 * <p>The default is an internal {@link #TIME_ZONE_SESSION_ATTRIBUTE_NAME}.
	 * @since 4.3.8
	 */
	public void setTimeZoneAttributeName(String timeZoneAttributeName) {
		this.timeZoneAttributeName = timeZoneAttributeName;
	}

	/**
	 * Set the function used to determine the default locale for the given request,
	 * called if no {@link Locale} session attribute has been found.
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
	 * called if no {@link TimeZone} session attribute has been found.
	 * <p>The default implementation returns the configured default time zone,
	 * if any, or {@code null} otherwise.
	 * @param defaultTimeZoneFunction the function used to determine the default time zone
	 * @since 6.0
	 * @see #setDefaultTimeZone
	 */
	public void setDefaultTimeZoneFunction(Function<HttpServletRequest, @Nullable TimeZone> defaultTimeZoneFunction) {
		Assert.notNull(defaultTimeZoneFunction, "defaultTimeZoneFunction must not be null");
		this.defaultTimeZoneFunction = defaultTimeZoneFunction;
	}

	@Override
	public Locale resolveLocale(HttpServletRequest request) {
		Locale locale = (Locale) WebUtils.getSessionAttribute(request, this.localeAttributeName);
		if (locale == null) {
			locale = this.defaultLocaleFunction.apply(request);
		}
		return locale;
	}

	@Override
	public LocaleContext resolveLocaleContext(final HttpServletRequest request) {
		return new TimeZoneAwareLocaleContext() {
			@Override
			public Locale getLocale() {
				Locale locale = (Locale) WebUtils.getSessionAttribute(request, localeAttributeName);
				if (locale == null) {
					locale = defaultLocaleFunction.apply(request);
				}
				return locale;
			}
			@Override
			public @Nullable TimeZone getTimeZone() {
				TimeZone timeZone = (TimeZone) WebUtils.getSessionAttribute(request, timeZoneAttributeName);
				if (timeZone == null) {
					timeZone = defaultTimeZoneFunction.apply(request);
				}
				return timeZone;
			}
		};
	}

	@Override
	public void setLocaleContext(HttpServletRequest request, @Nullable HttpServletResponse response,
			@Nullable LocaleContext localeContext) {

		Locale locale = null;
		TimeZone timeZone = null;
		if (localeContext != null) {
			locale = localeContext.getLocale();
			if (localeContext instanceof TimeZoneAwareLocaleContext timeZoneAwareLocaleContext) {
				timeZone = timeZoneAwareLocaleContext.getTimeZone();
			}
		}
		WebUtils.setSessionAttribute(request, this.localeAttributeName, locale);
		WebUtils.setSessionAttribute(request, this.timeZoneAttributeName, timeZone);
	}

}
