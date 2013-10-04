/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.convert.support;

import java.time.ZoneId;
import java.util.TimeZone;

import org.springframework.core.convert.converter.Converter;

/**
 * Simple Converter from {@link java.util.TimeZone} to Java 8's {@link java.time.ZoneId}.
 *
 * <p>Note that Spring's default ConversionService setup understands the 'of' convention that
 * the JSR-310 {@code java.time} package consistently uses. That convention is implemented
 * reflectively in {@link ObjectToObjectConverter}, not in specific JSR-310 converters.
 *
 * @author Juergen Hoeller
 * @since 4.0
 * @see ZoneIdToTimeZoneConverter
 */
class TimeZoneToZoneIdConverter implements Converter<TimeZone, ZoneId> {

	@Override
	public ZoneId convert(TimeZone source) {
		return source.toZoneId();
	}

}
