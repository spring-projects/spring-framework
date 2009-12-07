/*
 * Copyright 2002-2009 the original author or authors.
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

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.ReadableInstant;
import org.joda.time.ReadablePartial;
import org.joda.time.format.DateTimeFormatter;

import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.util.StringUtils;

/**
 * Formats fields annotated with the {@link DateTimeFormat} annotation.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 * @see DateTimeFormat
 */
public final class JodaDateTimeFormatAnnotationFormatterFactory implements AnnotationFormatterFactory<DateTimeFormat> {

	private final Set<Class<?>> fieldTypes;
	

	public JodaDateTimeFormatAnnotationFormatterFactory() {
		Set<Class<?>> rawFieldTypes = new HashSet<Class<?>>(8);
		rawFieldTypes.add(LocalDate.class);
		rawFieldTypes.add(LocalTime.class);
		rawFieldTypes.add(LocalDateTime.class);
		rawFieldTypes.add(DateTime.class);
		rawFieldTypes.add(DateMidnight.class);
		rawFieldTypes.add(Date.class);
		rawFieldTypes.add(Calendar.class);
		rawFieldTypes.add(Long.class);
		this.fieldTypes = Collections.unmodifiableSet(rawFieldTypes);
	}

	public Set<Class<?>> getFieldTypes() {
		return this.fieldTypes;
	}


	public Printer<?> getPrinter(DateTimeFormat annotation, Class<?> fieldType) {
		DateTimeFormatter formatter = configureDateTimeFormatterFrom(annotation);		
		if (ReadableInstant.class.isAssignableFrom(fieldType)) {
			return new ReadableInstantPrinter(formatter);
		}
		else if (ReadablePartial.class.isAssignableFrom(fieldType)) {
			return new ReadablePartialPrinter(formatter);
		}
		else if (Calendar.class.isAssignableFrom(fieldType)) {
			// assumes Calendar->ReadableInstant converter is registered
			return new ReadableInstantPrinter(formatter);			
		}
		else {
			// assumes Date->Long converter is registered
			return new MillisecondInstantPrinter(formatter);
		}		
	}

	public Parser<DateTime> getParser(DateTimeFormat annotation, Class<?> fieldType) {
		return new DateTimeParser(configureDateTimeFormatterFrom(annotation));				
	}


	// internal helpers
	
	private DateTimeFormatter configureDateTimeFormatterFrom(DateTimeFormat annotation) {
		if (StringUtils.hasLength(annotation.pattern())) {
			return forPattern(annotation.pattern());
		}
		else if (annotation.iso() != ISO.NONE) {
			return forIso(annotation.iso());
		}
		else {
			return forStyle(annotation.style());
		}
	}

	private DateTimeFormatter forPattern(String pattern) {
		return org.joda.time.format.DateTimeFormat.forPattern(pattern);
	}
	
	private DateTimeFormatter forIso(ISO iso) {
		if (iso == ISO.DATE) {
			return org.joda.time.format.ISODateTimeFormat.date();
		}
		else if (iso == ISO.TIME) {
			return org.joda.time.format.ISODateTimeFormat.time();
		}
		else {
			return org.joda.time.format.ISODateTimeFormat.dateTime();
		}		
	}

	private DateTimeFormatter forStyle(String style) {
		return org.joda.time.format.DateTimeFormat.forStyle(style);
	}

}
