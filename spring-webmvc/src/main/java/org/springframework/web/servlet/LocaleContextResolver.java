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

package org.springframework.web.servlet;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.SimpleLocaleContext;

/**
 * Extension of {@link LocaleResolver} that adds support for a rich locale context
 * (potentially including locale and time zone information).
 *
 * <p>Also provides {@code default} implementations of {@link #resolveLocale} and
 * {@link #setLocale} which delegate to {@link #resolveLocaleContext} and
 * {@link #setLocaleContext}, respectively.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 * @see org.springframework.context.i18n.LocaleContext
 * @see org.springframework.context.i18n.TimeZoneAwareLocaleContext
 * @see org.springframework.context.i18n.LocaleContextHolder
 * @see org.springframework.web.servlet.support.RequestContext#getTimeZone
 * @see org.springframework.web.servlet.support.RequestContextUtils#getTimeZone
 */
public interface LocaleContextResolver extends LocaleResolver {

	/**
	 * Resolve the current locale context via the given request.
	 * <p>This is primarily intended for framework-level processing; consider using
	 * {@link org.springframework.web.servlet.support.RequestContextUtils} or
	 * {@link org.springframework.web.servlet.support.RequestContext} for
	 * application-level access to the current locale and/or time zone.
	 * <p>The returned context may be a
	 * {@link org.springframework.context.i18n.TimeZoneAwareLocaleContext},
	 * containing a locale with associated time zone information.
	 * Simply apply an {@code instanceof} check and downcast accordingly.
	 * <p>Custom resolver implementations may also return extra settings in
	 * the returned context, which again can be accessed through downcasting.
	 * @param request the request to resolve the locale context for
	 * @return the current locale context (never {@code null}
	 * @see #resolveLocale(HttpServletRequest)
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocale
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getTimeZone
	 */
	LocaleContext resolveLocaleContext(HttpServletRequest request);

	/**
	 * Set the current locale context to the given one,
	 * potentially including a locale with associated time zone information.
	 * @param request the request to be used for locale modification
	 * @param response the response to be used for locale modification
	 * @param localeContext the new locale context, or {@code null} to clear the locale
	 * @throws UnsupportedOperationException if the LocaleResolver implementation
	 * does not support dynamic changing of the locale or time zone
	 * @see #setLocale(HttpServletRequest, HttpServletResponse, Locale)
	 * @see org.springframework.context.i18n.SimpleLocaleContext
	 * @see org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext
	 */
	void setLocaleContext(HttpServletRequest request, @Nullable HttpServletResponse response,
			@Nullable LocaleContext localeContext);

	/**
	 * Default implementation of {@link LocaleResolver#resolveLocale(HttpServletRequest)}
	 * that delegates to {@link #resolveLocaleContext(HttpServletRequest)}, falling
	 * back to {@link HttpServletRequest#getLocale()} if necessary.
	 * @param request the request to resolve the locale for
	 * @return the current locale (never {@code null})
	 * @since 6.0
	 */
	@Override
	default Locale resolveLocale(HttpServletRequest request) {
		Locale locale = resolveLocaleContext(request).getLocale();
		return (locale != null ? locale : request.getLocale());
	}

	/**
	 * Default implementation of {@link LocaleResolver#setLocale(HttpServletRequest,
	 * HttpServletResponse, Locale)} that delegates to
	 * {@link #setLocaleContext(HttpServletRequest, HttpServletResponse, LocaleContext)},
	 * using a {@link SimpleLocaleContext}.
	 * @param request the request to be used for locale modification
	 * @param response the response to be used for locale modification
	 * @param locale the new locale, or {@code null} to clear the locale
	 * @throws UnsupportedOperationException if the LocaleResolver implementation
	 * does not support dynamic changing of the locale
	 * @since 6.0
	 */
	@Override
	default void setLocale(HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Locale locale) {
		setLocaleContext(request, response, (locale != null ? new SimpleLocaleContext(locale) : null));
	}

}
