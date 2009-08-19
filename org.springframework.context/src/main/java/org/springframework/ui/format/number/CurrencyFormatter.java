/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.ui.format.number;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;

import org.springframework.ui.format.Formatter;

/**
 * A BigDecimal formatter for currency values.
 * Delegates to {@link NumberFormat#getCurrencyInstance(Locale)}.
 * Configures BigDecimal parsing so there is no loss of precision.
 * Sets the scale of parsed BigDecimal values to {@link NumberFormat#getMaximumFractionDigits()}.
 * Applies {@link RoundingMode#DOWN} to parsed values.
 * @author Keith Donald
 * @since 3.0
 * @see #setLenient(boolean)
 */
public final class CurrencyFormatter implements Formatter<BigDecimal> {

	private CurrencyNumberFormatFactory currencyFormatFactory = new CurrencyNumberFormatFactory();

	private boolean lenient;

	/**
	 * Specify whether or not parsing is to be lenient.
	 * With lenient parsing, the parser may allow inputs that do not precisely match the format.
	 * With strict parsing, inputs must match the format exactly.
	 * Default is false.
	 */
	public void setLenient(boolean lenient) {
		this.lenient = lenient;
	}

	public String format(BigDecimal decimal, Locale locale) {
		if (decimal == null) {
			return "";
		}
		NumberFormat format = currencyFormatFactory.getNumberFormat(locale);
		return format.format(decimal);
	}

	public BigDecimal parse(String formatted, Locale locale)
			throws ParseException {
		if (formatted.length() == 0) {
			return null;
		}		
		NumberFormat format = currencyFormatFactory.getNumberFormat(locale);
		ParsePosition position = new ParsePosition(0);		
		BigDecimal decimal = (BigDecimal) format.parse(formatted, position);
		if (position.getErrorIndex() != -1) {
			throw new ParseException(formatted, position.getIndex());			
		}
		if (!lenient) {
			if (formatted.length() != position.getIndex()) {
				// indicates a part of the string that was not parsed
				throw new ParseException(formatted, position.getIndex());
			}
		}		
		decimal = decimal.setScale(format.getMaximumFractionDigits(), format.getRoundingMode());
		return decimal;
	}
	
}