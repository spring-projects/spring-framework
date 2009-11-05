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

package org.springframework.format.support;

import java.beans.PropertyEditorSupport;

import org.springframework.core.convert.ConversionService;
import org.springframework.util.Assert;

/**
 * Adapter that exposes a {@link java.beans.PropertyEditor} for any given
 * {@link org.springframework.format.Formatter}, retrieving the current
 * Locale from {@link org.springframework.context.i18n.LocaleContextHolder}.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class FormattingPropertyEditorAdapter extends PropertyEditorSupport {

	private final ConversionService conversionService;

	private final Class<?> fieldType;

	/**
	 * Create a new FormattingPropertyEditorAdapter for the given Formatter.
	 * @param formatter the Formatter to wrap
	 */
	public FormattingPropertyEditorAdapter(ConversionService formattingService, Class<?> fieldType) {
		Assert.notNull(formattingService, "ConversionService must not be null");
		Assert.notNull(formattingService, "FieldType must not be null");
		this.conversionService = formattingService;
		this.fieldType = fieldType;
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		setValue(this.conversionService.convert(text, this.fieldType));
	}

	@Override
	public String getAsText() {
		return this.conversionService.convert(getValue(), String.class);
	}

}
