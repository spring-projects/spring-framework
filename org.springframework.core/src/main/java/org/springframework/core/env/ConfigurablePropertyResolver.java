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

package org.springframework.core.env;

import org.springframework.core.convert.ConversionService;


/**
 * Configuration interface to be implemented by most if not all {@link PropertyResolver
 * PropertyResolvers}. Provides facilities for accessing and customizing the
 * {@link ConversionService} used when converting property values from one type to
 * another.
 *
 * @author Chris Beams
 * @since 3.1
 */
public interface ConfigurablePropertyResolver extends PropertyResolver {

	/**
	 * @return the {@link ConversionService} used when performing type
	 * conversions on properties.
	 * @see PropertyResolver#getProperty(String, Class)
	 */
	ConversionService getConversionService();

	/**
	 * Set the {@link ConversionService} to be used when performing type
	 * conversions on properties.
	 * @see PropertyResolver#getProperty(String, Class)
	 */
	void setConversionService(ConversionService conversionService);

}
