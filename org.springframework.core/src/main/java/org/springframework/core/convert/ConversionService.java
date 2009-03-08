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
 * {@link #executeConversion(Object, Class) executeConversion} operations to perform a thread-safe type conversion using
 * this system. Call one of the {@link #getConversionExecutor(Class, Class) getConversionExecutor} operations to obtain
 * a thread-safe {@link ConversionExecutor} command for later use.
 * 
 * @author Keith Donald
 */
public interface ConversionService {

	/**
	 * Convert the source to target class T.
	 * @param source the source to convert from (may be null)
	 * @param targetClass the target class to convert to
	 * @return the converted object, an instance of the <code>targetClass</code>, or <code>null</code> if a null source
	 * was provided
	 * @throws ConversionExecutorNotFoundException if no suitable conversion executor could be found to convert the
	 * source to an instance of targetClass
	 * @throws ConversionException if an exception occurred during the conversion process
	 */
	public <T> T executeConversion(Object source, Class<T> targetClass) throws ConversionExecutorNotFoundException,
			ConversionException;

	/**
	 * Convert the source to target class T with a custom converter.
	 * @param converterId the id of the custom converter, which must be registered with this conversion service and
	 * capable of converting to the targetClass
	 * @param source the source to convert from (may be null)
	 * @param targetClass the target class to convert to
	 * @return the converted object, an instance of the <code>targetClass</code>, or <code>null</code> if a null source
	 * was provided
	 * @throws ConversionExecutorNotFoundException if no suitable conversion executor could be found to convert the
	 * source to an instance of targetClass
	 * @throws ConversionException if an exception occurred during the conversion process
	 */
	public <T> T executeConversion(String converterId, Object source, Class<T> targetClass)
			throws ConversionExecutorNotFoundException, ConversionException;

	/**
	 * Get a ConversionExecutor that converts objects from S to T.
	 * The returned ConversionExecutor is thread-safe and may safely be cached for later use by client code.
	 * @param sourceClass the source class to convert from (required)
	 * @param targetClass the target class to convert to (required)
	 * @return the executor that can execute instance type conversion, never null
	 * @throws ConversionExecutorNotFoundException when no suitable conversion executor could be found
	 */
	public <S, T> ConversionExecutor<S, T> getConversionExecutor(Class<S> sourceClass, Class<T> targetClass)
			throws ConversionExecutorNotFoundException;

	/**
	 * Get a ConversionExecutor that that converts objects from S to T with a custom converter.
	 * The returned ConversionExecutor is thread-safe and may safely be cached for use in client code.
	 * @param converterId the id of the custom converter, which must be registered with this conversion service and
	 * capable of converting from sourceClass to targetClass (required)
	 * @param sourceClass the source class to convert from (required)
	 * @param targetClass the target class to convert to (required)
	 * @return the executor that can execute instance type conversion, never null
	 * @throws ConversionExecutorNotFoundException when no suitable conversion executor could be found
	 */
	public <S, T> ConversionExecutor<S, T> getConversionExecutor(String converterId, Class<S> sourceClass,
			Class<T> targetClass) throws ConversionExecutorNotFoundException;

	/**
	 * Get a class by its alias.
	 * @return the class, or <code>null</code> if no such alias exists
	 */
	public Class<?> getClassForAlias(String alias);

}