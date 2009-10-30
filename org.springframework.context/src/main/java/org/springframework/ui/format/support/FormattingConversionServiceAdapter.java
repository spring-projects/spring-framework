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
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.convert.support.GenericConverter;
import org.springframework.ui.format.FormattingService;

/**
 * Adapter that exposes a {@link ConversionService} reference for a given {@link FormattingService},
 * retrieving the current Locale from {@link LocaleContextHolder}.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class FormattingConversionServiceAdapter extends GenericConversionService {

	private final FormattingService formattingService;
	
	public FormattingConversionServiceAdapter(FormattingService formattingService) {
		this.formattingService = formattingService;
		addGenericConverter(String.class, Object.class, new GenericConverter() {
			public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
				try {
					return FormattingConversionServiceAdapter.this.formattingService.parse((String) source, targetType, LocaleContextHolder.getLocale());
				} catch (ParseException e) {
					throw new ConversionFailedException(sourceType, targetType, source, e);
				} 
			}
		});
		addGenericConverter(Object.class, String.class, new GenericConverter() {
			public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
				return FormattingConversionServiceAdapter.this.formattingService.print(source, targetType, LocaleContextHolder.getLocale());
			}
		});		
	}

}
