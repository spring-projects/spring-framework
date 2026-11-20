/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.format.number.money;

import java.util.Locale;

import javax.money.CurrencyUnit;
import javax.money.Monetary;

import org.springframework.format.Formatter;

/**
 * Formatter for JSR-354 {@link javax.money.CurrencyUnit} values,
 * from and to currency code Strings.
 *
 * @author Juergen Hoeller
 * @since 4.2
 */
public class CurrencyUnitFormatter implements Formatter<CurrencyUnit> {

	@Override
	public String print(CurrencyUnit object, Locale locale) {
		return object.getCurrencyCode();
	}

	@Override
	public CurrencyUnit parse(String text, Locale locale) {
		return Monetary.getCurrency(text);
	}

}
