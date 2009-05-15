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

package org.springframework.expression.spel.support;

import org.springframework.core.convert.ConvertException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.BindingPoint;
import org.springframework.core.convert.support.DefaultTypeConverter;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;
import org.springframework.util.Assert;

/**
 * @author Juergen Hoeller
 * @author Andy Clement
 * @since 3.0
 */
public class StandardTypeConverter implements TypeConverter {

	private org.springframework.core.convert.TypeConverter typeConverter;
	
	public StandardTypeConverter() {
		this.typeConverter = new DefaultTypeConverter();
	}

	public StandardTypeConverter(org.springframework.core.convert.TypeConverter typeConverter) {
		Assert.notNull(typeConverter, "TypeConverter must not be null");
		this.typeConverter = typeConverter;
	}

	@SuppressWarnings("unchecked")
	public <T> T convertValue(Object value, Class<T> targetType) throws EvaluationException {
		return (T) convertValue(value, BindingPoint.valueOf(targetType));
	}

	@SuppressWarnings("unchecked")
	public Object convertValue(Object value, BindingPoint typeDescriptor) throws EvaluationException {
		try {
			return this.typeConverter.convert(value, typeDescriptor);
		}
		catch (ConverterNotFoundException cenfe) {
			throw new SpelException(cenfe, SpelMessages.TYPE_CONVERSION_ERROR, value.getClass(), typeDescriptor.asString());
		}
		catch (ConvertException ce) {
			throw new SpelException(ce, SpelMessages.TYPE_CONVERSION_ERROR, value.getClass(), typeDescriptor.asString());
		}
	}

	public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
		return canConvert(sourceType, BindingPoint.valueOf(targetType));
	}

	public boolean canConvert(Class<?> sourceType, BindingPoint targetType) {
		return this.typeConverter.canConvert(sourceType, targetType);
	}

}
