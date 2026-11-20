/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert.support;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.converter.Converter;

/**
 * Converts a String to a Character.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class StringToCharacterConverter implements Converter<String, @Nullable Character> {

	@Override
	public @Nullable Character convert(String source) {
		if (source.isEmpty()) {
			return null;
		}
		if (source.length() > 1) {
			throw new IllegalArgumentException(
					"Can only convert a [String] with length of 1 to a [Character]; string value '" + source + "' has length of " + source.length());
		}
		return source.charAt(0);
	}

}
