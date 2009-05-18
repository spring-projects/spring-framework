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
 * Call {@link #convert(Object, Class)} to perform a thread-safe type conversion using this system.<br>
 * Call {@link #convert(Object, ConversionContext)} to perform a conversion with additional context about the point
 * where conversion needs to occur.
 * 
 * @author Keith Donald
 */
public interface TypeConverter {

	/**
	 * Returns true if objects of sourceType can be converted to targetType.
	 * @param source the source to convert from (may be null)
	 * @param targetType the target type to convert to
	 * @return true if a conversion can be performed, false if not
	 */
	boolean canConvert(Class<?> sourceType, Class<?> targetType);
	
	/**
	 * Returns true if objects of sourceType can be converted to the type of the conversion point.
	 * @param source the source to convert from (may be null)
	 * @param context context about the point where conversion would occur
	 * @return true if a conversion can be performed, false if not
	 */
	boolean canConvert(Class<?> sourceType, ConversionContext<?> context);

	/**
	 * Convert the source to targetType.
	 * @param source the source to convert from (may be null)
	 * @param targetType the target type to convert to
	 * @return the converted object, an instance of targetType, or <code>null</code> if a null source was provided
	 * @throws ConvertException if an exception occurred
	 */
	<S, T> T convert(S source, Class<T> targetType);
	
	/**
	 * Convert the source to type T needed by the conversion point.
	 * @param source the source to convert from (may be null)
	 * @param context context about the point where conversion will occur
	 * @return the converted object, an instance of {@link ConversionContext#getType()}</code>, or <code>null</code> if a null source was provided
	 * @throws ConvertException if an exception occurred
	 */
	<S, T> T convert(S source, ConversionContext<T> context);

}