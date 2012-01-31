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

package org.springframework.core.convert.support;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.util.NumberUtils;

/**
 * Converts from a Character to any JDK-standard Number implementation.
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
final class CharacterToNumberFactory implements ConverterFactory<Character, Number> {

	public <T extends Number> Converter<Character, T> getConverter(Class<T> targetType) {
		return new CharacterToNumber<T>(targetType);
	}

	private static final class CharacterToNumber<T extends Number> implements Converter<Character, T> {

		private final Class<T> targetType;
		
		public CharacterToNumber(Class<T> targetType) {
			this.targetType = targetType;
		}
	
		public T convert(Character source) {
			return NumberUtils.convertNumberToTargetClass((short) source.charValue(), this.targetType);
		}
	}

}
