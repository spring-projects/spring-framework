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

package org.springframework.core.convert.support;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.util.Assert;

/**
 * Converts from a String to a {@link java.lang.Enum} by calling {@link Enum#valueOf(Class, String)}.
 *
 * @author Keith Donald
 * @author Stephane Nicoll
 * @since 3.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
final class StringToEnumConverterFactory implements ConverterFactory<String, Enum>, ConditionalConverter {

	@Override
	public <T extends Enum> Converter<String, @Nullable T> getConverter(Class<T> targetType) {
		Class<?> enumType = ConversionUtils.resolveEnumType(targetType);
		Assert.notNull(enumType, () -> "The target type " + targetType.getName() + " does not refer to an enum");
		return new StringToEnum(enumType);
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return (ConversionUtils.resolveEnumType(targetType.getType()) != null);
	}


	private static class StringToEnum<T extends Enum> implements Converter<String, @Nullable T> {

		private final Class<T> enumType;

		StringToEnum(Class<T> enumType) {
			this.enumType = enumType;
		}

		@Override
		public @Nullable T convert(String source) {
			if (source.isEmpty()) {
				// It's an empty enum identifier: reset the enum value to null.
				return null;
			}
			return (T) Enum.valueOf(this.enumType, source.trim());
		}
	}

}
