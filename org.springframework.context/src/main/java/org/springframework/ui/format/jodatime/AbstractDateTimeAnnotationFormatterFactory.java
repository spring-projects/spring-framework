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
package org.springframework.ui.format.jodatime;

import java.lang.annotation.Annotation;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;
import org.joda.time.ReadablePartial;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.ui.format.AnnotationFormatterFactory;
import org.springframework.ui.format.Parser;
import org.springframework.ui.format.Printer;

/**
 * Base class for annotation-based Joda DateTime formatters.
 * Can format any Joda {@link ReadableInstant} or {@link ReadablePartial} property, as well as standard {@link Date}, {@link Calendar}, and Long properties.
 * @author Keith Donald
 * @param <A> the format annotation parameter type to be declared by subclasses
 */
abstract class AbstractDateTimeAnnotationFormatterFactory<A extends Annotation> implements AnnotationFormatterFactory<A> {

	private final Set<Class<?>> propertyTypes;
	
	public AbstractDateTimeAnnotationFormatterFactory() {
		this.propertyTypes = Collections.unmodifiableSet(createPropertyTypes());
	}

	public Set<Class<?>> getFieldTypes() {
		return propertyTypes;
	}

	public Printer<?> getPrinter(A annotation, Class<?> propertyType) {
		DateTimeFormatter formatter = configureDateTimeFormatterFrom(annotation);		
		if (ReadableInstant.class.isAssignableFrom(propertyType)) {
			return new ReadableInstantPrinter(formatter);
		} else if (ReadablePartial.class.equals(propertyType)) {
			return new ReadablePartialPrinter(formatter);
		} else if (Calendar.class.isAssignableFrom(propertyType)) {
			// assumes Calendar->ReadableInstant converter is registered
			return new ReadableInstantPrinter(formatter);			
		} else {
			// assumes Date->Long converter is registered
			return new MillisecondInstantPrinter(formatter);
		}		
	}

	public Parser<DateTime> getParser(A annotation, Class<?> propertyType) {
		return new DateTimeParser(configureDateTimeFormatterFrom(annotation));				
	}

	/**
	 * Hook method subclasses should override to configure the Joda {@link DateTimeFormatter} from the <code>annotation</code> values.
	 * @param annotation the annotation containing formatter configuration rules
	 * @return the configured date time formatter
	 */
	protected abstract DateTimeFormatter configureDateTimeFormatterFrom(A annotation);

	// internal helpers
	
	private Set<Class<?>> createPropertyTypes() {
		Set<Class<?>> propertyTypes = new HashSet<Class<?>>(5);
		propertyTypes.add(ReadableInstant.class);
		propertyTypes.add(ReadablePartial.class);
		propertyTypes.add(Date.class);
		propertyTypes.add(Calendar.class);
		propertyTypes.add(Long.class);
		return propertyTypes;
	}

}
