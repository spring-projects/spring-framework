/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert.support;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.unit.DataSize;

/**
 * Converts from a {@link Number} to a {@link DataSize}.
 *
 * @author YeongJae Min
 * @author Yanming Zhou
 * @since 7.1
 * @see DataSize#ofBytes(long)
 * @see StringToDataSizeConverter
 */
final class NumberToDataSizeConverter implements Converter<Number, DataSize> {

	@Override
	public DataSize convert(Number source) {
		long bytes = source.longValue();
		// Ensure Number is a whole number.
		if (source.doubleValue() - bytes != 0) {
			throw new IllegalArgumentException("'" + source + "' is not a valid data size");
		}
		return DataSize.ofBytes(bytes);
	}

}
