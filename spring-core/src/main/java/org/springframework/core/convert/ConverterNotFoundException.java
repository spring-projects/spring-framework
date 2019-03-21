/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.convert;

/**
 * Exception to be thrown when a suitable converter could not be found
 * in a given conversion service.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
@SuppressWarnings("serial")
public class ConverterNotFoundException extends ConversionException {

	private final TypeDescriptor sourceType;

	private final TypeDescriptor targetType;


	/**
	 * Create a new conversion executor not found exception.
	 * @param sourceType the source type requested to convert from
	 * @param targetType the target type requested to convert to
	 */
	public ConverterNotFoundException(TypeDescriptor sourceType, TypeDescriptor targetType) {
		super("No converter found capable of converting from type [" + sourceType + "] to type [" + targetType + "]");
		this.sourceType = sourceType;
		this.targetType = targetType;
	}


	/**
	 * Return the source type that was requested to convert from.
	 */
	public TypeDescriptor getSourceType() {
		return this.sourceType;
	}

	/**
	 * Return the target type that was requested to convert to.
	 */
	public TypeDescriptor getTargetType() {
		return this.targetType;
	}

}
