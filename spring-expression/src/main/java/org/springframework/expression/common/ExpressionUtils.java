/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.expression.common;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypedValue;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Common utility functions that may be used by any Expression Language provider.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public abstract class ExpressionUtils {

	/**
	 * Determines if there is a type converter available in the specified context and
	 * attempts to use it to convert the supplied value to the specified type. Throws an
	 * exception if conversion is not possible.
	 * @param context the evaluation context that may define a type converter
	 * @param typedValue the value to convert and a type descriptor describing it
	 * @param targetType the type to attempt conversion to
	 * @return the converted value
	 * @throws EvaluationException if there is a problem during conversion or conversion
	 * of the value to the specified type is not supported
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <T> T convertTypedValue(
			@Nullable EvaluationContext context, TypedValue typedValue, @Nullable Class<T> targetType) {

		Object value = typedValue.getValue();
		if (targetType == null) {
			return (T) value;
		}
		if (context != null) {
			return (T) context.getTypeConverter().convertValue(
					value, typedValue.getTypeDescriptor(), TypeDescriptor.valueOf(targetType));
		}
		if (ClassUtils.isAssignableValue(targetType, value)) {
			return (T) value;
		}
		throw new EvaluationException("Cannot convert value '" + value + "' to type '" + targetType.getName() + "'");
	}

	/**
	 * Attempt to convert a typed value to an int using the supplied type converter.
	 */
	public static int toInt(TypeConverter typeConverter, TypedValue typedValue) {
		return convertValue(typeConverter, typedValue, Integer.class);
	}

	/**
	 * Attempt to convert a typed value to a boolean using the supplied type converter.
	 */
	public static boolean toBoolean(TypeConverter typeConverter, TypedValue typedValue) {
		return convertValue(typeConverter, typedValue, Boolean.class);
	}

	/**
	 * Attempt to convert a typed value to a double using the supplied type converter.
	 */
	public static double toDouble(TypeConverter typeConverter, TypedValue typedValue) {
		return convertValue(typeConverter, typedValue, Double.class);
	}

	/**
	 * Attempt to convert a typed value to a long using the supplied type converter.
	 */
	public static long toLong(TypeConverter typeConverter, TypedValue typedValue) {
		return convertValue(typeConverter, typedValue, Long.class);
	}

	/**
	 * Attempt to convert a typed value to a char using the supplied type converter.
	 */
	public static char toChar(TypeConverter typeConverter, TypedValue typedValue) {
		return convertValue(typeConverter, typedValue, Character.class);
	}

	/**
	 * Attempt to convert a typed value to a short using the supplied type converter.
	 */
	public static short toShort(TypeConverter typeConverter, TypedValue typedValue) {
		return convertValue(typeConverter, typedValue, Short.class);
	}

	/**
	 * Attempt to convert a typed value to a float using the supplied type converter.
	 */
	public static float toFloat(TypeConverter typeConverter, TypedValue typedValue) {
		return convertValue(typeConverter, typedValue, Float.class);
	}

	/**
	 * Attempt to convert a typed value to a byte using the supplied type converter.
	 */
	public static byte toByte(TypeConverter typeConverter, TypedValue typedValue) {
		return convertValue(typeConverter, typedValue, Byte.class);
	}

	@SuppressWarnings("unchecked")
	private static <T> T convertValue(TypeConverter typeConverter, TypedValue typedValue, Class<T> targetType) {
		Object result = typeConverter.convertValue(typedValue.getValue(), typedValue.getTypeDescriptor(),
				TypeDescriptor.valueOf(targetType));
		if (result == null) {
			throw new IllegalStateException("Null conversion result for value [" + typedValue.getValue() + "]");
		}
		return (T) result;
	}

}
