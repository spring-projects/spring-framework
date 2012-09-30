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

package org.springframework.format.datetime;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Formatter;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.datetime.joda.JodaDateTimeFormatAnnotationFormatterFactory;
import org.springframework.util.StringValueResolver;

/**
 * Formats fields annotated with the {@link DateTimeFormat} annotation.
 *
 * @author Phillip Webb
 * @see JodaDateTimeFormatAnnotationFormatterFactory
 * @since 3.2
 */
public class DateTimeFormatAnnotationFormatterFactory implements
		AnnotationFormatterFactory<DateTimeFormat>, EmbeddedValueResolverAware {


	private static final Set<Class<?>> FIELD_TYPES;
	static {
		Set<Class<?>> fieldTypes = new HashSet<Class<?>>();
		fieldTypes.add(Date.class);
		fieldTypes.add(Calendar.class);
		fieldTypes.add(Long.class);
		FIELD_TYPES = Collections.unmodifiableSet(fieldTypes);
	}


	private StringValueResolver embeddedValueResolver;


	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}

	public Set<Class<?>> getFieldTypes() {
		return FIELD_TYPES;
	}

	public Printer<?> getPrinter(DateTimeFormat annotation, Class<?> fieldType) {
		return getFormatter(annotation, fieldType);
	}

	public Parser<?> getParser(DateTimeFormat annotation, Class<?> fieldType) {
		return getFormatter(annotation, fieldType);
	}

	protected Formatter<Date> getFormatter(DateTimeFormat annotation, Class<?> fieldType) {
		DateFormatter formatter = new DateFormatter();
		formatter.setStylePattern(resolveEmbeddedValue(annotation.style()));
		formatter.setIso(annotation.iso());
		formatter.setPattern(resolveEmbeddedValue(annotation.pattern()));
		return formatter;
	}

	protected String resolveEmbeddedValue(String value) {
		return (this.embeddedValueResolver != null ? this.embeddedValueResolver.resolveStringValue(value) : value);
	}
}
