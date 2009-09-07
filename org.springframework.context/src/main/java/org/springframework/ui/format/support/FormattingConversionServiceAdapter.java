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

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.ui.format.Formatter;
import org.springframework.ui.format.FormatterRegistry;
import org.springframework.util.Assert;

/**
 * Adapter that exposes a {@link ConversionService} reference for a given
 * {@link org.springframework.ui.format.FormatterRegistry}, retrieving the current
 * Locale from {@link org.springframework.context.i18n.LocaleContextHolder}.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class FormattingConversionServiceAdapter extends GenericConversionService {

	private final FormatterRegistry formatterRegistry;


	/**
	 * Create a new FormattingConversionServiceAdapter for the given FormatterRegistry.
	 * @param formatterRegistry the FormatterRegistry to wrap
	 */
	public FormattingConversionServiceAdapter(FormatterRegistry formatterRegistry) {
		Assert.notNull(formatterRegistry, "FormatterRegistry must not be null");
		this.formatterRegistry = formatterRegistry;
		if (formatterRegistry instanceof GenericFormatterRegistry) {
			setParent(((GenericFormatterRegistry) formatterRegistry).getConversionService());
		}
		else {
			setParent(new DefaultConversionService());
		}
	}


	@Override
	protected <T> Converter findRegisteredConverter(Class<?> sourceType, Class<T> targetType) {
		if (String.class.equals(sourceType)) {
			Formatter<T> formatter = this.formatterRegistry.getFormatter(targetType);
			if (formatter != null) {
				return new FormattingConverter<T>(formatter);
			}
		}
		return super.findRegisteredConverter(sourceType, targetType);
	}


	private static class FormattingConverter<T> implements Converter<String, T> {

		private final Formatter<T> formatter;

		public FormattingConverter(Formatter<T> formatter) {
			this.formatter = formatter;
		}

		public T convert(String source) throws Exception {
			return this.formatter.parse(source, LocaleContextHolder.getLocale());
		}
	}

}
