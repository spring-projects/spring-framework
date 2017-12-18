/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.server.i18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@link LocaleContextResolver} implementation that simply uses the primary locale
 * specified in the "Accept-Language" header of the HTTP request (that is,
 * the locale sent by the client browser, normally that of the client's OS).
 *
 * <p>Note: Does not support {@code setLocale}, since the accept header
 * can only be changed through changing the client's locale settings.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class AcceptHeaderLocaleContextResolver implements LocaleContextResolver {

	private final List<Locale> supportedLocales = new ArrayList<>(4);

	@Nullable
	private Locale defaultLocale;


	/**
	 * Configure supported locales to check against the requested locales
	 * determined via {@link HttpHeaders#getAcceptLanguageAsLocales()}.
	 * @param locales the supported locales
	 */
	public void setSupportedLocales(@Nullable List<Locale> locales) {
		this.supportedLocales.clear();
		if (locales != null) {
			this.supportedLocales.addAll(locales);
		}
	}

	/**
	 * Return the configured list of supported locales.
	 */
	public List<Locale> getSupportedLocales() {
		return this.supportedLocales;
	}

	/**
	 * Configure a fixed default locale to fall back on if the request does not
	 * have an "Accept-Language" header (not set by default).
	 * @param defaultLocale the default locale to use
	 */
	public void setDefaultLocale(@Nullable Locale defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

	/**
	 * The configured default locale, if any.
	 */
	@Nullable
	public Locale getDefaultLocale() {
		return this.defaultLocale;
	}

	@Override
	public LocaleContext resolveLocaleContext(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		List<Locale> acceptableLocales = request.getHeaders().getAcceptLanguageAsLocales();
		if (this.defaultLocale != null && acceptableLocales.isEmpty()) {
			return new SimpleLocaleContext(this.defaultLocale);
		}
		Locale requestLocale = acceptableLocales.isEmpty() ? null : acceptableLocales.get(0);
		if (isSupportedLocale(requestLocale)) {
			return new SimpleLocaleContext(requestLocale);
		}
		Locale supportedLocale = findSupportedLocale(request);
		if (supportedLocale != null) {
			return new SimpleLocaleContext(supportedLocale);
		}
		return (this.defaultLocale != null ? new SimpleLocaleContext(this.defaultLocale) :
				new SimpleLocaleContext(requestLocale));
	}

	private boolean isSupportedLocale(@Nullable Locale locale) {
		if (locale == null) {
			return false;
		}
		List<Locale> supportedLocales = getSupportedLocales();
		return (supportedLocales.isEmpty() || supportedLocales.contains(locale));
	}

	@Nullable
	private Locale findSupportedLocale(ServerHttpRequest request) {
		List<Locale> requestLocales = request.getHeaders().getAcceptLanguageAsLocales();
		for (Locale locale : requestLocales) {
			if (getSupportedLocales().contains(locale)) {
				return locale;
			}
		}
		return null;
	}

	@Override
	public void setLocaleContext(ServerWebExchange exchange, @Nullable LocaleContext locale) {
		throw new UnsupportedOperationException(
				"Cannot change HTTP accept header - use a different locale context resolution strategy");
	}

}
