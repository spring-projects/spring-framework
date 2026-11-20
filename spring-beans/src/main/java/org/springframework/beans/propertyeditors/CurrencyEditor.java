/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.util.Currency;

import org.springframework.util.StringUtils;

/**
 * Editor for {@code java.util.Currency}, translating currency codes into Currency
 * objects. Exposes the currency code as text representation of a Currency object.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see java.util.Currency
 */
public class CurrencyEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.hasText(text)) {
			text = text.trim();
		}
		setValue(Currency.getInstance(text));
	}

	@Override
	public String getAsText() {
		Currency value = (Currency) getValue();
		return (value != null ? value.getCurrencyCode() : "");
	}

}
