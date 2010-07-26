/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConverterNotFoundException;

/**
 * Simple implementation of the TypeConverter interface that does not operate
 * on any specific target object. This is an alternative to using a full-blown
 * BeanWrapperImpl instance for arbitrary type conversion needs.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see BeanWrapperImpl
 */
public class SimpleTypeConverter extends PropertyEditorRegistrySupport implements TypeConverter {

	private final TypeConverterDelegate typeConverterDelegate = new TypeConverterDelegate(this);


	public SimpleTypeConverter() {
		registerDefaultEditors();
	}


	public <T> T convertIfNecessary(Object value, Class<T> requiredType) throws TypeMismatchException {
		return convertIfNecessary(value, requiredType, null);
	}

	public <T> T convertIfNecessary(
			Object value, Class<T> requiredType, MethodParameter methodParam) throws TypeMismatchException {
		try {
			return this.typeConverterDelegate.convertIfNecessary(value, requiredType, methodParam);
		}
		catch (ConverterNotFoundException ex) {
			throw new ConversionNotSupportedException(value, requiredType, ex);
		}
		catch (ConversionException ex) {
			throw new TypeMismatchException(value, requiredType, ex);
		}
		catch (IllegalStateException ex) {
			throw new ConversionNotSupportedException(value, requiredType, ex);
		}
		catch (IllegalArgumentException ex) {
			throw new TypeMismatchException(value, requiredType, ex);
		}
	}

}
