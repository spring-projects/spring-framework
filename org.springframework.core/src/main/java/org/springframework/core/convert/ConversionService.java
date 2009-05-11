/*
 * Copyright 2004-2009 the original author or authors.
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
 * <p>
 * Call {@link #convert(Object, TypeDescriptor)} to perform a thread-safe type conversion using
 * this system.
 * 
 * @author Keith Donald
 */
public interface ConversionService {

	/**
	 * Returns true if objects of sourceType can be converted to targetType.
	 * @param source the source to convert from (may be null)
	 * @param targetType context about the target type to convert to
	 * @return true if a conversion can be performed, false if not
	 */
	public boolean canConvert(Class<?> sourceType, TypeDescriptor targetType);

	/**
	 * Convert the source to targetType.
	 * @param source the source to convert from (may be null)
	 * @param targetType context about the target type to convert to
	 * @return the converted object, an instance of {@link TypeDescriptor#getType()}</code>, or <code>null</code> if a null source
	 * was provided
	 * @throws ConverterNotFoundException if no suitable conversion executor could be found to convert the
	 * source to an instance of targetType
	 * @throws ConversionException if an exception occurred during the conversion process
	 */
	public Object convert(Object source, TypeDescriptor targetType);

}