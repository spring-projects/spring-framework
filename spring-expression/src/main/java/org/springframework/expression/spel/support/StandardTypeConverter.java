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

package org.springframework.expression.spel.support;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.util.Assert;

/**
 * Default implementation of the {@link TypeConverter} interface,
 * delegating to a core Spring {@link ConversionService}.
 *
 * @author Juergen Hoeller
 * @author Andy Clement
 * @since 3.0
 * @see org.springframework.core.convert.ConversionService
 */
public class StandardTypeConverter implements TypeConverter {

	private final Supplier<ConversionService> conversionService;


	/**
	 * Create a StandardTypeConverter for the default ConversionService.
	 * @see DefaultConversionService#getSharedInstance()
	 */
	public StandardTypeConverter() {
		this.conversionService = DefaultConversionService::getSharedInstance;
	}

	/**
	 * Create a StandardTypeConverter for the given ConversionService.
	 * @param conversionService the ConversionService to delegate to
	 */
	public StandardTypeConverter(ConversionService conversionService) {
		Assert.notNull(conversionService, "ConversionService must not be null");
		this.conversionService = () -> conversionService;
	}

	/**
	 * Create a StandardTypeConverter for the given ConversionService.
	 * @param conversionService a Supplier for the ConversionService to delegate to
	 * @since 5.3.11
	 */
	public StandardTypeConverter(Supplier<ConversionService> conversionService) {
		Assert.notNull(conversionService, "Supplier must not be null");
		this.conversionService = conversionService;
	}


	@Override
	public boolean canConvert(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {
		return this.conversionService.get().canConvert(sourceType, targetType);
	}

	@Override
	public @Nullable Object convertValue(@Nullable Object value, @Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {
		try {
			return this.conversionService.get().convert(value, sourceType, targetType);
		}
		catch (ConversionException ex) {
			throw new SpelEvaluationException(ex, SpelMessage.TYPE_CONVERSION_ERROR,
					(sourceType != null ? sourceType.toString() : (value != null ? value.getClass().getName() : "null")),
					targetType.toString());
		}
	}

}
