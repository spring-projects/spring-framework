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

package org.springframework.beans;

import java.beans.PropertyChangeEvent;

/**
 * Exception thrown when no suitable editor can be found to set a bean property.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public class ConversionNotSupportedException extends TypeMismatchException {

	public ConversionNotSupportedException(PropertyChangeEvent propertyChangeEvent, Class requiredType) {
		super(propertyChangeEvent, requiredType);
	}

	public ConversionNotSupportedException(PropertyChangeEvent propertyChangeEvent, Class requiredType, Throwable cause) {
		super(propertyChangeEvent, requiredType, cause);
	}

	public ConversionNotSupportedException(Object value, Class requiredType) {
		super(value, requiredType);
	}

	public ConversionNotSupportedException(Object value, Class requiredType, Throwable cause) {
		super(value, requiredType, cause);
	}
}
