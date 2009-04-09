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

package org.springframework.expression;

import org.springframework.core.convert.TypeDescriptor;

/**
 * A type converter can convert values between different types encountered
 * during expression evaluation.
 *
 * @author Andy Clement
 * @since 3.0
 */
public interface TypeConverter {
	// TODO replace this stuff with Keiths spring-binding conversion code
	// TODO should ExpressionException be thrown for lost precision in the case of coercion?
	// TODO could remove the methods where the target is Class and just keep the TypeDescriptor variants

	/**
	 * Convert (may coerce) a value from one type to another, for example from a boolean to a string.
	 * @param value the value to be converted
	 * @param targetType the type that the value should be converted to if possible
	 * @return the converted value
	 * @throws EvaluationException if conversion is not possible
	 */
	<T> T convertValue(Object value, Class<T> targetType) throws EvaluationException;

	/**
	 * Convert (may coerce) a value from one type to another, for example from a boolean to a string.
	 * The typeDescriptor parameter enables support for typed collections - if the caller really wishes they
	 * can have a List<Integer> for example, rather than simply a List.
	 * @param value the value to be converted
	 * @param typeDescriptor a type descriptor that supplies extra information about the requested result type
	 * @return the converted value
	 * @throws EvaluationException if conversion is not possible
	 */
	Object convertValue(Object value, TypeDescriptor typeDescriptor) throws EvaluationException;

	/**
	 * Return true if the type converter can convert the specified type to the desired target type.
	 * @param sourceType the type to be converted from
	 * @param targetType the type to be converted to
	 * @return true if that conversion can be performed
	 */
	boolean canConvert(Class<?> sourceType, Class<?> targetType);

	/**
	 * Return true if the type converter can convert the specified type to the desired target type.
	 * @param sourceType the type to be converted from
	 * @param typeDescriptor a type descriptor that supplies extra information about the requested result type
	 * @return true if that conversion can be performed
	 */
	boolean canConvert(Class<?> sourceType, TypeDescriptor typeDescriptor);

}
