/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert.support;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.util.Assert;

/**
 * Converts from an Integer to a {@link java.lang.Enum} by calling {@link Class#getEnumConstants()}.
 *
 * @author Yanming Zhou
 * @author Stephane Nicoll
 * @since 4.3
 */
@SuppressWarnings({"rawtypes", "unchecked"})
final class IntegerToEnumConverterFactory implements ConverterFactory<Integer, Enum>, ConditionalConverter {

	@Override
	public <T extends Enum> Converter<Integer, @Nullable T> getConverter(Class<T> targetType) {
		Class<?> enumType = ConversionUtils.resolveEnumType(targetType);
		Assert.notNull(enumType, () -> "The target type " + targetType.getName() + " does not refer to an enum");
		return new IntegerToEnum(enumType);
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return (ConversionUtils.resolveEnumType(targetType.getType()) != null);
	}


	private static class IntegerToEnum<T extends Enum> implements Converter<Integer, T> {

		private final Class<T> enumType;

		public IntegerToEnum(Class<T> enumType) {
			this.enumType = enumType;
		}

		@Override
		public T convert(Integer source) {
			return this.enumType.getEnumConstants()[source];
		}
	}

}
