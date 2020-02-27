/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.server.i18n;

import java.util.Locale;
import java.util.TimeZone;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.lang.Nullable;
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

	@Nullable
	private final TimeZone timeZone;


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
			@Nullable
			public TimeZone getTimeZone() {
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
