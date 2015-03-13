/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.format.number.money;

import java.text.ParseException;
import java.util.Collections;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import javax.money.CurrencyUnit;
import javax.money.MonetaryAmount;
import javax.money.MonetaryAmounts;
import javax.money.MonetaryCurrencies;

import org.springframework.context.support.EmbeddedValueResolutionSupport;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Formatter;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.format.annotation.NumberFormat.Style;
import org.springframework.format.number.CurrencyStyleFormatter;
import org.springframework.format.number.NumberStyleFormatter;
import org.springframework.format.number.PercentStyleFormatter;
import org.springframework.util.StringUtils;

/**
 * Formats {@link javax.money.MonetaryAmount} fields annotated
 * with Spring's common {@link NumberFormat} annotation.
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see NumberFormat
 */
public class Jsr354NumberFormatAnnotationFormatterFactory extends EmbeddedValueResolutionSupport
		implements AnnotationFormatterFactory<NumberFormat> {

	@Override
	@SuppressWarnings("unchecked")
	public Set<Class<?>> getFieldTypes() {
		return (Set) Collections.singleton(MonetaryAmount.class);
	}

	@Override
	public Printer<MonetaryAmount> getPrinter(NumberFormat annotation, Class<?> fieldType) {
		return configureFormatterFrom(annotation);
	}

	@Override
	public Parser<MonetaryAmount> getParser(NumberFormat annotation, Class<?> fieldType) {
		return configureFormatterFrom(annotation);
	}


	private Formatter<MonetaryAmount> configureFormatterFrom(NumberFormat annotation) {
		if (StringUtils.hasLength(annotation.pattern())) {
			return new NumberDecoratingFormatter(null, resolveEmbeddedValue(annotation.pattern()));
		}
		else {
			Style style = annotation.style();
			if (style == Style.PERCENT) {
				return new NumberDecoratingFormatter(new PercentStyleFormatter(), null);
			}
			else if (style == Style.NUMBER) {
				return new NumberDecoratingFormatter(new NumberStyleFormatter(), null);
			}
			else {
				return new NumberDecoratingFormatter(null, null);
			}
		}
	}


	private static class NumberDecoratingFormatter implements Formatter<MonetaryAmount> {

		private final Formatter<Number> numberFormatter;

		private final String pattern;

		public NumberDecoratingFormatter(Formatter<Number> numberFormatter, String pattern) {
			this.numberFormatter = numberFormatter;
			this.pattern = pattern;
		}

		@Override
		public String print(MonetaryAmount object, Locale locale) {
			Formatter<Number> formatterToUse = this.numberFormatter;
			if (formatterToUse == null) {
				CurrencyStyleFormatter formatter = new CurrencyStyleFormatter();
				formatter.setCurrency(Currency.getInstance(object.getCurrency().getCurrencyCode()));
				formatter.setPattern(this.pattern);
				formatterToUse = formatter;
			}
			return formatterToUse.print(object.getNumber(), locale);
		}

		@Override
		public MonetaryAmount parse(String text, Locale locale) throws ParseException {
			Currency currency = Currency.getInstance(locale);
			Formatter<Number> formatterToUse = this.numberFormatter;
			if (formatterToUse == null) {
				CurrencyStyleFormatter formatter = new CurrencyStyleFormatter();
				formatter.setCurrency(currency);
				formatter.setPattern(this.pattern);
				formatterToUse = formatter;
			}
			Number numberValue = formatterToUse.parse(text, locale);
			CurrencyUnit currencyUnit = MonetaryCurrencies.getCurrency(currency.getCurrencyCode());
			return MonetaryAmounts.getDefaultAmountFactory().setNumber(numberValue).setCurrency(currencyUnit).create();
		}
	}

}
