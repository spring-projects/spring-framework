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

package org.springframework.core.convert;

/**
 * A service interface for type conversion. This is the entry point into the convert system.
 * Call {@link #convert(Object, Class)} to perform a thread-safe type conversion using this system.
 *
 * @author Keith Donald
 * @since 3.0
 */
public interface ConversionService {

	/**
	 * Returns true if objects of sourceType can be converted to targetType.
	 * @param sourceType the source type to convert from (required)
	 * @param targetType the target type to convert to (required)
	 * @return true if a conversion can be performed, false if not
	 */
	boolean canConvert(Class<?> sourceType, Class<?> targetType);

	/**
	 * Convert the source to targetType.
	 * @param source the source object to convert (may be null)
	 * @param targetType the target type to convert to (required)
	 * @return the converted object, an instance of targetType
	 * @throws ConversionException if an exception occurred
	 */
	<T> T convert(Object source, Class<T> targetType);

	/**
	 * Returns true if objects of sourceType can be converted to the targetType.
	 * The TypeDescriptors provide additional context about the field locations where conversion would occur, often object property locations.
	 * This flavor of the canConvert operation exists mainly for use by a general purpose data mapping framework, and not for use by user code.
	 * @param sourceType context about the source type to convert from (required)
	 * @param targetType context about the target type to convert to (required)
	 * @return true if a conversion can be performed between the source and target types, false if not
	 */
	boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType);

	/**
	 * Convert the source to targetType.
	 * The TypeDescriptors provide additional context about the field locations where conversion will occur, often object property locations.
	 * This flavor of the convert operation exists mainly for use by a general purpose data mapping framework, and not for use by user code.
	 * @param source the source object to convert (may be null)
	 * @param sourceType context about the source type converting from (required)
	 * @param targetType context about the target type to convert to (required)
	 * @return the converted object, an instance of {@link TypeDescriptor#getObjectType() targetType}</code>
	 * @throws ConversionException if an exception occurred
	 */
	Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType);

}
