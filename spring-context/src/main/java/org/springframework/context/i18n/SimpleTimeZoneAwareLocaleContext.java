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

package org.springframework.context.i18n;

import java.util.Locale;
import java.util.TimeZone;

import org.springframework.lang.Nullable;

/**
 * Simple implementation of the {@link TimeZoneAwareLocaleContext} interface,
 * always returning a specified {@code Locale} and {@code TimeZone}.
 *
 * <p>Note: Prefer the use of {@link SimpleLocaleContext} when only setting
 * a Locale but no TimeZone.
 *
 * @author Juergen Hoeller
 * @author Nicholas Williams
 * @since 4.0
 * @see LocaleContextHolder#setLocaleContext
 * @see LocaleContextHolder#getTimeZone()
 */
public class SimpleTimeZoneAwareLocaleContext extends SimpleLocaleContext implements TimeZoneAwareLocaleContext {

	@Nullable
	private final TimeZone timeZone;


	/**
	 * Create a new SimpleTimeZoneAwareLocaleContext that exposes the specified
	 * Locale and TimeZone. Every {@link #getLocale()} call will return the given
	 * Locale, and every {@link #getTimeZone()} call will return the given TimeZone.
	 * @param locale the Locale to expose
	 * @param timeZone the TimeZone to expose
	 */
	public SimpleTimeZoneAwareLocaleContext(@Nullable Locale locale, @Nullable TimeZone timeZone) {
		super(locale);
		this.timeZone = timeZone;
	}


	@Override
	@Nullable
	public TimeZone getTimeZone() {
		return this.timeZone;
	}

	@Override
	public String toString() {
		return super.toString() + " " + (this.timeZone != null ? this.timeZone : "-");
	}

}
