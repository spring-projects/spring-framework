/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.server.i18n;

import org.jspecify.annotations.Nullable;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.web.server.ServerWebExchange;

/**
 * Interface for web-based locale context resolution strategies that allows
 * for both locale context resolution via the request and locale context modification
 * via the HTTP exchange.
 *
 * <p>The {@link org.springframework.context.i18n.LocaleContext} object can potentially
 * include associated time zone and other locale related information.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 * @see LocaleContext
 */
public interface LocaleContextResolver {

	/**
	 * Resolve the current locale context via the given exchange.
	 * <p>The returned context may be a
	 * {@link org.springframework.context.i18n.TimeZoneAwareLocaleContext},
	 * containing a locale with associated time zone information.
	 * Simply apply an {@code instanceof} check and downcast accordingly.
	 * <p>Custom resolver implementations may also return extra settings in
	 * the returned context, which again can be accessed through downcasting.
	 * @param exchange current server exchange
	 * @return the current locale context (never {@code null})
	 */
	LocaleContext resolveLocaleContext(ServerWebExchange exchange);

	/**
	 * Set the current locale context to the given one,
	 * potentially including a locale with associated time zone information.
	 * @param exchange current server exchange
	 * @param localeContext the new locale context, or {@code null} to clear the locale
	 * @throws UnsupportedOperationException if the LocaleResolver implementation
	 * does not support dynamic changing of the locale or time zone
	 * @see org.springframework.context.i18n.SimpleLocaleContext
	 * @see org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext
	 */
	void setLocaleContext(ServerWebExchange exchange, @Nullable LocaleContext localeContext);

}
