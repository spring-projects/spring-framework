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

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;

import org.springframework.ui.format.Formatter;

/**
 * A Long formatter for whole integer values.
 * Delegates to {@link NumberFormat#getIntegerInstance(Locale)}.
 * @author Keith Donald
 */
public class IntegerFormatter implements Formatter<Long> {

	private IntegerNumberFormatFactory formatFactory = new IntegerNumberFormatFactory();

	private boolean lenient;

	public String format(Long integer, Locale locale) {
		if (integer == null) {
			return "";
		}
		NumberFormat format = formatFactory.getNumberFormat(locale);
		return format.format(integer);
	}

	public Long parse(String formatted, Locale locale)
			throws ParseException {
		if (formatted.length() == 0) {
			return null;
		}
		NumberFormat format = formatFactory.getNumberFormat(locale);
		ParsePosition position = new ParsePosition(0);		
		Long integer = (Long) format.parse(formatted, position);
		if (position.getErrorIndex() != -1) {
			throw new ParseException(formatted, position.getIndex());			
		}
		if (!lenient) {
			if (formatted.length() != position.getIndex()) {
				// indicates a part of the string that was not parsed
				throw new ParseException(formatted, position.getIndex());
			}
		}
		return integer;
	}

}