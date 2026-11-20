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
import org.springframework.util.NumberUtils;

/**
 * Converts from any JDK-standard Number implementation to any other JDK-standard Number implementation.
 *
 * <p>Support Number classes including Byte, Short, Integer, Float, Double, Long, BigInteger, BigDecimal. This class
 * delegates to {@link NumberUtils#convertNumberToTargetClass(Number, Class)} to perform the conversion.
 *
 * @author Keith Donald
 * @since 3.0
 * @see java.lang.Byte
 * @see java.lang.Short
 * @see java.lang.Integer
 * @see java.lang.Long
 * @see java.math.BigInteger
 * @see java.lang.Float
 * @see java.lang.Double
 * @see java.math.BigDecimal
 * @see NumberUtils
 */
final class NumberToNumberConverterFactory implements ConverterFactory<Number, Number>, ConditionalConverter {

	@Override
	public <T extends Number> Converter<Number, ? extends @Nullable T> getConverter(Class<T> targetType) {
		return new NumberToNumber<>(targetType);
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return !sourceType.equals(targetType);
	}


	private static final class NumberToNumber<T extends Number> implements Converter<Number, T> {

		private final Class<T> targetType;

		NumberToNumber(Class<T> targetType) {
			this.targetType = targetType;
		}

		@Override
		public T convert(Number source) {
			return NumberUtils.convertNumberToTargetClass(source, this.targetType);
		}
	}

}
