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
 * A meta interface a Converter may implement to describe what types he can convert between.
 *
 * Implementing this interface is required when registering converters that do not declare their
 * parameterized types S and T with a {@link org.springframework.core.convert.ConversionService}.
 * Such Converters are often dynamically created by a {@link ConverterFactory}.
 *
 * @author Keith Donald
 * @since 3.0
 * @see Converter
 * @see ConverterFactory
 */
public interface ConverterInfo {

	/**
	 * The source type the converter converts from.
	 */
	Class<?> getSourceType();

	/**
	 * The target type the converter converts to.
	 */
	Class<?> getTargetType();

}
