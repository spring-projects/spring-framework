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
 * @since 3.0
 */
public interface ConverterRegistry {
	
	/**
	 * Add a converter to this registry.
	 */
	void add(Converter<?, ?> converter);

	/**
	 * Add a converter factory to this registry.
	 */
	void add(ConverterFactory<?, ?> converterFactory);

	/**
	 * Remove the conversion logic from the sourceType to the targetType.
	 * @param sourceType the source type
	 * @param targetType the target type
	 */
	void removeConverter(Class<?> sourceType, Class<?> targetType);

}
