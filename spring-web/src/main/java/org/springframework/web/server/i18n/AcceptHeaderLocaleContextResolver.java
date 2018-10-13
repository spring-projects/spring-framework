/*
 * Copyright 2002-2018 the original author or authors.
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
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@link LocaleContextResolver} implementation that simply uses the primary locale
 * specified in the "Accept-Language" header of the HTTP request (that is,
 * the locale sent by the client browser, normally that of the client's OS).
 *
 * <p>Note: Does not support {@link #setLocaleContext}, since the accept header
 * can only be changed through changing the client's locale settings.
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
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
	public void setSupportedLocales(List<Locale> locales) {
		this.supportedLocales.clear();
		this.supportedLocales.addAll(locales);
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
		List<Locale> requestLocales = null;
		try {
			requestLocales = exchange.getRequest().getHeaders().getAcceptLanguageAsLocales();
		}
		catch (IllegalArgumentException ex) {
			// Invalid Accept-Language header: treat as empty for matching purposes
		}
		return new SimpleLocaleContext(resolveSupportedLocale(requestLocales));
	}

	@Nullable
	private Locale resolveSupportedLocale(@Nullable List<Locale> requestLocales) {
		if (CollectionUtils.isEmpty(requestLocales)) {
			return this.defaultLocale;  // may be null
		}
		List<Locale> supportedLocales = getSupportedLocales();
		if (supportedLocales.isEmpty()) {
			return requestLocales.get(0);  // never null
		}

		Locale languageMatch = null;
		for (Locale locale : requestLocales) {
			if (supportedLocales.contains(locale)) {
				if (languageMatch == null || languageMatch.getLanguage().equals(locale.getLanguage())) {
					// Full match: language + country, possibly narrowed from earlier language-only match
					return locale;
				}
			}
			else if (languageMatch == null) {
				// Let's try to find a language-only match as a fallback
				for (Locale candidate : supportedLocales) {
					if (!StringUtils.hasLength(candidate.getCountry()) &&
							candidate.getLanguage().equals(locale.getLanguage())) {
						languageMatch = candidate;
						break;
					}
				}
			}
		}
		if (languageMatch != null) {
			return languageMatch;
		}

		return (this.defaultLocale != null ? this.defaultLocale : requestLocales.get(0));
	}

	@Override
	public void setLocaleContext(ServerWebExchange exchange, @Nullable LocaleContext locale) {
		throw new UnsupportedOperationException(
				"Cannot change HTTP accept header - use a different locale context resolution strategy");
	}

}
