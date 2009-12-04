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
 * Thrown when an attempt to execute a type conversion fails.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
public final class ConversionFailedException extends ConversionException {

	private final TypeDescriptor sourceType;

	private final TypeDescriptor targetType;


	/**
	 * Create a new conversion exception.
	 * @param value the value we tried to convert
	 * @param sourceType the value's original type
	 * @param targetType the value's target type
	 * @param cause the cause of the conversion failure
	 */
	public ConversionFailedException(TypeDescriptor sourceType, TypeDescriptor targetType, Object value, Throwable cause) {
		super("Unable to convert value " + value + " from type '" + sourceType.getName() +
				"' to type '" + targetType.getName() + "'", cause);
		this.sourceType = sourceType;
		this.targetType = targetType;
	}


	/**
	 * Return the source type we tried to convert the value from.
	 */
	public TypeDescriptor getSourceType() {
		return this.sourceType;
	}

	/**
	 * Returns the target type we tried to convert the value to.
	 */
	public TypeDescriptor getTargetType() {
		return this.targetType;
	}

}
