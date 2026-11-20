/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert.support;

import java.util.TimeZone;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;

/**
 * Convert a String to a {@link TimeZone}.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 4.2
 */
class StringToTimeZoneConverter implements Converter<String, TimeZone> {

	@Override
	public TimeZone convert(String source) {
		if (StringUtils.hasText(source)) {
			source = source.trim();
		}
		return StringUtils.parseTimeZoneString(source);
	}

}
