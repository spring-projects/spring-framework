/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert.support;

import kotlin.text.Regex;
import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.converter.Converter;

/**
 * Converts from a String to a Kotlin {@link Regex}.
 *
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 * @since 6.1
 */
final class StringToRegexConverter implements Converter<String, @Nullable Regex> {

	@Override
	public @Nullable Regex convert(String source) {
		if (source.isEmpty()) {
			return null;
		}
		return new Regex(source);
	}

}
