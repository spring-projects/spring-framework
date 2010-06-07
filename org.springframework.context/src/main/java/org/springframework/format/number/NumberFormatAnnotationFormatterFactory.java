/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Formatter;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.format.annotation.NumberFormat.Style;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * Formats fields annotated with the {@link NumberFormat} annotation.
 *
 * @author Keith Donald
 * @since 3.0
 * @see NumberFormat
 */
public class NumberFormatAnnotationFormatterFactory
		implements AnnotationFormatterFactory<NumberFormat>, EmbeddedValueResolverAware {

	private final Set<Class<?>> fieldTypes;

	private StringValueResolver embeddedValueResolver;


	public NumberFormatAnnotationFormatterFactory() {
		Set<Class<?>> rawFieldTypes = new HashSet<Class<?>>(7);
		rawFieldTypes.add(Short.class);
		rawFieldTypes.add(Integer.class);
		rawFieldTypes.add(Long.class);
		rawFieldTypes.add(Float.class);
		rawFieldTypes.add(Double.class);
		rawFieldTypes.add(BigDecimal.class);
		rawFieldTypes.add(BigInteger.class);
		this.fieldTypes = Collections.unmodifiableSet(rawFieldTypes);
	}

	public final Set<Class<?>> getFieldTypes() {
		return this.fieldTypes;
	}


	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}

	protected String resolveEmbeddedValue(String value) {
		return (this.embeddedValueResolver != null ? this.embeddedValueResolver.resolveStringValue(value) : value);
	}


	public Printer<Number> getPrinter(NumberFormat annotation, Class<?> fieldType) {
		return configureFormatterFrom(annotation);
	}
	
	public Parser<Number> getParser(NumberFormat annotation, Class<?> fieldType) {
		return configureFormatterFrom(annotation);
	}


	private Formatter<Number> configureFormatterFrom(NumberFormat annotation) {
		if (StringUtils.hasLength(annotation.pattern())) {
			return new NumberFormatter(resolveEmbeddedValue(annotation.pattern()));
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
