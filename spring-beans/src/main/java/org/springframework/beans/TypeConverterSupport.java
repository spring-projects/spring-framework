/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.beans;

import java.lang.reflect.Field;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.Assert;

/**
 * Base implementation of the {@link TypeConverter} interface, using a package-private delegate.
 * Mainly serves as base class for {@link BeanWrapperImpl}.
 *
 * @author Juergen Hoeller
 * @since 3.2
 * @see SimpleTypeConverter
 */
public abstract class TypeConverterSupport extends PropertyEditorRegistrySupport implements TypeConverter {

	@Nullable TypeConverterDelegate typeConverterDelegate;


	@Override
	public <T> @Nullable T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType) throws TypeMismatchException {
		return convertIfNecessary(null, value, requiredType, TypeDescriptor.valueOf(requiredType));
	}

	@Override
	public <T> @Nullable T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
			@Nullable MethodParameter methodParam) throws TypeMismatchException {

		return convertIfNecessary((methodParam != null ? methodParam.getParameterName() : null), value, requiredType,
				(methodParam != null ? new TypeDescriptor(methodParam) : TypeDescriptor.valueOf(requiredType)));
	}

	@Override
	public <T> @Nullable T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable Field field)
			throws TypeMismatchException {

		return convertIfNecessary((field != null ? field.getName() : null), value, requiredType,
				(field != null ? new TypeDescriptor(field) : TypeDescriptor.valueOf(requiredType)));
	}

	@Override
	public <T> @Nullable T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
			@Nullable TypeDescriptor typeDescriptor) throws TypeMismatchException {

		return convertIfNecessary(null, value, requiredType, typeDescriptor);
	}

	private <T> @Nullable T convertIfNecessary(@Nullable String propertyName, @Nullable Object value,
			@Nullable Class<T> requiredType, @Nullable TypeDescriptor typeDescriptor) throws TypeMismatchException {

		Assert.state(this.typeConverterDelegate != null, "No TypeConverterDelegate");
		try {
			return this.typeConverterDelegate.convertIfNecessary(
					propertyName, null, value, requiredType, typeDescriptor);
		}
		catch (ConverterNotFoundException | IllegalStateException ex) {
			throw new ConversionNotSupportedException(value, requiredType, ex);
		}
		catch (ConversionException | IllegalArgumentException ex) {
			throw new TypeMismatchException(value, requiredType, ex);
		}
	}

}
