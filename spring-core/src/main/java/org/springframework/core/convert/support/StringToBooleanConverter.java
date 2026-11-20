/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert.support;

import java.util.Locale;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.converter.Converter;

/**
 * Converts a String to a Boolean.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
final class StringToBooleanConverter implements Converter<String, @Nullable Boolean> {

	private static final Set<String> trueValues = Set.of("true", "on", "yes", "1");

	private static final Set<String> falseValues = Set.of("false", "off", "no", "0");


	@Override
	public @Nullable Boolean convert(String source) {
		String value = source.trim();
		if (value.isEmpty()) {
			return null;
		}
		value = value.toLowerCase(Locale.ROOT);
		if (trueValues.contains(value)) {
			return Boolean.TRUE;
		}
		else if (falseValues.contains(value)) {
			return Boolean.FALSE;
		}
		else {
			throw new IllegalArgumentException("Invalid boolean value '" + source + "'");
		}
	}

}
