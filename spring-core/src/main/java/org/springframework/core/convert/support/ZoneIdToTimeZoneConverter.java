/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert.support;

import java.time.ZoneId;
import java.util.TimeZone;

import org.springframework.core.convert.converter.Converter;

/**
 * Simple converter from Java's {@link java.time.ZoneId} to {@link java.util.TimeZone}.
 *
 * <p>Note that Spring's default ConversionService setup understands the 'from'/'to' convention
 * that the JSR-310 {@code java.time} package consistently uses. That convention is implemented
 * reflectively in {@link ObjectToObjectConverter}, not in specific JSR-310 converters.
 * It covers {@link java.util.TimeZone#toZoneId()} as well, and also
 * {@link java.util.Date#from(java.time.Instant)} and {@link java.util.Date#toInstant()}.
 *
 * @author Juergen Hoeller
 * @since 4.0
 * @see TimeZone#getTimeZone(java.time.ZoneId)
 */
final class ZoneIdToTimeZoneConverter implements Converter<ZoneId, TimeZone> {

	@Override
	public TimeZone convert(ZoneId source) {
		return TimeZone.getTimeZone(source);
	}

}
