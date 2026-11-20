/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert.support;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;

/**
 * Converts from a String to a {@link java.util.UUID}.
 *
 * @author Phillip Webb
 * @since 3.2
 * @see UUID#fromString
 */
final class StringToUUIDConverter implements Converter<String, @Nullable UUID> {

	@Override
	public @Nullable UUID convert(String source) {
		return (StringUtils.hasText(source) ? UUID.fromString(source.trim()) : null);
	}

}
