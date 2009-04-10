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
package org.springframework.core.convert.converter;

import org.springframework.util.NumberUtils;

/**
 * Converts from any JDK-standard Number implementation to a Character and back.
 * 
 * Support Number classes including Byte, Short, Integer, Float, Double, Long, BigInteger, BigDecimal. This class
 * delegates to {@link NumberUtils#convertNumberToTargetClass(Number, Class)} to perform the conversion.
 * 
 * @see java.lang.Byte
 * @see java.lang.Short
 * @see java.lang.Integer
 * @see java.lang.Long
 * @see java.math.BigInteger
 * @see java.lang.Float
 * @see java.lang.Double
 * @see java.math.BigDecimal
 * @see NumberUtils
 * 
 * @author Keith Donald
 */
public class NumberToCharacter implements SuperTwoWayConverter<Number, Character> {

	@SuppressWarnings("unchecked")
	public <RT extends Character> RT convert(Number source, Class<RT> targetClass) {
		return (RT) Character.valueOf((char) source.shortValue());
	}

	public <RS extends Number> RS convertBack(Character target, Class<RS> sourceClass) {
		return NumberUtils.convertNumberToTargetClass((short) target.charValue(), sourceClass);
	}
	
}