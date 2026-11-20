/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
