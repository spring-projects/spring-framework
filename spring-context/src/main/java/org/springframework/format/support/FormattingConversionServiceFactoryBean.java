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

package org.springframework.format.support;

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
import org.springframework.util.StringValueResolver;

/**
 * A factory providing convenient access to a {@code FormattingConversionService}
 * configured with converters and formatters for common types such as numbers and
 * datetimes.
 *
 * <p>Additional converters and formatters can be registered declaratively through
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
 * <p>Like all {@code FactoryBean} implementations, this class is suitable for
 * use when configuring a Spring application context using Spring {@code <beans>}
 * XML. When configuring the container with
 * {@link org.springframework.context.annotation.Configuration @Configuration}
 * classes, simply instantiate, configure and return the appropriate
 * {@code FormattingConversionService} object from a
 * {@link org.springframework.context.annotation.Bean @Bean} method.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author Chris Beams
 * @since 3.0
 */
public class FormattingConversionServiceFactoryBean
		implements FactoryBean<FormattingConversionService>, EmbeddedValueResolverAware, InitializingBean {

	private Set<?> converters;

	private Set<?> formatters;

	private Set<FormatterRegistrar> formatterRegistrars;

	private boolean registerDefaultFormatters = true;

	private StringValueResolver embeddedValueResolver;

	private FormattingConversionService conversionService;


	/**
	 * Configure the set of custom converter objects that should be added.
	 * @param converters instances of any of the following:
	 * {@link org.springframework.core.convert.converter.Converter},
	 * {@link org.springframework.core.convert.converter.ConverterFactory},
	 * {@link org.springframework.core.convert.converter.GenericConverter}
	 */
	public void setConverters(Set<?> converters) {
		this.converters = converters;
	}

	/**
	 * Configure the set of custom formatter objects that should be added.
	 * @param formatters instances of {@link Formatter} or {@link AnnotationFormatterFactory}
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

	/**
	 * Indicate whether default formatters should be registered or not.
	 * <p>By default, built-in formatters are registered. This flag can be used
	 * to turn that off and rely on explicitly registered formatters only.
	 * @see #setFormatters(Set)
	 * @see #setFormatterRegistrars(Set)
	 */
	public void setRegisterDefaultFormatters(boolean registerDefaultFormatters) {
		this.registerDefaultFormatters = registerDefaultFormatters;
	}

	public void setEmbeddedValueResolver(StringValueResolver embeddedValueResolver) {
		this.embeddedValueResolver = embeddedValueResolver;
	}


	public void afterPropertiesSet() {
		this.conversionService = new DefaultFormattingConversionService(this.embeddedValueResolver, this.registerDefaultFormatters);
		ConversionServiceFactory.registerConverters(this.converters, this.conversionService);
		registerFormatters();
	}

	private void registerFormatters() {
		if (this.formatters != null) {
			for (Object formatter : this.formatters) {
				if (formatter instanceof Formatter<?>) {
					this.conversionService.addFormatter((Formatter<?>) formatter);
				}
				else if (formatter instanceof AnnotationFormatterFactory<?>) {
					this.conversionService.addFormatterForFieldAnnotation((AnnotationFormatterFactory<?>) formatter);
				}
				else {
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
	 * Subclasses may override this method to register formatters and/or converters.
	 * Starting with Spring 3.1 however the recommended way of doing that is to
	 * through FormatterRegistrars.
	 * @see #setFormatters(Set)
	 * @see #setFormatterRegistrars(Set)
	 * @deprecated since Spring 3.1 in favor of {@link #setFormatterRegistrars(Set)}
	 */
	@Deprecated
	protected void installFormatters(FormatterRegistry registry) {
	}


	public FormattingConversionService getObject() {
		return this.conversionService;
	}

	public Class<? extends FormattingConversionService> getObjectType() {
		return FormattingConversionService.class;
	}

	public boolean isSingleton() {
		return true;
	}

}
