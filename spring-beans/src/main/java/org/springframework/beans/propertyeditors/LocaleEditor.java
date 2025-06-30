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

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;

import org.springframework.util.StringUtils;

/**
 * Editor for {@code java.util.Locale}, to directly populate a Locale property.
 *
 * <p>Expects the same syntax as Locale's {@code toString()}, i.e. language +
 * optionally country + optionally variant, separated by "_" (for example, "en", "en_US").
 * Also accepts spaces as separators, as an alternative to underscores.
 *
 * @author Juergen Hoeller
 * @since 26.05.2003
 * @see java.util.Locale
 * @see org.springframework.util.StringUtils#parseLocale
 */
public class LocaleEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) {
		setValue(StringUtils.parseLocale(text));
	}

	@Override
	public String getAsText() {
		Object value = getValue();
		return (value != null ? value.toString() : "");
	}

}
