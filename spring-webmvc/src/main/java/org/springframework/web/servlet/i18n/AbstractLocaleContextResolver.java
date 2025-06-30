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

import java.util.TimeZone;

import org.jspecify.annotations.Nullable;

import org.springframework.web.servlet.LocaleContextResolver;

/**
 * Abstract base class for {@link LocaleContextResolver} implementations.
 *
 * <p>Provides support for a {@linkplain #setDefaultLocale(java.util.Locale) default
 * locale} and a {@linkplain #setDefaultTimeZone(TimeZone) default time zone}.
 *
 * @author Juergen Hoeller
 * @since 4.0
 * @see #setDefaultLocale
 * @see #setDefaultTimeZone
 */
public abstract class AbstractLocaleContextResolver extends AbstractLocaleResolver implements LocaleContextResolver {

	private @Nullable TimeZone defaultTimeZone;


	/**
	 * Set a default {@link TimeZone} that this resolver will return if no other
	 * time zone is found.
	 */
	public void setDefaultTimeZone(@Nullable TimeZone defaultTimeZone) {
		this.defaultTimeZone = defaultTimeZone;
	}

	/**
	 * Get the default {@link TimeZone} that this resolver is supposed to fall
	 * back to, if any.
	 */
	public @Nullable TimeZone getDefaultTimeZone() {
		return this.defaultTimeZone;
	}

}
