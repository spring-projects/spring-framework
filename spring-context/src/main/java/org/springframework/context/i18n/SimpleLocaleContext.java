/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.i18n;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * Simple implementation of the {@link LocaleContext} interface,
 * always returning a specified {@code Locale}.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see LocaleContextHolder#setLocaleContext
 * @see LocaleContextHolder#getLocale()
 * @see SimpleTimeZoneAwareLocaleContext
 */
public class SimpleLocaleContext implements LocaleContext {

	private final @Nullable Locale locale;


	/**
	 * Create a new {@code SimpleLocaleContext} that exposes the specified {@link Locale}.
	 * <p>Every {@link #getLocale()} call will return this locale.
	 * @param locale the {@code Locale} to expose, or {@code null} for no specific one
	 */
	public SimpleLocaleContext(@Nullable Locale locale) {
		this.locale = locale;
	}

	@Override
	public @Nullable Locale getLocale() {
		return this.locale;
	}

	@Override
	public String toString() {
		return (this.locale != null ? this.locale.toString() : "-");
	}

}
