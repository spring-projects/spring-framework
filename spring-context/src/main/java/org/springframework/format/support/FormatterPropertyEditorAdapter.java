/*
 * Copyright 2002-2016 the original author or authors.
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

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.text.ParseException;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.Formatter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Adapter that bridges between {@link Formatter} and {@link PropertyEditor}.
 *
 * @author Juergen Hoeller
 * @since 4.2
 */
public class FormatterPropertyEditorAdapter extends PropertyEditorSupport {

	private final Formatter<Object> formatter;


	/**
	 * Create a new {@code FormatterPropertyEditorAdapter} for the given {@link Formatter}.
	 * @param formatter the {@link Formatter} to wrap
	 */
	@SuppressWarnings("unchecked")
	public FormatterPropertyEditorAdapter(Formatter<?> formatter) {
		Assert.notNull(formatter, "Formatter must not be null");
		this.formatter = (Formatter<Object>) formatter;
	}


	/**
	 * Determine the {@link Formatter}-declared field type.
	 * @return the field type declared in the wrapped {@link Formatter} implementation
	 * (never {@code null})
	 * @throws IllegalArgumentException if the {@link Formatter}-declared field type
	 * cannot be inferred
	 */
	public Class<?> getFieldType() {
		return FormattingConversionService.getFieldType(this.formatter);
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.hasText(text)) {
			try {
				setValue(this.formatter.parse(text, LocaleContextHolder.getLocale()));
			}
			catch (ParseException ex) {
				throw new IllegalArgumentException("Parse attempt failed for value [" + text + "]", ex);
			}
		}
		else {
			setValue(null);
		}
	}

	@Override
	public String getAsText() {
		Object value = getValue();
		return (value != null ? this.formatter.print(value, LocaleContextHolder.getLocale()) : "");
	}

}
