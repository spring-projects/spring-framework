/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.format.number;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Currency;
import java.util.Locale;

import org.springframework.util.ClassUtils;

/**
 * A BigDecimal formatter for currency values.
 *
 * <p>Delegates to {@link NumberFormat#getCurrencyInstance(Locale)}.
 * Configures BigDecimal parsing so there is no loss of precision.
 * Can apply a specified {@link RoundingMode} to parsed values.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 * @see #setLenient
 * @see #setRoundingMode
 */
public class CurrencyFormatter extends AbstractNumberFormatter {

	private static final boolean roundingModeOnDecimalFormat =
			ClassUtils.hasMethod(DecimalFormat.class, "setRoundingMode", RoundingMode.class);

	private int fractionDigits = 2;

	private RoundingMode roundingMode;

	private Currency currency;


	/**
	 * Specify the desired number of fraction digits.
	 * Default is 2.
	 */
	public void setFractionDigits(int fractionDigits) {
		this.fractionDigits = fractionDigits;
	}

	/**
	 * Specify the rounding mode to use for decimal parsing.
	 * Default is {@link RoundingMode#UNNECESSARY}.
	 */
	public void setRoundingMode(RoundingMode roundingMode) {
		this.roundingMode = roundingMode;
	}

	/**
	 * Specify the currency, if known.
	 */
	public void setCurrency(Currency currency) {
		this.currency = currency;
	}


	@Override
	public BigDecimal parse(String text, Locale locale) throws ParseException {
		BigDecimal decimal = (BigDecimal) super.parse(text, locale);
		if (decimal != null) {
			if (this.roundingMode != null) {
				decimal = decimal.setScale(this.fractionDigits, this.roundingMode);
			}
			else {
				decimal = decimal.setScale(this.fractionDigits);
			}
		}
		return decimal;
	}

	@Override
	protected NumberFormat getNumberFormat(Locale locale) {
		DecimalFormat format = (DecimalFormat) NumberFormat.getCurrencyInstance(locale);
		format.setParseBigDecimal(true);
		format.setMaximumFractionDigits(this.fractionDigits);
		format.setMinimumFractionDigits(this.fractionDigits);
		if (this.roundingMode != null && roundingModeOnDecimalFormat) {
			format.setRoundingMode(this.roundingMode);
		}
		if (this.currency != null) {
			format.setCurrency(this.currency);
		}
		return format;
	}

}
