/*
 * Copyright 2002-2011 the original author or authors.
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
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistrar;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.datetime.joda.JodaTimeFormatterRegistrar;
import org.springframework.format.number.NumberFormatAnnotationFormatterFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringValueResolver;

/**
 * <p>A factory for a {@link FormattingConversionService} that installs default 
 * converters and formatters for common types such as numbers and datetimes.
 * 
 * <p>Converters and formatters can be registered declaratively through 
 * {@link #setConverters(Set)} and {@link #setFormatters(Set)}. Another option
 * is to register converters and formatters in code by implementing the 
 * {@link FormatterRegistrar} interface. You can then configure provide the set 
 * of registrars to use through {@link #setFormatterRegistrars(Set)}.
 * 
 * <p>A good example for registering converters and formatters in code is 
 * <code>JodaTimeFormatterRegistrar</code>, which registers a number of 
 * date-related formatters and converters. For a more detailed list of cases
 * see {@link #setFormatterRegistrars(Set)} 
 * 
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 3.0
 */
public class FormattingConversionServiceFactoryBean
		implements FactoryBean<FormattingConversionService>, EmbeddedValueResolverAware, InitializingBean {

	private static final boolean jodaTimePresent = ClassUtils.isPresent(
			"org.joda.time.LocalDate", FormattingConversionService.class.getClassLoader());

	private Set<?> converters;

	private Set<?> formatters;

	private Set<FormatterRegistrar> formatterRegistrars;

	private StringValueResolver embeddedValueResolver;

	private FormattingConversionService conversionService;

	private boolean registerDefaultFormatters = true;

	/**
	 * Configure the set of custom converter objects that should be added.
	 * @param converters instances of any of the following:
	 * 		{@link org.springframework.core.convert.converter.Converter},
	 * 		{@link org.springframework.core.convert.converter.ConverterFactory},
	 * 		{@link org.springframework.core.convert.converter.GenericConverter}.
	 */
	public void setConverters(Set<?> converters) {
		this.converters = converters;
	}

	/**
	 * Configure the set of custom formatter objects that should be added.
	 * @param formatters instances of {@link Formatter} or 
	 * 		{@link AnnotationFormatterFactory}.
	 */
	public void setFormatters(Set<?> formatters) {
		this.formatters = formatters;
	}

	/**
	 * <p>Configure the set of FormatterRegistrars to invoke to register 
	 * Converters and Formatters in addition to those added declaratively 
	 * via {@link #setConverters(Set)} and {@link #setFormatters(Set)}.
	 * <p>FormatterRegistrars are useful when registering multiple related 
	 * converters and formatters for a formatting category, such as Date 
	 * formatting. All types related needed to support the formatting 
	 * category can be registered from one place.
	 * <p>FormatterRegistrars can also be used to register Formatters 
	 * indexed under a specific field type different from its own &lt;T&gt;, 
	 * or when registering a Formatter from a Printer/Parser pair.
	 * @see FormatterRegistry#addFormatterForFieldType(Class, Formatter)
	 * @see FormatterRegistry#addFormatterForFieldType(Class, Printer, Parser)
	 */
	public void setFormatterRegistrars(Set<FormatterRegistrar> formatterRegistrars) {
		this.formatterRegistrars = formatterRegistrars;
	}

	public void setEmbeddedValueResolver(StringValueResolver embeddedValueResolver) {
		this.embeddedValueResolver = embeddedValueResolver;
	}

	/**
	 * Indicates whether default formatters should be registered or not. By 
	 * default built-in formatters are registered. This flag can be used to 
	 * turn that off and rely on explicitly registered formatters only.
	 * @see #setFormatters(Set)
	 * @see #setFormatterRegistrars(Set)
	 */
	public void setRegisterDefaultFormatters(boolean registerDefaultFormatters) {
		this.registerDefaultFormatters = registerDefaultFormatters;
	}

	public void afterPropertiesSet() {
		this.conversionService = new FormattingConversionService();
		this.conversionService.setEmbeddedValueResolver(this.embeddedValueResolver);
		ConversionServiceFactory.addDefaultConverters(this.conversionService);
		ConversionServiceFactory.registerConverters(this.converters, this.conversionService);
		addDefaultFormatters();
		registerFormatters();
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
	 * Subclasses may override this method to register formatters and/or converters. 
	 * Starting with Spring 3.1 however the recommended way of doing that is to 
	 * through FormatterRegistrars.
	 * @see #setFormatters(Set)
	 * @see #setFormatterRegistrars(Set) 
	 */
	protected void installFormatters(FormatterRegistry registry) {
	}

	// private helper methods

	private void addDefaultFormatters() {
		if (registerDefaultFormatters) {
			this.conversionService.addFormatterForFieldAnnotation(new NumberFormatAnnotationFormatterFactory());
			if (jodaTimePresent) {
				new JodaTimeFormatterRegistrar().registerFormatters(this.conversionService);
			} else {
				this.conversionService
						.addFormatterForFieldAnnotation(new NoJodaDateTimeFormatAnnotationFormatterFactory());
			}
		}
	}

	private void registerFormatters() {
		if (this.formatters != null) {
			for (Object formatter : this.formatters) {
				if (formatter instanceof Formatter<?>) {
					this.conversionService.addFormatter((Formatter<?>) formatter);
				} else if (formatter instanceof AnnotationFormatterFactory<?>) {
					this.conversionService.addFormatterForFieldAnnotation((AnnotationFormatterFactory<?>) formatter);
				} else {
					throw new IllegalArgumentException(
							"Custom formatters must be implementations of Formatter or AnnotationFormatterFactory");
				}
			}
		}
		if (this.formatterRegistrars != null) {
			for (FormatterRegistrar registrar : this.formatterRegistrars) {
				registrar.registerFormatters(this.conversionService);
			}
		}
		installFormatters(this.conversionService);
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
