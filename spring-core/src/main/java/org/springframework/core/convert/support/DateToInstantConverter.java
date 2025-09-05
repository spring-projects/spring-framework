/*
 * Copyright 2002-present the original author or authors.
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
