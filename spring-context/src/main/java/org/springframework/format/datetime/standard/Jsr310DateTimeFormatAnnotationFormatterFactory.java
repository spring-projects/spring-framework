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
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.context.support.EmbeddedValueResolutionSupport;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;

/**
 * Formats fields annotated with the {@link DateTimeFormat} annotation using the
 * JSR-310 <code>java.time</code> package in JDK 8.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Kazuki Shimizu
 * @since 4.0
 * @see org.springframework.format.annotation.DateTimeFormat
 */
public class Jsr310DateTimeFormatAnnotationFormatterFactory extends EmbeddedValueResolutionSupport
		implements AnnotationFormatterFactory<DateTimeFormat> {

	// Create the set of field types that may be annotated with @DateTimeFormat.
	private static final Set<Class<?>> FIELD_TYPES = Set.of(
				Instant.class,
				LocalDate.class,
				LocalTime.class,
				LocalDateTime.class,
				ZonedDateTime.class,
				OffsetDateTime.class,
				OffsetTime.class,
				YearMonth.class,
				MonthDay.class);

	@Override
	public final Set<Class<?>> getFieldTypes() {
		return FIELD_TYPES;
	}

	@Override
	public Printer<?> getPrinter(DateTimeFormat annotation, Class<?> fieldType) {
		DateTimeFormatter formatter = getFormatter(annotation, fieldType);

		// Efficient ISO_LOCAL_* variants for printing since they are twice as fast...
		if (formatter == DateTimeFormatter.ISO_DATE) {
			if (isLocal(fieldType)) {
				formatter = DateTimeFormatter.ISO_LOCAL_DATE;
			}
		}
		else if (formatter == DateTimeFormatter.ISO_TIME) {
			if (isLocal(fieldType)) {
				formatter = DateTimeFormatter.ISO_LOCAL_TIME;
			}
		}
		else if (formatter == DateTimeFormatter.ISO_DATE_TIME) {
			if (isLocal(fieldType)) {
				formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
			}
		}

		return new TemporalAccessorPrinter(formatter);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Parser<?> getParser(DateTimeFormat annotation, Class<?> fieldType) {
		DateTimeFormatter formatter = getFormatter(annotation, fieldType);

		List<String> resolvedFallbackPatterns = new ArrayList<>();
		for (String fallbackPattern : annotation.fallbackPatterns()) {
			String resolvedFallbackPattern = resolveEmbeddedValue(fallbackPattern);
			if (StringUtils.hasLength(resolvedFallbackPattern)) {
				resolvedFallbackPatterns.add(resolvedFallbackPattern);
			}
		}

		return new TemporalAccessorParser((Class<? extends TemporalAccessor>) fieldType,
				formatter, resolvedFallbackPatterns.toArray(new String[0]), annotation);
	}

	/**
	 * Factory method used to create a {@link DateTimeFormatter}.
	 * @param annotation the format annotation for the field
	 * @param fieldType the declared type of the field
	 * @return a {@link DateTimeFormatter} instance
	 */
	protected DateTimeFormatter getFormatter(DateTimeFormat annotation, Class<?> fieldType) {
		DateTimeFormatterFactory factory = new DateTimeFormatterFactory();
		String style = resolveEmbeddedValue(annotation.style());
		if (StringUtils.hasLength(style)) {
			factory.setStylePattern(style);
		}
		factory.setIso(annotation.iso());
		String pattern = resolveEmbeddedValue(annotation.pattern());
		if (StringUtils.hasLength(pattern)) {
			factory.setPattern(pattern);
		}
		return factory.createDateTimeFormatter();
	}

	private boolean isLocal(Class<?> fieldType) {
		return fieldType.getSimpleName().startsWith("Local");
	}

}
