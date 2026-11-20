/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.i18n;

import java.util.Locale;
import java.util.TimeZone;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;

/**
 * {@link org.springframework.web.servlet.LocaleResolver} implementation
 * that always returns a fixed default locale and optionally time zone.
 * Default is the current JVM's default locale.
 *
 * <p>Note: Does not support {@code setLocale(Context)}, as the fixed
 * locale and time zone cannot be changed.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setDefaultLocale
 * @see #setDefaultTimeZone
 */
public class FixedLocaleResolver extends AbstractLocaleContextResolver {

	/**
	 * Create a default FixedLocaleResolver, exposing a configured default
	 * locale (or the JVM's default locale as fallback).
	 * @see #setDefaultLocale
	 * @see #setDefaultTimeZone
	 */
	public FixedLocaleResolver() {
		setDefaultLocale(Locale.getDefault());
	}

	/**
	 * Create a FixedLocaleResolver that exposes the given locale.
	 * @param locale the locale to expose
	 */
	public FixedLocaleResolver(Locale locale) {
		setDefaultLocale(locale);
	}

	/**
	 * Create a FixedLocaleResolver that exposes the given locale and time zone.
	 * @param locale the locale to expose
	 * @param timeZone the time zone to expose
	 */
	public FixedLocaleResolver(Locale locale, TimeZone timeZone) {
		setDefaultLocale(locale);
		setDefaultTimeZone(timeZone);
	}


	@Override
	public Locale resolveLocale(HttpServletRequest request) {
		Locale locale = getDefaultLocale();
		if (locale == null) {
			locale = Locale.getDefault();
		}
		return locale;
	}

	@Override
	public LocaleContext resolveLocaleContext(HttpServletRequest request) {
		return new TimeZoneAwareLocaleContext() {
			@Override
			public @Nullable Locale getLocale() {
				return getDefaultLocale();
			}
			@Override
			public @Nullable TimeZone getTimeZone() {
				return getDefaultTimeZone();
			}
		};
	}

	@Override
	public void setLocaleContext( HttpServletRequest request, @Nullable HttpServletResponse response,
			@Nullable LocaleContext localeContext) {

		throw new UnsupportedOperationException("Cannot change fixed locale - use a different locale resolution strategy");
	}

}
