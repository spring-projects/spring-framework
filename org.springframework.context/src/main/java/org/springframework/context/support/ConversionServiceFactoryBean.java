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
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.core.convert.support.GenericConversionService;

/**
 * A factory for a ConversionService that installs default converters appropriate for most environments.
 * Set the {@link #setConverters "converters"} property to supplement or override the default converters.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 * @see ConversionServiceFactory#createDefaultConversionService()
 */
public class ConversionServiceFactoryBean implements FactoryBean<ConversionService>, InitializingBean {

	private Set<Object> converters;

	private GenericConversionService conversionService;


	/**
	 * Configure the set of custom converter objects that should be added:
	 * implementing {@link org.springframework.core.convert.converter.Converter},
	 * {@link org.springframework.core.convert.converter.ConverterFactory},
	 * or {@link org.springframework.core.convert.converter.GenericConverter}.
	 */
	public void setConverters(Set<Object> converters) {
		this.converters = converters;
	}

	public void afterPropertiesSet() {
		this.conversionService = createConversionService();
		ConversionServiceFactory.addDefaultConverters(this.conversionService);
		ConversionServiceFactory.registerConverters(this.converters, this.conversionService);
	}

	/**
	 * Create the ConversionService instance returned by this factory bean.
	 * <p>Creates a simple {@link GenericConversionService} instance by default.
	 * Subclasses may override to customize the ConversionService instance that gets created.
	 */
	protected GenericConversionService createConversionService() {
		return new GenericConversionService();
	}


	// implementing FactoryBean
	
	public ConversionService getObject() {
		return this.conversionService;
	}

	public Class<? extends ConversionService> getObjectType() {
		return GenericConversionService.class;
	}

	public boolean isSingleton() {
		return true;
	}

}
