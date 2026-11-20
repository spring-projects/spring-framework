/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert.support;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;

/**
 * Converts from a String to a {@link java.util.Locale}.
 *
 * <p>Accepts the classic {@link Locale} String format ({@link Locale#toString()})
 * as well as BCP 47 language tags ({@link Locale#forLanguageTag}.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 * @see StringUtils#parseLocale
 */
final class StringToLocaleConverter implements Converter<String, @Nullable Locale> {

	@Override
	public @Nullable Locale convert(String source) {
		return StringUtils.parseLocale(source);
	}

}
