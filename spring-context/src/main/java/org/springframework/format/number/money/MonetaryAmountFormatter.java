/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.format.number.money;

import java.util.Locale;
import javax.money.MonetaryAmount;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryFormats;

import org.springframework.format.Formatter;
import org.springframework.lang.Nullable;

/**
 * Formatter for JSR-354 {@link javax.money.MonetaryAmount} values,
 * delegating to {@link javax.money.format.MonetaryAmountFormat#format}
 * and {@link javax.money.format.MonetaryAmountFormat#parse}.
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see #getMonetaryAmountFormat
 */
public class MonetaryAmountFormatter implements Formatter<MonetaryAmount> {

	@Nullable
	private String formatName;


	/**
	 * Create a locale-driven MonetaryAmountFormatter.
	 */
	public MonetaryAmountFormatter() {
	}

	/**
	 * Create a new MonetaryAmountFormatter for the given format name.
	 * @param formatName the format name, to be resolved by the JSR-354
	 * provider at runtime
	 */
	public MonetaryAmountFormatter(String formatName) {
		this.formatName = formatName;
	}


	/**
	 * Specify the format name, to be resolved by the JSR-354 provider
	 * at runtime.
	 * <p>Default is none, obtaining a {@link MonetaryAmountFormat}
	 * based on the current locale.
	 */
	public void setFormatName(String formatName) {
		this.formatName = formatName;
	}


	@Override
	public String print(MonetaryAmount object, Locale locale) {
		return getMonetaryAmountFormat(locale).format(object);
	}

	@Override
	public MonetaryAmount parse(String text, Locale locale) {
		return getMonetaryAmountFormat(locale).parse(text);
	}


	/**
	 * Obtain a MonetaryAmountFormat for the given locale.
	 * <p>The default implementation simply calls
	 * {@link javax.money.format.MonetaryFormats#getAmountFormat}
	 * with either the configured format name or the given locale.
	 * @param locale the current locale
	 * @return the MonetaryAmountFormat (never {@code null})
	 * @see #setFormatName
	 */
	protected MonetaryAmountFormat getMonetaryAmountFormat(Locale locale) {
		if (this.formatName != null) {
			return MonetaryFormats.getAmountFormat(this.formatName);
		}
		else {
			return MonetaryFormats.getAmountFormat(locale);
		}
	}

}
