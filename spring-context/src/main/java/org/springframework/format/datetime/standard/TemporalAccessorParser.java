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

package org.springframework.format.datetime.standard;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import org.springframework.format.Parser;
import org.springframework.util.ObjectUtils;

/**
 * {@link Parser} implementation for a JSR-310 {@link java.time.temporal.TemporalAccessor},
 * using a {@link java.time.format.DateTimeFormatter} (the contextual one, if available).
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Kazuki Shimizu
 * @since 4.0
 * @see DateTimeContextHolder#getFormatter
 * @see java.time.LocalDate#parse(CharSequence, java.time.format.DateTimeFormatter)
 * @see java.time.LocalTime#parse(CharSequence, java.time.format.DateTimeFormatter)
 * @see java.time.LocalDateTime#parse(CharSequence, java.time.format.DateTimeFormatter)
 * @see java.time.ZonedDateTime#parse(CharSequence, java.time.format.DateTimeFormatter)
 * @see java.time.OffsetDateTime#parse(CharSequence, java.time.format.DateTimeFormatter)
 * @see java.time.OffsetTime#parse(CharSequence, java.time.format.DateTimeFormatter)
 * @see java.time.YearMonth#parse(CharSequence, java.time.format.DateTimeFormatter)
 * @see java.time.MonthDay#parse(CharSequence, java.time.format.DateTimeFormatter)
 */
public final class TemporalAccessorParser implements Parser<TemporalAccessor> {

	private final Class<? extends TemporalAccessor> temporalAccessorType;

	private final DateTimeFormatter formatter;

	private final String @Nullable [] fallbackPatterns;

	private final @Nullable Object source;


	/**
	 * Create a new TemporalAccessorParser for the given TemporalAccessor type.
	 * @param temporalAccessorType the specific TemporalAccessor class
	 * (LocalDate, LocalTime, LocalDateTime, ZonedDateTime, OffsetDateTime, OffsetTime)
	 * @param formatter the base DateTimeFormatter instance
	 */
	public TemporalAccessorParser(Class<? extends TemporalAccessor> temporalAccessorType, DateTimeFormatter formatter) {
		this(temporalAccessorType, formatter, null, null);
	}

	TemporalAccessorParser(Class<? extends TemporalAccessor> temporalAccessorType, DateTimeFormatter formatter,
			String @Nullable [] fallbackPatterns, @Nullable Object source) {

		this.temporalAccessorType = temporalAccessorType;
		this.formatter = formatter;
		this.fallbackPatterns = fallbackPatterns;
		this.source = source;
	}


	@Override
	public TemporalAccessor parse(String text, Locale locale) throws ParseException {
		try {
			return doParse(text, locale, this.formatter);
		}
		catch (DateTimeParseException ex) {
			if (!ObjectUtils.isEmpty(this.fallbackPatterns)) {
				for (String pattern : this.fallbackPatterns) {
					try {
						DateTimeFormatter fallbackFormatter = DateTimeFormatterUtils.createStrictDateTimeFormatter(pattern);
						return doParse(text, locale, fallbackFormatter);
					}
					catch (DateTimeParseException ignoredException) {
						// Ignore fallback parsing exceptions since the exception thrown below
						// will include information from the "source" if available -- for example,
						// the toString() of a @DateTimeFormat annotation.
					}
				}
			}
			else {
				// Fallback to ISO-based default java.time type parsing
				try {
					return defaultParse(text);
				}
				catch (DateTimeParseException ignoredException) {
					// Ignore fallback parsing exception like above
				}
			}
			if (this.source != null) {
				throw new DateTimeParseException(
						String.format("Unable to parse date time value \"%s\" using configuration from %s", text, this.source),
						text, ex.getErrorIndex(), ex);
			}
			// else rethrow original exception
			throw ex;
		}
	}

	private TemporalAccessor doParse(String text, Locale locale, DateTimeFormatter formatter) throws DateTimeParseException {
		DateTimeFormatter formatterToUse = DateTimeContextHolder.getFormatter(formatter, locale);
		if (Instant.class == this.temporalAccessorType) {
			return formatterToUse.parse(text, Instant::from);
		}
		else if (LocalDate.class == this.temporalAccessorType) {
			return LocalDate.parse(text, formatterToUse);
		}
		else if (LocalTime.class == this.temporalAccessorType) {
			return LocalTime.parse(text, formatterToUse);
		}
		else if (LocalDateTime.class == this.temporalAccessorType) {
			return LocalDateTime.parse(text, formatterToUse);
		}
		else if (ZonedDateTime.class == this.temporalAccessorType) {
			return ZonedDateTime.parse(text, formatterToUse);
		}
		else if (OffsetDateTime.class == this.temporalAccessorType) {
			return OffsetDateTime.parse(text, formatterToUse);
		}
		else if (OffsetTime.class == this.temporalAccessorType) {
			return OffsetTime.parse(text, formatterToUse);
		}
		else if (YearMonth.class == this.temporalAccessorType) {
			return YearMonth.parse(text, formatterToUse);
		}
		else if (MonthDay.class == this.temporalAccessorType) {
			return MonthDay.parse(text, formatterToUse);
		}
		else {
			throw new IllegalStateException("Unsupported TemporalAccessor type: " + this.temporalAccessorType);
		}
	}

	private TemporalAccessor defaultParse(String text) throws DateTimeParseException {
		if (Instant.class == this.temporalAccessorType) {
			return Instant.parse(text);
		}
		else if (LocalDate.class == this.temporalAccessorType) {
			return LocalDate.parse(text);
		}
		else if (LocalTime.class == this.temporalAccessorType) {
			return LocalTime.parse(text);
		}
		else if (LocalDateTime.class == this.temporalAccessorType) {
			return LocalDateTime.parse(text);
		}
		else if (ZonedDateTime.class == this.temporalAccessorType) {
			return ZonedDateTime.parse(text);
		}
		else if (OffsetDateTime.class == this.temporalAccessorType) {
			return OffsetDateTime.parse(text);
		}
		else if (OffsetTime.class == this.temporalAccessorType) {
			return OffsetTime.parse(text);
		}
		else if (YearMonth.class == this.temporalAccessorType) {
			return YearMonth.parse(text);
		}
		else if (MonthDay.class == this.temporalAccessorType) {
			return MonthDay.parse(text);
		}
		else {
			throw new IllegalStateException("Unsupported TemporalAccessor type: " + this.temporalAccessorType);
		}
	}

}
