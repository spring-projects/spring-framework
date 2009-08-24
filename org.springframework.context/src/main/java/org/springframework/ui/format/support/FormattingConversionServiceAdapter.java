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

package org.springframework.ui.format.support;

import java.text.ParseException;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.ui.format.Formatter;
import org.springframework.ui.format.FormatterRegistry;
import org.springframework.ui.format.support.GenericFormatterRegistry;
import org.springframework.util.Assert;

/**
 * Adapter that exposes a {@link ConversionService} reference for a given
 * {@link org.springframework.ui.format.FormatterRegistry}, retrieving the current
 * Locale from {@link org.springframework.context.i18n.LocaleContextHolder}.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class FormattingConversionServiceAdapter implements ConversionService {

	private final FormatterRegistry formatterRegistry;

	private final ConversionService targetConversionService;


	/**
	 * Create a new FormattingConversionServiceAdapter for the given FormatterRegistry.
	 * @param formatterRegistry the FormatterRegistry to wrap
	 */
	public FormattingConversionServiceAdapter(FormatterRegistry formatterRegistry) {
		Assert.notNull(formatterRegistry, "FormatterRegistry must not be null");
		this.formatterRegistry = formatterRegistry;
		if (formatterRegistry instanceof GenericFormatterRegistry) {
			this.targetConversionService = ((GenericFormatterRegistry) formatterRegistry).getConversionService();
		}
		else {
			this.targetConversionService = new DefaultConversionService();
		}
	}


	public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
		return canConvert(sourceType, TypeDescriptor.valueOf(targetType));
	}

	public boolean canConvert(Class<?> sourceType, TypeDescriptor targetType) {
		return (this.formatterRegistry.getFormatter(targetType) != null ||
				this.targetConversionService.canConvert(sourceType, targetType));
	}

	@SuppressWarnings("unchecked")
	public <T> T convert(Object source, Class<T> targetType) {
		return (T) convert(source, TypeDescriptor.valueOf(targetType));
	}

	public Object convert(Object source, TypeDescriptor targetType) {
		if (source instanceof String) {
			Formatter formatter = this.formatterRegistry.getFormatter(targetType);
			if (formatter != null) {
				try {
					return formatter.parse((String) source, LocaleContextHolder.getLocale());
				}
				catch (ParseException ex) {
					throw new ConversionFailedException(source, String.class, targetType.getType(), ex);
				}
			}
		}
		return this.targetConversionService.convert(source, targetType);
	}

}
