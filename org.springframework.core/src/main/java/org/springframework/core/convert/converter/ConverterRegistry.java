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

package org.springframework.core.convert.converter;

/**
 * For registering converters with a type conversion system.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
public interface ConverterRegistry {
	
	/**
	 * Add a plain converter to this registry.
	 */
	void addConverter(Converter<?, ?> converter);

	/**
	 * Add a generic converter to this registry.
	 */
	void addConverter(GenericConverter converter);

	/**
	 * Add a ranged converter factory to this registry.
	 */
	void addConverterFactory(ConverterFactory<?, ?> converterFactory);

	/**
	 * Remove any converters from sourceType to targetType.
	 * @param sourceType the source type
	 * @param targetType the target type
	 */
	void removeConvertible(Class<?> sourceType, Class<?> targetType);

}
