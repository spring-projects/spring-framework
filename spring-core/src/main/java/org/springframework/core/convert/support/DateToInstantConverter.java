/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert.support;

import java.time.Instant;
import java.util.Date;

import org.springframework.core.convert.converter.Converter;

/**
 * Convert a {@link java.util.Date} to a {@link java.time.Instant}.
 *
 * <p>This includes conversion support for {@link java.sql.Timestamp} and other
 * subtypes of {@code java.util.Date}. Note, however, that an attempt to convert
 * a {@link java.sql.Date} or {@link java.sql.Time} to a {@code java.time.Instant}
 * results in an {@link UnsupportedOperationException} since those types do not
 * have time or date components, respectively.
 *
 * @author Sam Brannen
 * @since 6.2.9
 * @see Date#toInstant()
 * @see InstantToDateConverter
 */
final class DateToInstantConverter implements Converter<Date, Instant> {

	@Override
	public Instant convert(Date date) {
		return date.toInstant();
	}

}
