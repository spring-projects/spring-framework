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
package org.springframework.ui.format;

import java.lang.annotation.Annotation;

import org.springframework.core.convert.converter.ConverterRegistry;

/**
 * A registry of field formatting logic.
 *
 * @author Keith Donald
 * @since 3.0
 */
public interface FormatterRegistry {

	/**
	 * Adds a Formatter to format fields of a specific type.
	 * The Formatter will delegate to the specified <code>printer</code> for printing and <code>parser</code> for parsing.
	 * <p>
	 * On print, if the Printer's type T is declared and <code>fieldType</code> is not assignable to T,
	 * a coersion to T will be attempted before delegating to <code>printer</code> to print a field value.
	 * On parse, if the object returned by the Parser is not assignable to the runtime field type,
	 * a coersion to the field type will be attempted before returning the parsed field value.
	 * @param fieldType the field type to format
	 * @param formatter the formatter to add
	 */
	void addFormatterForFieldType(Class<?> fieldType, Printer<?> printer, Parser<?> parser);

	/**
	 * Adds a Formatter to format fields of a specific type.
	 * <p>
	 * On print, if the Formatter's type T is declared and <code>fieldType</code> is not assignable to T,
	 * a coersion to T will be attempted before delegating to <code>formatter</code> to print a field value.
	 * On parse, if the parsed object returned by <code>formatter</code> is not assignable to the runtime field type,
	 * a coersion to the field type will be attempted before returning the parsed field value.
	 * @param fieldType the field type to format
	 * @param formatter the formatter to add
	 */
	void addFormatterForFieldType(Class<?> fieldType, Formatter<?> formatter);

	/**
	 * Adds a Formatter to format fields annotated with a specific format annotation.
	 * @param annotationFormatterFactory the annotation formatter factory to add
	 */
	void addFormatterForFieldAnnotation(AnnotationFormatterFactory<? extends Annotation> annotationFormatterFactory);
	
	/**
	 * Returns the registry of Converters that coerse field values to types required by Formatters.
	 * Allows clients to register their own custom converters directly.
	 * For example, a date/time formatting configuration might expect a java.util.Date field value to be coersed to a Long for formatting.
	 * Registering a simpler DateToLongConverter allievates the need to register multiple formatters for closely related types.
	 * @return the converter registry, allowing new Converters to be registered
	 */
	ConverterRegistry getConverterRegistry();

}
