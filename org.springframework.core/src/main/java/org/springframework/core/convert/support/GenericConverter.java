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

package org.springframework.core.convert.support;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

/**
 * Uniform converter interface as returned from {@link GenericConversionService#getConverter}.
 *
 * <p>This interface is primarily an internal detail of the {@link GenericConversionService}
 * implementation. It should generally not be implemented by application code directly.
 * See {@link Converter} and {@link ConverterFactory} interfaces for simpler public converter SPIs.
 *
 * @author Keith Donald
 * @since 3.0
 * @see Converter
 * @see ConverterFactory
 * @see GenericConversionService
 */
public interface GenericConverter {

	/**
	 * Convert the source to the targetType described by the TypeDescriptor.
	 * @param source the source object to convert (may be null)
	 * @param sourceType context about the source type to convert from
	 * @param targetType context about the target type to convert to
	 * @return the converted object
	 */
	Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType);

}
