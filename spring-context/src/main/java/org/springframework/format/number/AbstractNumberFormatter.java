/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.format.number;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;

import org.springframework.format.Formatter;

/**
 * Abstract formatter for Numbers,
 * providing a {@link #getNumberFormat(java.util.Locale)} template method.
 *
 * @author Juergen Hoeller
 * @author Keith Donald
 * @since 3.0
 */
public abstract class AbstractNumberFormatter implements Formatter<Number> {

	private boolean lenient = false;


	/**
	 * Specify whether or not parsing is to be lenient. Default is false.
	 * <p>With lenient parsing, the parser may allow inputs that do not precisely match the format.
	 * With strict parsing, inputs must match the format exactly.
	 */
	public void setLenient(boolean lenient) {
		this.lenient = lenient;
	}


	@Override
	public String print(Number number, Locale locale) {
		return getNumberFormat(locale).format(number);
	}

	@Override
	public Number parse(String text, Locale locale) throws ParseException {
		NumberFormat format = getNumberFormat(locale);
		ParsePosition position = new ParsePosition(0);
		Number number = format.parse(text, position);
		if (position.getErrorIndex() != -1) {
			throw new ParseException(text, position.getIndex());
		}
		if (!this.lenient) {
			if (text.length() != position.getIndex()) {
				// indicates a part of the string that was not parsed
				throw new ParseException(text, position.getIndex());
			}
		}
		return number;
	}

	/**
	 * Obtain a concrete NumberFormat for the specified locale.
	 * @param locale the current locale
	 * @return the NumberFormat instance (never {@code null})
	 */
	protected abstract NumberFormat getNumberFormat(Locale locale);

}
