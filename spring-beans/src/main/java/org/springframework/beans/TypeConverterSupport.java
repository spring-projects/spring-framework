/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
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

	@Nullable
	TypeConverterDelegate typeConverterDelegate;


	@Override
	@Nullable
	public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType) throws TypeMismatchException {
		return convertIfNecessary(value, requiredType, TypeDescriptor.valueOf(requiredType));
	}

	@Override
	@Nullable
	public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
			@Nullable MethodParameter methodParam) throws TypeMismatchException {

		return convertIfNecessary(value, requiredType,
				(methodParam != null ? new TypeDescriptor(methodParam) : TypeDescriptor.valueOf(requiredType)));
	}

	@Override
	@Nullable
	public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable Field field)
			throws TypeMismatchException {

		return convertIfNecessary(value, requiredType,
				(field != null ? new TypeDescriptor(field) : TypeDescriptor.valueOf(requiredType)));
	}

	@Nullable
	@Override
	public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
			@Nullable TypeDescriptor typeDescriptor) throws TypeMismatchException {

		Assert.state(this.typeConverterDelegate != null, "No TypeConverterDelegate");
		try {
			return this.typeConverterDelegate.convertIfNecessary(null, null, value, requiredType, typeDescriptor);
		}
		catch (ConverterNotFoundException | IllegalStateException ex) {
			throw new ConversionNotSupportedException(value, requiredType, ex);
		}
		catch (ConversionException | IllegalArgumentException ex) {
			throw new TypeMismatchException(value, requiredType, ex);
		}
	}

}
