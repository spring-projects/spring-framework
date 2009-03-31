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
 * @author Keith Donald
 */
public class ConversionExecutorNotFoundException extends ConversionException {

	private TypeDescriptor sourceType;

	private TypeDescriptor targetType;

	/**
	 * Creates a new conversion executor not found exception.
	 * @param sourceType the source type requested to convert from
	 * @param targetType the target type requested to convert to
	 * @param message a descriptive message
	 */
	public ConversionExecutorNotFoundException(TypeDescriptor sourceType, TypeDescriptor targetType, String message) {
		super(message);
		this.sourceType = sourceType;
		this.targetType = targetType;
	}

	/**
	 * Returns the source type that was requested to convert from.
	 */
	public TypeDescriptor getSourceType() {
		return sourceType;
	}

	/**
	 * Returns the target type that was requested to convert to.
	 */
	public TypeDescriptor getTargetType() {
		return targetType;
	}
}
