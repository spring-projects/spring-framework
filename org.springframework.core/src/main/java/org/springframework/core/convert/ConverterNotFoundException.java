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
 * Thrown when a suitable converter could not be found in a conversion service.
 *
 * @author Keith Donald
 * @since 3.0 
 */
public class ConverterNotFoundException extends ConversionException {

	private final Class<?> sourceType;

	private final Class<?> targetType;


	/**
	 * Creates a new conversion executor not found exception.
	 * @param sourceType the source type requested to convert from
	 * @param targetType the target type requested to convert to
	 * @param message a descriptive message
	 */
	public ConverterNotFoundException(Class<?> sourceType, Class<?> targetType, String message) {
		super(message);
		this.sourceType = sourceType;
		this.targetType = targetType;
	}


	/**
	 * Returns the source type that was requested to convert from.
	 */
	public Class<?> getSourceType() {
		return this.sourceType;
	}

	/**
	 * Returns the target type that was requested to convert to.
	 */
	public Class<?> getTargetType() {
		return this.targetType;
	}

}
