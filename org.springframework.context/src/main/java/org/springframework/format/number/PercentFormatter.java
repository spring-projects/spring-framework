/*
 * Copyright 2002-2009 the original author or authors.
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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * A Number formatter for percent values.
 *
 * <p>Delegates to {@link NumberFormat#getPercentInstance(Locale)}.
 * Configures BigDecimal parsing so there is no loss in precision.
 * The {@link #parse(String, Locale)} routine always returns a BigDecimal.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 * @see #setLenient
 */
public final class PercentFormatter extends AbstractNumberFormatter {

	protected NumberFormat getNumberFormat(Locale locale) {
		NumberFormat format = NumberFormat.getPercentInstance(locale);
		if (format instanceof DecimalFormat) {
			((DecimalFormat) format).setParseBigDecimal(true);
		}
		return format;
	}

}
