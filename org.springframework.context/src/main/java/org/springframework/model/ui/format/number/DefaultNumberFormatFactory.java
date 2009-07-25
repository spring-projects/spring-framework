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
package org.springframework.model.ui.format.number;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Works with a general purpose {@link DecimalFormat} instance returned by calling
 * {@link NumberFormat#getInstance(Locale)} by default.
 * @author Keith Donald
 * @see NumberFormat
 * @see DecimalFormat
 * @since 3.0
 */
class DefaultNumberFormatFactory extends NumberFormatFactory {

	private static Log logger = LogFactory.getLog(DefaultNumberFormatFactory.class);

	private String pattern;

	private Boolean parseBigDecimal;
	
	/**
	 * Sets the pattern to use to format number values.
	 * If not specified, the default DecimalFormat pattern is used.
	 * @param pattern the format pattern
	 * @see DecimalFormat#applyPattern(String)
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * Sets whether the format should always parse a big decimal.
	 * @param parseBigDecimal the big decimal parse status
	 * @see DecimalFormat#setParseBigDecimal(boolean)
	 */
	public void setParseBigDecimal(boolean parseBigDecimal) {
		this.parseBigDecimal = parseBigDecimal;
	}

	public NumberFormat getNumberFormat(Locale locale) {
		NumberFormat format = NumberFormat.getInstance(locale);
		if (pattern != null) {
			if (format instanceof DecimalFormat) {
				((DecimalFormat) format).applyPattern(pattern);
			} else {
				logger.warn("Unable to apply format pattern '" + pattern
						+ "'; Returned NumberFormat is not a DecimalFormat");
			}
		}
		if (parseBigDecimal != null) {
			if (format instanceof DecimalFormat) {
				((DecimalFormat) format).setParseBigDecimal(parseBigDecimal);
			} else {
				logger.warn("Unable to call setParseBigDecimal; not a DecimalFormat");
			}
		}
		return format;
	}

}