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

import org.springframework.core.style.StylerUtils;

/**
 * Thrown when an attempt to execute a type conversion fails.
 * 
 * @author Keith Donald
 */
public class ConversionExecutionException extends ConversionException {

	/**
	 * The value we tried to convert. Transient because we cannot guarantee that the value is Serializable.
	 */
	private transient Object value;

	/**
	 * The source type we tried to convert the value from.
	 */
	private Class<?> sourceClass;

	/**
	 * The target type we tried to convert the value to.
	 */
	private Class<?> targetClass;

	/**
	 * Creates a new conversion exception.
	 * @param value the value we tried to convert
	 * @param sourceClass the value's original type
	 * @param targetClass the value's target type
	 * @param cause the cause of the conversion failure
	 */
	public ConversionExecutionException(Object value, Class<?> sourceClass, Class<?> targetClass, Throwable cause) {
		super(defaultMessage(value, sourceClass, targetClass, cause), cause);
		this.value = value;
		this.sourceClass = sourceClass;
		this.targetClass = targetClass;
	}

	/**
	 * Creates a new conversion exception.
	 * @param value the value we tried to convert
	 * @param sourceClass the value's original type
	 * @param targetClass the value's target type
	 * @param message a descriptive message of what went wrong.
	 */
	public ConversionExecutionException(Object value, Class<?> sourceClass, Class<?> targetClass, String message) {
		super(message);
		this.value = value;
		this.sourceClass = sourceClass;
		this.targetClass = targetClass;
	}

	/**
	 * Returns the actual value we tried to convert, an instance of {@link #getSourceClass()}.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Returns the source type we tried to convert the value from.
	 */
	public Class<?> getSourceClass() {
		return sourceClass;
	}

	/**
	 * Returns the target type we tried to convert the value to.
	 */
	public Class<?> getTargetClass() {
		return targetClass;
	}

	private static String defaultMessage(Object value, Class<?> sourceClass, Class<?> targetClass, Throwable cause) {
		return "Unable to convert value " + StylerUtils.style(value) + " from type [" + sourceClass.getName()
				+ "] to type [" + targetClass.getName() + "]; reason = '" + cause.getMessage() + "'";
	}

}