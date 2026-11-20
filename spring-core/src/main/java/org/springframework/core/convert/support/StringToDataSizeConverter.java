/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert.support;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.unit.DataSize;

/**
 * Converts from a {@link String} to a {@link DataSize}.
 *
 * @author YeongJae Min
 * @since 7.1
 * @see DataSize#parse(CharSequence)
 * @see NumberToDataSizeConverter
 */
final class StringToDataSizeConverter implements Converter<String, @Nullable DataSize> {

	@Override
	public @Nullable DataSize convert(String source) {
		if (source.isEmpty()) {
			return null;
		}
		return DataSize.parse(source);
	}

}
