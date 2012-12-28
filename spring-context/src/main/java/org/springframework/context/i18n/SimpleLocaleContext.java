/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.context.i18n;

import java.util.Locale;

import org.springframework.util.Assert;

/**
 * Simple implementation of the {@link LocaleContext} interface,
 * always returning a specified {@code Locale}.
 *
 * @author Juergen Hoeller
 * @since 1.2
 */
public class SimpleLocaleContext implements LocaleContext {

	private final Locale locale;


	/**
	 * Create a new SimpleLocaleContext that exposes the specified Locale.
	 * Every {@code getLocale()} will return this Locale.
	 * @param locale the Locale to expose
	 */
	public SimpleLocaleContext(Locale locale) {
		Assert.notNull(locale, "Locale must not be null");
		this.locale = locale;
	}

	public Locale getLocale() {
		return this.locale;
	}

	@Override
	public String toString() {
		return this.locale.toString();
	}

}
