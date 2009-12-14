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

package org.springframework.format.support;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.Parser;
import org.springframework.format.Printer;

/**
 * A {@link org.springframework.core.convert.ConversionService} implementation
 * designed to be configured as a {@link FormatterRegistry}.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
public class FormattingConversionService extends GenericConversionService implements FormatterRegistry {

	public void addFormatterForFieldType(Class<?> fieldType, Formatter<?> formatter) {
		addConverter(new PrinterConverter(fieldType, formatter, this));
		addConverter(new ParserConverter(fieldType, formatter, this));
	}

	public void addFormatterForFieldType(Class<?> fieldType, Printer<?> printer, Parser<?> parser) {
		addConverter(new PrinterConverter(fieldType, printer, this));
		addConverter(new ParserConverter(fieldType, parser, this));
	}

	@SuppressWarnings("unchecked")
	public void addFormatterForFieldAnnotation(final AnnotationFormatterFactory annotationFormatterFactory) {
		final Class<? extends Annotation> annotationType = (Class<? extends Annotation>)
				GenericTypeResolver.resolveTypeArgument(annotationFormatterFactory.getClass(), AnnotationFormatterFactory.class);
		if (annotationType == null) {
			throw new IllegalArgumentException(
					"Unable to extract parameterized Annotation type argument from AnnotationFormatterFactory ["
							+ annotationFormatterFactory.getClass().getName()
							+ "]; does the factory parameterize the <A extends Annotation> generic type?");
		}		
		Set<Class<?>> fieldTypes = annotationFormatterFactory.getFieldTypes();

		for (final Class<?> fieldType : fieldTypes) {
			addConverter(new ConditionalGenericConverter() {
				public Set<ConvertiblePair> getConvertibleTypes() {
					return Collections.singleton(new ConvertiblePair(fieldType, String.class));
				}
				public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
					return (sourceType.getAnnotation(annotationType) != null);
				}
				public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
					Printer<?> printer = annotationFormatterFactory.getPrinter(sourceType.getAnnotation(annotationType), sourceType.getType());
					return new PrinterConverter(fieldType, printer, FormattingConversionService.this).convert(source, sourceType, targetType);
				}
				public String toString() {
					return "@" + annotationType.getName() + " " + fieldType.getName() + " -> " +
							String.class.getName() + ": " + annotationFormatterFactory;
				}
			});
			addConverter(new ConditionalGenericConverter() {
				public Set<ConvertiblePair> getConvertibleTypes() {
					return Collections.singleton(new ConvertiblePair(String.class, fieldType));
				}
				public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
					return (targetType.getAnnotation(annotationType) != null);
				}
				public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
					Parser<?> parser = annotationFormatterFactory.getParser(targetType.getAnnotation(annotationType), targetType.getType());
					return new ParserConverter(fieldType, parser, FormattingConversionService.this).convert(source, sourceType, targetType);
				}
				public String toString() {
					return String.class.getName() + " -> @" + annotationType.getName() + " " +
							fieldType.getName() + ": " + annotationFormatterFactory;
				}				
			});
		}
	}


	private static class PrinterConverter implements GenericConverter {

		private Class<?> fieldType;
		
		private TypeDescriptor printerObjectType;

		@SuppressWarnings("unchecked")
		private Printer printer;

		private ConversionService conversionService;

		public PrinterConverter(Class<?> fieldType, Printer<?> printer, ConversionService conversionService) {
			this.fieldType = fieldType;
			this.printerObjectType = TypeDescriptor.valueOf(resolvePrinterObjectType(printer));
			this.printer = printer;
			this.conversionService = conversionService;
		}

		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(new ConvertiblePair(this.fieldType, String.class));
		}

		@SuppressWarnings("unchecked")
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (!sourceType.isAssignableTo(this.printerObjectType)) {
				source = this.conversionService.convert(source, sourceType, this.printerObjectType);
			}
			return (source != null ? this.printer.print(source, LocaleContextHolder.getLocale()) : "");
		}
		
		private Class<?> resolvePrinterObjectType(Printer<?> printer) {
			return GenericTypeResolver.resolveTypeArgument(printer.getClass(), Printer.class);
		}
		
		public String toString() {
			return this.fieldType.getName() + " -> " + String.class.getName() + " : " + this.printer;
		}
	}


	private static class ParserConverter implements GenericConverter {

		private Class<?> fieldType;
		
		private Parser<?> parser;

		private ConversionService conversionService;

		public ParserConverter(Class<?> fieldType, Parser<?> parser, ConversionService conversionService) {
			this.fieldType = fieldType;
			this.parser = parser;
			this.conversionService = conversionService;
		}

		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(new ConvertiblePair(String.class, this.fieldType));
		}

		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			String text = (String) source;
			if (text == null || text.length() == 0) {
				return null;
			}
			try {
				Object result = this.parser.parse(text, LocaleContextHolder.getLocale());
				TypeDescriptor resultType = TypeDescriptor.valueOf(result.getClass());
				if (!resultType.isAssignableTo(targetType)) {
					result = this.conversionService.convert(result, resultType, targetType);
				}
				return result;
			}
			catch (Exception ex) {
				throw new ConversionFailedException(sourceType, targetType, source, ex);
			}
		}

		public String toString() {
			return String.class.getName() + " -> " + this.fieldType.getName() + ": " + this.parser;
		}

	}
	
}
