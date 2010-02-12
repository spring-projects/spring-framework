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

package org.springframework.expression.common;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
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
	 * Determines if there is a type converter available in the specified context and attempts to use it to convert the
	 * supplied value to the specified type. Throws an exception if conversion is not possible.
	 * @param context the evaluation context that may define a type converter
	 * @param value the value to convert (may be null)
	 * @param targetType the type to attempt conversion to
	 * @return the converted value
	 * @throws EvaluationException if there is a problem during conversion or conversion of the value to the specified
	 * type is not supported
	 */
	public static <T> T convert(EvaluationContext context, Object value, Class<T> targetType)
			throws EvaluationException {
		// TODO remove this function over time and use the one it delegates to
		return convertTypedValue(context,new TypedValue(value,TypeDescriptor.forObject(value)),targetType);
	}

	/**
	 * Determines if there is a type converter available in the specified context and attempts to use it to convert the
	 * supplied value to the specified type. Throws an exception if conversion is not possible.
	 * @param context the evaluation context that may define a type converter
	 * @param typedValue the value to convert and a type descriptor describing it
	 * @param targetType the type to attempt conversion to
	 * @return the converted value
	 * @throws EvaluationException if there is a problem during conversion or conversion of the value to the specified
	 * type is not supported
	 */
	@SuppressWarnings("unchecked")
	public static <T> T convertTypedValue(EvaluationContext context, TypedValue typedValue, Class<T> targetType) {
		Object value = typedValue.getValue();
		
		if (targetType == null || ClassUtils.isAssignableValue(targetType, value)) {
			return (T) value;
		}
		if (context != null) {
			return (T) context.getTypeConverter().convertValue(value, typedValue.getTypeDescriptor(), TypeDescriptor.valueOf(targetType));
		}
		throw new EvaluationException("Cannot convert value '" + value + "' to type '" + targetType.getName() + "'");
	}

}
