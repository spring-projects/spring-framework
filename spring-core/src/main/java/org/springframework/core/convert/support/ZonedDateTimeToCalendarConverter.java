/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.convert.support;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.UsesJava8;

/**
 * Simple converter from Java 8's {@link java.time.ZonedDateTime} to {@link java.util.Calendar}.
 *
 * <p>Note that Spring's default ConversionService setup understands the 'from'/'to' convention
 * that the JSR-310 {@code java.time} package consistently uses. That convention is implemented
 * reflectively in {@link ObjectToObjectConverter}, not in specific JSR-310 converters.
 * It covers {@link java.util.GregorianCalendar#toZonedDateTime()} as well, and also
 * {@link java.util.Date#from(java.time.Instant)} and {@link java.util.Date#toInstant()}.
 *
 * @author Juergen Hoeller
 * @since 4.0.1
 * @see java.util.GregorianCalendar#from(java.time.ZonedDateTime)
 */
@UsesJava8
final class ZonedDateTimeToCalendarConverter implements Converter<ZonedDateTime, Calendar> {

	@Override
	public Calendar convert(ZonedDateTime source) {
		return GregorianCalendar.from(source);
	}

}
