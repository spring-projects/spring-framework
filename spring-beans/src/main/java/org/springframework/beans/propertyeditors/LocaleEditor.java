/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
