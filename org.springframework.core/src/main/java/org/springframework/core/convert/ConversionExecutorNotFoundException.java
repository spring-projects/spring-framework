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
 * Thrown when a conversion executor could not be found in a conversion service.
 * 
 * @see ConversionService#getConversionExecutor(Class, Class)
 * @author Keith Donald
 */
public class ConversionExecutorNotFoundException extends ConversionException {

	private Class<?> sourceClass;

	private Class<?> targetClass;

	/**
	 * Creates a new conversion executor not found exception.
	 * @param sourceClass the source type requested to convert from
	 * @param targetClass the target type requested to convert to
	 * @param message a descriptive message
	 */
	public ConversionExecutorNotFoundException(Class<?> sourceClass, Class<?> targetClass, String message) {
		super(message);
		this.sourceClass = sourceClass;
		this.targetClass = targetClass;
	}

	/**
	 * Returns the source type requested to convert from.
	 */
	public Class<?> getSourceClass() {
		return sourceClass;
	}

	/**
	 * Returns the target type requested to convert to.
	 */
	public Class<?> getTargetClass() {
		return targetClass;
	}
}
