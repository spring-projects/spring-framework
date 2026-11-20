/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.i18n;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import org.springframework.web.servlet.LocaleResolver;

/**
 * Abstract base class for {@link LocaleResolver} implementations.
 *
 * <p>Provides support for a {@linkplain #setDefaultLocale(Locale) default locale}.
 *
 * @author Juergen Hoeller
 * @since 1.2.9
 * @see #setDefaultLocale
 */
public abstract class AbstractLocaleResolver implements LocaleResolver {

	private @Nullable Locale defaultLocale;


	/**
	 * Set a default {@link Locale} that this resolver will return if no other
	 * locale is found.
	 */
	public void setDefaultLocale(@Nullable Locale defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

	/**
	 * Get the default {@link Locale} that this resolver is supposed to fall back
	 * to, if any.
	 */
	protected @Nullable Locale getDefaultLocale() {
		return this.defaultLocale;
	}

}
