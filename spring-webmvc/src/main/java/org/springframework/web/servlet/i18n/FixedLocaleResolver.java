/*
 * Copyright 2002-2012 the original author or authors.
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link org.springframework.web.servlet.LocaleResolver} implementation
 * that always returns a fixed default locale. Default is the current
 * JVM's default locale.
 *
 * <p>Note: Does not support {@code setLocale}, as the fixed locale
 * cannot be changed.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setDefaultLocale
 */
public class FixedLocaleResolver extends AbstractLocaleResolver {

	/**
	 * Create a default FixedLocaleResolver, exposing a configured default
	 * locale (or the JVM's default locale as fallback).
	 * @see #setDefaultLocale
	 */
	public FixedLocaleResolver() {
	}

	/**
	 * Create a FixedLocaleResolver that exposes the given locale.
	 * @param locale the locale to expose
	 */
	public FixedLocaleResolver(Locale locale) {
		setDefaultLocale(locale);
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
	public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
		throw new UnsupportedOperationException(
				"Cannot change fixed locale - use a different locale resolution strategy");
	}

}
