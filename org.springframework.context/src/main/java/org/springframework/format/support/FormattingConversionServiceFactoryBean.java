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

package org.springframework.format.support;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.datetime.joda.JodaTimeFormattingConfigurer;
import org.springframework.format.number.NumberFormatAnnotationFormatterFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringValueResolver;

/**
 * A factory for a {@link FormattingConversionService} that installs default
 * formatters for common types such as numbers and datetimes.
 *
 * <p>Subclasses may override {@link #installFormatters(FormatterRegistry)}
 * to register custom formatters.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
public class FormattingConversionServiceFactoryBean
		implements FactoryBean<FormattingConversionService>, EmbeddedValueResolverAware, InitializingBean {

	private static final boolean jodaTimePresent = ClassUtils.isPresent(
			"org.joda.time.LocalDate", FormattingConversionService.class.getClassLoader());

	private Set<?> converters;

	private StringValueResolver embeddedValueResolver;

	private FormattingConversionService conversionService;


	/**
	 * Configure the set of custom converter objects that should be added:
	 * implementing {@link org.springframework.core.convert.converter.Converter},
	 * {@link org.springframework.core.convert.converter.ConverterFactory},
	 * or {@link org.springframework.core.convert.converter.GenericConverter}.
	 */
	public void setConverters(Set<?> converters) {
		this.converters = converters;
	}

	public void setEmbeddedValueResolver(StringValueResolver embeddedValueResolver) {
		this.embeddedValueResolver = embeddedValueResolver;
	}

	public void afterPropertiesSet() {
		this.conversionService = new FormattingConversionService();
		this.conversionService.setEmbeddedValueResolver(this.embeddedValueResolver);
		ConversionServiceFactory.addDefaultConverters(this.conversionService);
		ConversionServiceFactory.registerConverters(this.converters, this.conversionService);
		installFormatters(this.conversionService);
	}


	// implementing FactoryBean

	public FormattingConversionService getObject() {
		return this.conversionService;
	}

	public Class<? extends FormattingConversionService> getObjectType() {
		return FormattingConversionService.class;
	}

	public boolean isSingleton() {
		return true;
	}


	// subclassing hooks

	/**
	 * Install Formatters and Converters into the new FormattingConversionService using the FormatterRegistry SPI.
	 * Subclasses may override to customize the set of formatters and/or converters that are installed.
	 */
	protected void installFormatters(FormatterRegistry registry) {
		registry.addFormatterForFieldAnnotation(new NumberFormatAnnotationFormatterFactory());
		if (jodaTimePresent) {
			new JodaTimeFormattingConfigurer().installJodaTimeFormatting(registry);			
		}
		else {
			registry.addFormatterForFieldAnnotation(new NoJodaDateTimeFormatAnnotationFormatterFactory());
		}
	}


	/**
	 * Dummy AnnotationFormatterFactory that simply fails if @DateTimeFormat is being used
	 * without the JodaTime library being present.
	 */
	private static final class NoJodaDateTimeFormatAnnotationFormatterFactory
			implements AnnotationFormatterFactory<DateTimeFormat> {

		private final Set<Class<?>> fieldTypes;

		public NoJodaDateTimeFormatAnnotationFormatterFactory() {
			Set<Class<?>> rawFieldTypes = new HashSet<Class<?>>(4);
			rawFieldTypes.add(Date.class);
			rawFieldTypes.add(Calendar.class);
			rawFieldTypes.add(Long.class);
			this.fieldTypes = Collections.unmodifiableSet(rawFieldTypes);
		}

		public Set<Class<?>> getFieldTypes() {
			return this.fieldTypes;
		}

		public Printer<?> getPrinter(DateTimeFormat annotation, Class<?> fieldType) {
			throw new IllegalStateException("JodaTime library not available - @DateTimeFormat not supported");
		}

		public Parser<?> getParser(DateTimeFormat annotation, Class<?> fieldType) {
			throw new IllegalStateException("JodaTime library not available - @DateTimeFormat not supported");
		}
	}

}
