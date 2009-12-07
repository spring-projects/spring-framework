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

package org.springframework.format.number;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Formatter;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.format.annotation.NumberFormat.Style;
import org.springframework.util.StringUtils;

/**
 * Formats fields annotated with the {@link NumberFormat} annotation.
 *
 * @author Keith Donald
 * @since 3.0
 * @see NumberFormat
 */
public final class NumberFormatAnnotationFormatterFactory implements AnnotationFormatterFactory<NumberFormat> {

	private final Set<Class<?>> fieldTypes;


	public NumberFormatAnnotationFormatterFactory() {
		this.fieldTypes = Collections.unmodifiableSet(createFieldTypes());
	}


	public Set<Class<?>> getFieldTypes() {
		return this.fieldTypes;
	}

	public Printer<Number> getPrinter(NumberFormat annotation, Class<?> fieldType) {
		return configureFormatterFrom(annotation, fieldType);
	}
	
	public Parser<Number> getParser(NumberFormat annotation, Class<?> fieldType) {
		return configureFormatterFrom(annotation, fieldType);
	}


	// internal helpers
	
	private Set<Class<?>> createFieldTypes() {
		Set<Class<?>> fieldTypes = new HashSet<Class<?>>(7);
		fieldTypes.add(Short.class);
		fieldTypes.add(Integer.class);
		fieldTypes.add(Long.class);
		fieldTypes.add(Float.class);
		fieldTypes.add(Double.class);
		fieldTypes.add(BigDecimal.class);
		fieldTypes.add(BigInteger.class);
		return fieldTypes;
	}

	private Formatter<Number> configureFormatterFrom(NumberFormat annotation, Class<?> fieldType) {
		if (StringUtils.hasLength(annotation.pattern())) {
			return new NumberFormatter(annotation.pattern());
		}
		else {
			Style style = annotation.style();
			if (style == Style.PERCENT) {
				return new PercentFormatter();
			}
			else if (style == Style.CURRENCY) {
				return new CurrencyFormatter();
			}
			else {
				return new NumberFormatter();
			}
		}
	}

}
