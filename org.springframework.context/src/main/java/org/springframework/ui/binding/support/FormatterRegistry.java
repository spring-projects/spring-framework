/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.ui.binding.support;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.ui.format.AnnotationFormatterFactory;
import org.springframework.ui.format.Formatter;

/**
 * A centralized registry of Formatters indexed by property types.
 * @author Keith Donald
 * @since 3.0
 */
public interface FormatterRegistry {

	/**
	 * Get the Formatter for the property type.
	 * @param propertyType the property type descriptor, which provides additional property metadata.
	 * @return the Formatter, or <code>null</code> if none is registered
	 */
	Formatter<?> getFormatter(TypeDescriptor<?> propertyType);
	
	/**
	 * Adds a Formatter that will format the values of properties of the provided type.
	 * The type should generally be a concrete class for a scalar value, such as BigDecimal, and not a collection value.
	 * The type may be an annotation type, which will have the Formatter format values of properties annotated with that annotation.
	 * Use {@link #add(AnnotationFormatterFactory)} when the format annotation defines configurable annotation instance values.
	 * <p>
	 * Note the Formatter's formatted object type does not have to equal the associated property type.
	 * When the property type differs from the formatted object type, the caller of the Formatter is expected to coerse a property value to the type expected by the Formatter.  
	 * @param propertyType the type
	 * @param formatter the formatter
	 */
	void add(Class<?> propertyType, Formatter<?> formatter);

	/**
	 * Adds a AnnotationFormatterFactory that will format values of properties annotated with a specific annotation.
	 * @param factory the annotation formatter factory
	 */
	void add(AnnotationFormatterFactory<?, ?> factory);

}
