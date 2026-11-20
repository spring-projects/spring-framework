/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert.support;

import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.converter.Converter;

/**
 * Converts from a String to a {@link java.util.regex.Pattern}.
 *
 * @author Valery Yatsynovich
 * @author Stephane Nicoll
 * @since 6.1
 */
final class StringToPatternConverter implements Converter<String, @Nullable Pattern> {

	@Override
	public @Nullable Pattern convert(String source) {
		if (source.isEmpty()) {
			return null;
		}
		return Pattern.compile(source);
	}

}
