/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.format.datetime.joda;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.ReadableInstant;
import org.joda.time.ReadablePartial;
import org.joda.time.format.DateTimeFormatter;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringValueResolver;

/**
 * Formats fields annotated with the {@link DateTimeFormat} annotation using Joda-Time.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 * @see DateTimeFormat
 */
public class JodaDateTimeFormatAnnotationFormatterFactory
		implements AnnotationFormatterFactory<DateTimeFormat>, EmbeddedValueResolverAware {

	private static final Set<Class<?>> FIELD_TYPES;
	static {
		// Create the set of field types that may be annotated with @DateTimeFormat.
		// Note: the 3 ReadablePartial concrete types are registered explicitly since
		// addFormatterForFieldType rules exist for each of these types
		// (if we did not do this, the default byType rules for LocalDate, LocalTime,
		// and LocalDateTime would take precedence over the annotation rule, which
		// is not what we want)
		Set<Class<?>> fieldTypes = new HashSet<Class<?>>(7);
		fieldTypes.add(ReadableInstant.class);
		fieldTypes.add(LocalDate.class);
		fieldTypes.add(LocalTime.class);
		fieldTypes.add(LocalDateTime.class);
		fieldTypes.add(Date.class);
		fieldTypes.add(Calendar.class);
		fieldTypes.add(Long.class);
		FIELD_TYPES = Collections.unmodifiableSet(fieldTypes);
	}


	private StringValueResolver embeddedValueResolver;


	public final Set<Class<?>> getFieldTypes() {
		return FIELD_TYPES;
	}

	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}

	protected String resolveEmbeddedValue(String value) {
		return (this.embeddedValueResolver != null ? this.embeddedValueResolver.resolveStringValue(value) : value);
	}

	public Printer<?> getPrinter(DateTimeFormat annotation, Class<?> fieldType) {
		DateTimeFormatter formatter = getFormatter(annotation, fieldType);
		if (ReadableInstant.class.isAssignableFrom(fieldType)) {
			return new ReadableInstantPrinter(formatter);
		}
		if (ReadablePartial.class.isAssignableFrom(fieldType)) {
			return new ReadablePartialPrinter(formatter);
		}
		if (Calendar.class.isAssignableFrom(fieldType)) {
			// assumes Calendar->ReadableInstant converter is registered
			return new ReadableInstantPrinter(formatter);
		}
		// assumes Date->Long converter is registered
		return new MillisecondInstantPrinter(formatter);
	}

	public Parser<DateTime> getParser(DateTimeFormat annotation, Class<?> fieldType) {
		return new DateTimeParser(getFormatter(annotation, fieldType));
	}

	/**
	 * Factory method used to create a {@link DateTimeFormatter}.
	 * @param annotation the format annotation for the field
	 * @param fieldType the type of field
	 * @return a {@link DateTimeFormatter} instance
	 * @since 3.2
	 */
	protected DateTimeFormatter getFormatter(DateTimeFormat annotation, Class<?> fieldType) {
		DateTimeFormatterFactory factory = new DateTimeFormatterFactory();
		factory.setStyle(resolveEmbeddedValue(annotation.style()));
		factory.setIso(annotation.iso());
		factory.setPattern(resolveEmbeddedValue(annotation.pattern()));
		return factory.createDateTimeFormatter();
	}

}
