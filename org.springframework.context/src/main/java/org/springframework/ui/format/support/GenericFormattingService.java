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

package org.springframework.ui.format.support;

import java.lang.annotation.Annotation;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.ui.format.AnnotationFormatterFactory;
import org.springframework.ui.format.Formatter;
import org.springframework.ui.format.FormatterRegistry;
import org.springframework.ui.format.FormattingService;
import org.springframework.ui.format.Parser;
import org.springframework.ui.format.Printer;
import org.springframework.util.Assert;

/**
 * A generic implementation of {@link FormattingService} suitable for use in most environments.
 * Is a {@link FormatterRegistry} to allow for registration of field formatting logic.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
public class GenericFormattingService implements FormattingService, FormatterRegistry {

	private final Map<Class<?>, GenericFormatter> typeFormatters = new ConcurrentHashMap<Class<?>, GenericFormatter>();

	private final Map<Class<? extends Annotation>, GenericAnnotationFormatterFactory> annotationFormatters = new ConcurrentHashMap<Class<? extends Annotation>, GenericAnnotationFormatterFactory>();

	private GenericConversionService conversionService = new GenericConversionService();

	/**
	 * Configure a parent of the type conversion service that will be used to coerce objects to types required for formatting.
	 */
	public void setParentConversionService(ConversionService parentConversionService) {
		this.conversionService.setParent(parentConversionService);
	}

	// implementing FormattingService
	
	public String print(Object fieldValue, TypeDescriptor fieldType, Locale locale) {
		return getFormatter(fieldType).print(fieldValue, fieldType, locale);
	}

	public Object parse(String submittedValue, TypeDescriptor fieldType, Locale locale) throws ParseException {
		return getFormatter(fieldType).parse(submittedValue, fieldType, locale);
	}

	// implementing FormatterRegistry

	public void addFormatterForFieldType(Class<?> fieldType, Printer<?> printer, Parser<?> parser) {
		Class<?> printerObjectType = resolvePrinterObjectType(printer);
		Class<?> parserObjectType = resolveParserObjectType(parser);
		this.typeFormatters.put(fieldType, new GenericFormatter(printerObjectType, printer, parserObjectType, parser));
	}

	public void addFormatterForFieldType(Class<?> fieldType, Formatter<?> formatter) {
		Class<?> formatterObjectType = resolveFormatterObjectType(formatter);
		this.typeFormatters.put(fieldType, new GenericFormatter(formatterObjectType, formatter, formatterObjectType, formatter));
	}

	public void addFormatterForFieldAnnotation(AnnotationFormatterFactory<?> annotationFormatterFactory) {
		Class<? extends Annotation> annotationType = resolveAnnotationType(annotationFormatterFactory);
		if (annotationType == null) {
			throw new IllegalArgumentException(
					"Unable to extract parameterized Annotation type argument from AnnotationFormatterFactory ["
							+ annotationFormatterFactory.getClass().getName()
							+ "]; does the factory parameterize the <A extends Annotation> generic type?");
		}
		this.annotationFormatters.put(annotationType, new GenericAnnotationFormatterFactory(annotationFormatterFactory));
	}

	public ConverterRegistry getConverterRegistry() {
		return this.conversionService;
	}

	// internal helpers

	private Class<?> resolveParserObjectType(Parser<?> parser) {
		return GenericTypeResolver.resolveTypeArgument(parser.getClass(), Parser.class);
	}

	private Class<?> resolvePrinterObjectType(Printer<?> printer) {
		return GenericTypeResolver.resolveTypeArgument(printer.getClass(), Printer.class);
	}

	private Class<?> resolveFormatterObjectType(Formatter<?> formatter) {
		return GenericTypeResolver.resolveTypeArgument(formatter.getClass(), Formatter.class);
	}
	
	@SuppressWarnings("unchecked")
	private Class<? extends Annotation> resolveAnnotationType(AnnotationFormatterFactory<?> annotationFormatterFactory) {
		return (Class<? extends Annotation>) GenericTypeResolver.resolveTypeArgument(annotationFormatterFactory.getClass(), AnnotationFormatterFactory.class);
	}

	private GenericFormatter getFormatter(TypeDescriptor fieldType) {
		Assert.notNull(fieldType, "Field TypeDescriptor is required");
		GenericFormatter formatter = findFormatterForAnnotatedField(fieldType);
		Class<?> fieldObjectType = fieldType.getObjectType();
		if (formatter == null) {
			formatter = findFormatterForFieldType(fieldObjectType);
		}
		return formatter;
	}

	private GenericFormatter findFormatterForAnnotatedField(TypeDescriptor fieldType) {
		for (Annotation annotation : fieldType.getAnnotations()) {
			GenericFormatter formatter = findFormatterForAnnotation(annotation, fieldType.getObjectType());
			if (formatter != null) {
				return formatter;
			}
		}
		return null;
	}

	private GenericFormatter findFormatterForAnnotation(Annotation annotation, Class<?> fieldType) {
		Class<? extends Annotation> annotationType = annotation.annotationType();
		GenericAnnotationFormatterFactory factory = this.annotationFormatters.get(annotationType);
		if (factory != null) {
			return factory.getFormatter(annotation, fieldType);
		} else {
			return null;
		}
	}

	private GenericFormatter findFormatterForFieldType(Class<?> fieldType) {
		LinkedList<Class<?>> classQueue = new LinkedList<Class<?>>();
		classQueue.addFirst(fieldType);
		while (!classQueue.isEmpty()) {
			Class<?> currentClass = classQueue.removeLast();
			GenericFormatter formatter = this.typeFormatters.get(currentClass);
			if (formatter != null) {
				return formatter;
			}
			if (currentClass.getSuperclass() != null) {
				classQueue.addFirst(currentClass.getSuperclass());
			}
			Class<?>[] interfaces = currentClass.getInterfaces();
			for (Class<?> ifc : interfaces) {
				classQueue.addFirst(ifc);
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private class GenericFormatter {
		
		private TypeDescriptor printerObjectType;
		
		private Printer printer;

		private Parser parser;

		public GenericFormatter(Class<?> printerObjectType, Printer<?> printer, Class<?> parserObjectType, Parser<?> parser) {
			this.printerObjectType = TypeDescriptor.valueOf(printerObjectType);
			this.printer = printer;
			this.parser = parser;
		}
		
		public String print(Object fieldValue, TypeDescriptor fieldType, Locale locale) {
			if (!fieldType.isAssignableTo(this.printerObjectType)) {
				fieldValue = GenericFormattingService.this.conversionService.convert(fieldValue, fieldType, this.printerObjectType);
			}
			return fieldType != null ? this.printer.print(fieldValue, locale) : "";
		}

		public Object parse(String submittedValue, TypeDescriptor fieldType, Locale locale) throws ParseException {
			if (submittedValue.isEmpty()) {
				return null;
			}
			Object parsedValue = this.parser.parse(submittedValue, locale);
			TypeDescriptor parsedObjectType = TypeDescriptor.valueOf(parsedValue.getClass());
			if (!parsedObjectType.isAssignableTo(fieldType)) {
				parsedValue = GenericFormattingService.this.conversionService.convert(parsedValue, parsedObjectType, fieldType);
			}
			return parsedValue;
		}
		
	}

	@SuppressWarnings("unchecked")
	private class GenericAnnotationFormatterFactory {

		private AnnotationFormatterFactory annotationFormatterFactory;

		public GenericAnnotationFormatterFactory(AnnotationFormatterFactory<?> annotationFormatterFactory) {
			this.annotationFormatterFactory = annotationFormatterFactory;
		}

		public GenericFormatter getFormatter(Annotation annotation, Class<?> fieldType) {
			Printer<?> printer = this.annotationFormatterFactory.getPrinter(annotation, fieldType);
			Parser<?> parser = this.annotationFormatterFactory.getParser(annotation, fieldType);
			return new GenericFormatter(getPrinterObjectType(printer, fieldType), printer, getParserObjectType(parser, fieldType), parser);
		}

		// internal helpers
		
		private Class<?> getPrinterObjectType(Printer<?> printer, Class<?> fieldType) {
			// TODO cache
			return resolvePrinterObjectType(printer);
		}

		private Class<?> getParserObjectType(Parser<?> parser, Class<?> fieldType) {
			// TODO cache
			return resolveParserObjectType(parser);
		}

	}

}
