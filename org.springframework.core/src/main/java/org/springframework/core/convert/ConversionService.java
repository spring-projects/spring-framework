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
 * A service interface for type conversion. This is the entry point into the convert system. Call one of the
 * <i>executeConversion</i> operations to perform a thread-safe type conversion using
 * this system. Call one of the <i>getConversionExecutor</i> operations to obtain
 * a thread-safe {@link ConversionExecutor} command for later use.
 * 
 * @author Keith Donald
 */
public interface ConversionService {

	/**
	 * Returns true if objects of sourceType can be converted to targetType.
	 * @param source the source to convert from (may be null)
	 * @param targetType the target type to convert to
	 * @return true if a conversion can be performed, false if not
	 */
	public boolean canConvert(Class<?> sourceType, TypeDescriptor targetType);

	/**
	 * Returns true if the source can be converted to targetType.
	 * @param source the source to convert from (may be null)
	 * @param targetType the target type to convert to
	 * @return true if a conversion can be performed, false if not
	 */
	public boolean canConvert(Object source, TypeDescriptor targetType);
	
	/**
	 * Convert the source to targetType.
	 * @param source the source to convert from (may be null)
	 * @param targetType the target type to convert to
	 * @return the converted object, an instance of the <code>targetType</code>, or <code>null</code> if a null source
	 * was provided
	 * @throws ConversionExecutorNotFoundException if no suitable conversion executor could be found to convert the
	 * source to an instance of targetType
	 * @throws ConversionException if an exception occurred during the conversion process
	 */
	public Object executeConversion(Object source, TypeDescriptor targetType) throws ConversionExecutorNotFoundException,
			ConversionException;

	/**
	 * Get a ConversionExecutor that converts objects from sourceType to targetType.
	 * The returned ConversionExecutor is thread-safe and may safely be cached for later use by client code.
	 * @param sourceType the source type to convert from (required)
	 * @param targetType the target type to convert to (required)
	 * @return the executor that can execute instance type conversion, never null
	 * @throws ConversionExecutorNotFoundException when no suitable conversion executor could be found
	 */
	public ConversionExecutor getConversionExecutor(Class<?> sourceType, TypeDescriptor targetType)
			throws ConversionExecutorNotFoundException;

	/**
	 * Get a type by its name; may be the fully-qualified class name or a registered type alias such as 'int'.
	 * @return the class, or <code>null</code> if no such name exists
	 */
	public Class<?> getType(String name);

}