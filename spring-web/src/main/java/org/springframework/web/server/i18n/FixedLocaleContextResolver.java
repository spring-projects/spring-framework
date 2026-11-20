/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.server.i18n;

import java.util.Locale;
import java.util.TimeZone;

import org.jspecify.annotations.Nullable;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@link LocaleContextResolver} implementation that always returns a fixed locale
 * and optionally time zone. Default is the current JVM's default locale.
 *
 * <p>Note: Does not support {@link #setLocaleContext}, as the fixed locale and
 * time zone cannot be changed.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class FixedLocaleContextResolver implements LocaleContextResolver {

	private final Locale locale;

	private final @Nullable TimeZone timeZone;


	/**
	 * Create a default FixedLocaleResolver, exposing a configured default
	 * locale (or the JVM's default locale as fallback).
	 */
	public FixedLocaleContextResolver() {
		this(Locale.getDefault());
	}

	/**
	 * Create a FixedLocaleResolver that exposes the given locale.
	 * @param locale the locale to expose
	 */
	public FixedLocaleContextResolver(Locale locale) {
		this(locale, null);
	}

	/**
	 * Create a FixedLocaleResolver that exposes the given locale and time zone.
	 * @param locale the locale to expose
	 * @param timeZone the time zone to expose
	 */
	public FixedLocaleContextResolver(Locale locale, @Nullable TimeZone timeZone) {
		Assert.notNull(locale, "Locale must not be null");
		this.locale = locale;
		this.timeZone = timeZone;
	}


	@Override
	public LocaleContext resolveLocaleContext(ServerWebExchange exchange) {
		return new TimeZoneAwareLocaleContext() {
			@Override
			public Locale getLocale() {
				return locale;
			}
			@Override
			public @Nullable TimeZone getTimeZone() {
				return timeZone;
			}
		};
	}

	@Override
	public void setLocaleContext(ServerWebExchange exchange, @Nullable LocaleContext localeContext) {
		throw new UnsupportedOperationException(
				"Cannot change fixed locale - use a different locale context resolution strategy");
	}

}
