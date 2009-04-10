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

import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionExecutorNotFoundException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.service.DefaultConversionService;
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

	private ConversionService conversionService;
	
	public StandardTypeConverter() {
		conversionService = new DefaultConversionService();
	}

	public StandardTypeConverter(ConversionService conversionService) {
		Assert.notNull(conversionService, "The conversionService must not be null");
		this.conversionService = conversionService;
	}

	@SuppressWarnings("unchecked")
	public <T> T convertValue(Object value, Class<T> targetType) throws EvaluationException {
		return (T) convertValue(value,TypeDescriptor.valueOf(targetType));
	}

	public Object convertValue(Object value, TypeDescriptor typeDescriptor) throws EvaluationException {
		try {
			return conversionService.executeConversion(value, typeDescriptor);
		} catch (ConversionExecutorNotFoundException cenfe) {
			throw new SpelException(cenfe, SpelMessages.TYPE_CONVERSION_ERROR, value.getClass(), typeDescriptor.asString());
		} catch (ConversionException ce) {
			throw new SpelException(ce, SpelMessages.TYPE_CONVERSION_ERROR, value.getClass(), typeDescriptor.asString());
		}
	}

	public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
		return canConvert(sourceType, TypeDescriptor.valueOf(targetType));
	}

	public boolean canConvert(Class<?> sourceType, TypeDescriptor targetType) {
		return conversionService.canConvert(sourceType, targetType);
	}

}
