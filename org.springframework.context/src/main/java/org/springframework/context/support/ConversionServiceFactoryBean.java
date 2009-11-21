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
package org.springframework.context.support;

import java.util.Set;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.support.ConversionServiceFactory;

/**
 * A factory for a ConversionService that installs default converters appropriate for most environments.
 * Set the <code>converters</code> property to supplement or override the default converters.
 * @author Keith Donald
 * @since 3.0
 * @see ConversionServiceFactory#createDefaultConversionService()
 */
public class ConversionServiceFactoryBean implements FactoryBean<ConversionService>, InitializingBean {

	private Set<Object> converters;
	
	private ConversionService conversionService;

	/**
	 * Configure the set of custom Converters that should be added.
	 */
	public void setConverters(Set<Object> converters) {
		this.converters = converters;
	}

	// implementing InitializingBean

	public void afterPropertiesSet() {
		this.conversionService = createConversionService();
		registerConverters(this.converters, (ConverterRegistry) this.conversionService);
	}

	// implementing FactoryBean
	
	public ConversionService getObject() {
		return this.conversionService;
	}

	public Class<? extends ConversionService> getObjectType() {
		return ConversionService.class;
	}

	public boolean isSingleton() {
		return true;
	}

	// subclassing hooks
	
	/**
	 * Creates the ConversionService instance returned by this factory bean.
	 * Creates a default conversion service instance by default.
	 * Subclasses may override to customize the ConversionService instance that gets created.
	 * @see ConversionServiceFactory#createDefaultConversionService()
	 */
	protected ConversionService createConversionService() {
		return ConversionServiceFactory.createDefaultConversionService();
	}

	// internal helpers
	
	private void registerConverters(Set<Object> converters, ConverterRegistry registry) {
		if (converters != null) {
			for (Object converter : converters) {
				if (converter instanceof Converter<?, ?>) {
					registry.addConverter((Converter<?, ?>) converter);
				} else if (converter instanceof ConverterFactory<?, ?>) {
					registry.addConverterFactory((ConverterFactory<?, ?>) converter);
				} else if (converter instanceof GenericConverter) {
					registry.addGenericConverter((GenericConverter) converter);
				} else {
					throw new IllegalArgumentException("Each converter must implement one of the Converter, ConverterFactory, or GenericConverter interfaces");
				}
			}
		}		
	}
	
}
