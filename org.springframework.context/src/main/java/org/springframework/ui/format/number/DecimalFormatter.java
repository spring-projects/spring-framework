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
 * A BigDecimal formatter for decimal values.
 * Delegates to {@link NumberFormat#getInstance(Locale)}.
 * Configures BigDecimal parsing so there is no loss in precision.
 * Allows configuration over the decimal number pattern; see {@link #DecimalFormatter(String)}.
 * @author Keith Donald
 * @since 3.0
 */
public final class DecimalFormatter implements Formatter<BigDecimal> {

	private DefaultNumberFormatFactory formatFactory = new DefaultNumberFormatFactory();

	private boolean lenient;

	public DecimalFormatter() {
		initDefaults();
	}
	
	public DecimalFormatter(String pattern) {
		initDefaults();
		formatFactory.setPattern(pattern);
	}
	
	public String format(BigDecimal decimal, Locale locale) {
		if (decimal == null) {
			return "";
		}
		NumberFormat format = formatFactory.getNumberFormat(locale);
		return format.format(decimal);
	}

	public BigDecimal parse(String formatted, Locale locale)
			throws ParseException {
		if (formatted.length() == 0) {
			return null;
		}
		NumberFormat format = formatFactory.getNumberFormat(locale);
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
	
	private void initDefaults() {
		formatFactory.setParseBigDecimal(true);
	}

}