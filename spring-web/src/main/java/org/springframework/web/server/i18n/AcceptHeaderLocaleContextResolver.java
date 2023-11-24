/*
 * Copyright 2002-2023 the original author or authors.
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
 * {@link LocaleContextResolver} implementation that looks for a match between
 * locales in the {@code Accept-Language} header and a list of configured
 * supported locales.
 *
 * <p>See {@link #setSupportedLocales(List)} for further details on how
 * supported and requested locales are matched.
 *
 * <p>Note: Does not support {@link #setLocaleContext}, since the accept header
 * can only be changed through changing the client's locale settings.
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 5.0
 * @see HttpHeaders#getAcceptLanguageAsLocales()
 */
public class AcceptHeaderLocaleContextResolver implements LocaleContextResolver {

	private final List<Locale> supportedLocales = new ArrayList<>(4);

	@Nullable
	private Locale defaultLocale;


	/**
	 * Configure the list of supported locales to compare and match against
	 * {@link HttpHeaders#getAcceptLanguageAsLocales() requested locales}.
	 * <p>In order for a supported locale to be considered a match, it must match
	 * on both country and language. If you want to support a language-only match
	 * as a fallback, you must configure the language explicitly as a supported
	 * locale.
	 * <p>For example, if the supported locales are {@code ["de-DE","en-US"]},
	 * then a request for {@code "en-GB"} will not match, and neither will a
	 * request for {@code "en"}. If you want to support additional locales for a
	 * given language such as {@code "en"}, then you must add it to the list of
	 * supported locales.
	 * <p>If there is no match, then the {@link #setDefaultLocale(Locale)
	 * defaultLocale} is used, if configured, or otherwise falling back on
	 * the first requested locale.
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
	 * <p>This method may be overridden in subclasses.
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
			return getDefaultLocale();  // may be null
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
				for (Locale supportedLocale : supportedLocales) {
					if (!StringUtils.hasLength(supportedLocale.getCountry()) &&
							supportedLocale.getLanguage().equals(locale.getLanguage())) {
						languageMatch = supportedLocale;
						break;
					}
				}
			}
		}
		if (languageMatch != null) {
			return languageMatch;
		}

		Locale defaultLocale = getDefaultLocale();
		return (defaultLocale != null ? defaultLocale : requestLocales.get(0));
	}

	@Override
	public void setLocaleContext(ServerWebExchange exchange, @Nullable LocaleContext locale) {
		throw new UnsupportedOperationException(
				"Cannot change HTTP accept header - use a different locale context resolution strategy");
	}

}
