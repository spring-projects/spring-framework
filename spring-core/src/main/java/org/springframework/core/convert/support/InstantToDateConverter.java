/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert.support;

import java.time.Instant;
import java.util.Date;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.Converter;

/**
 * Convert a {@link java.time.Instant} to a {@link java.util.Date}.
 *
 * <p>This does not include conversion support for target types which are subtypes
 * of {@code java.util.Date}.
 *
 * @author Sam Brannen
 * @since 6.2.9
 * @see Date#from(Instant)
 * @see DateToInstantConverter
 */
final class InstantToDateConverter implements ConditionalConverter, Converter<Instant, Date> {

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return targetType.getType().equals(Date.class);
	}

	@Override
	public Date convert(Instant instant) {
		return Date.from(instant);
	}

}
