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
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;

import org.springframework.ui.format.Formatter;

/**
 * A Number formatter for percent values.
 * Delegates to {@link NumberFormat#getPercentInstance(Locale)}.
 * Configures BigDecimal parsing so there is no loss in precision.
 * The {@link #parse(String, Locale)} routine always returns a BigDecimal.
 * @author Keith Donald
 * @since 3.0
 * @see #setLenient(boolean)
 */
public final class PercentFormatter implements Formatter<Number> {

	private PercentNumberFormatFactory percentFormatFactory = new PercentNumberFormatFactory();

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

	public String format(Number number, Locale locale) {
		if (number == null) {
			return "";
		}
		NumberFormat format = percentFormatFactory.getNumberFormat(locale);
		return format.format(number);
	}

	public Number parse(String formatted, Locale locale)
			throws ParseException {
		if (formatted.length() == 0) {
			return null;
		}
		NumberFormat format = percentFormatFactory.getNumberFormat(locale);
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
		return decimal;
	}

}