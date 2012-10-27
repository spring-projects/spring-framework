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

package org.springframework.core.convert;

/**
 * A service interface for type conversion. This is the entry point into the convert system.
 * Call {@link #convert(Object, Class)} to perform a thread-safe type conversion using this system.
 *
 * @author Keith Donald
 * @author Phillip Webb
 * @since 3.0
 */
public interface ConversionService {

	/**
	 * Returns true if objects of sourceType can be converted to targetType.
	 * If this method returns true, it means {@link #convert(Object, Class)} is capable of converting an instance of sourceType to targetType.
	 * Special note on collections, arrays, and maps types:
	 * For conversion between collection, array, and map types, this method will return 'true'
	 * even though a convert invocation may still generate a {@link ConversionException} if the underlying elements are not convertible.
	 * Callers are expected to handle this exceptional case when working with collections and maps.
	 * @param sourceType the source type to convert from (may be null if source is null)
	 * @param targetType the target type to convert to (required)
	 * @return true if a conversion can be performed, false if not
	 * @throws IllegalArgumentException if targetType is null
	 */
	boolean canConvert(Class<?> sourceType, Class<?> targetType);

	/**
	 * Returns true if objects of sourceType can be converted to the targetType.
	 * The TypeDescriptors provide additional context about the source and target locations where conversion would occur, often object fields or property locations.
	 * If this method returns true, it means {@link #convert(Object, TypeDescriptor, TypeDescriptor)} is capable of converting an instance of sourceType to targetType.
	 * Special note on collections, arrays, and maps types:
	 * For conversion between collection, array, and map types, this method will return 'true'
	 * even though a convert invocation may still generate a {@link ConversionException} if the underlying elements are not convertible.
	 * Callers are expected to handle this exceptional case when working with collections and maps.
	 * @param sourceType context about the source type to convert from (may be null if source is null)
	 * @param targetType context about the target type to convert to (required)
	 * @return true if a conversion can be performed between the source and target types, false if not
	 * @throws IllegalArgumentException if targetType is null
	 */
	boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType);

	/**
	 * Returns true if conversion between the sourceType and targetType can be bypassed.
	 * More precisely this method will return true if objects of sourceType can be
	 * converted to the targetType by returning the source object unchanged.
	 * @param sourceType context about the source type to convert from (may be null if source is null)
	 * @param targetType context about the target type to convert to (required)
	 * @return true if conversion can be bypassed
	 * @throws IllegalArgumentException if targetType is null
	 */
	boolean canBypassConvert(Class<?> sourceType, Class<?> targetType);

	/**
	 * Returns true if conversion between the sourceType and targetType can be bypassed.
	 * More precisely this method will return true if objects of sourceType can be
	 * converted to the targetType by returning the source object unchanged.
	 * @param sourceType context about the source type to convert from (may be null if source is null)
	 * @param targetType context about the target type to convert to (required)
	 * @return true if conversion can be bypassed
	 * @throws IllegalArgumentException if targetType is null
	 */
	boolean canBypassConvert(TypeDescriptor sourceType, TypeDescriptor targetType);

	/**
	 * Convert the source to targetType.
	 * @param source the source object to convert (may be null)
	 * @param targetType the target type to convert to (required)
	 * @return the converted object, an instance of targetType
	 * @throws ConversionException if a conversion exception occurred
	 * @throws IllegalArgumentException if targetType is null
	 */
	<T> T convert(Object source, Class<T> targetType);

	/**
	 * Convert the source to targetType.
	 * The TypeDescriptors provide additional context about the source and target locations where conversion will occur, often object fields or property locations.
	 * @param source the source object to convert (may be null)
	 * @param sourceType context about the source type converting from (may be null if source is null)
	 * @param targetType context about the target type to convert to (required)
	 * @return the converted object, an instance of {@link TypeDescriptor#getObjectType() targetType}</code>
	 * @throws ConversionException if a conversion exception occurred
	 * @throws IllegalArgumentException if targetType is null
	 * @throws IllegalArgumentException if sourceType is null but source is not null
	 */
	Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType);

}
