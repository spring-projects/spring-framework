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

import java.util.Locale;
import javax.money.CurrencyUnit;
import javax.money.MonetaryAmount;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.validation.DataBinder;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @since 4.2
 */
public class MoneyFormattingTests {

	private final FormattingConversionService conversionService = new DefaultFormattingConversionService();


	@Before
	public void setUp() {
		LocaleContextHolder.setLocale(Locale.US);
	}

	@After
	public void tearDown() {
		LocaleContextHolder.setLocale(null);
	}


	@Test
	public void testAmountAndUnit() {
		DataBinder binder = new DataBinder(new MoneyHolder());
		binder.setConversionService(conversionService);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("amount", "USD 10.50");
		propertyValues.add("unit", "USD");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("USD10.50", binder.getBindingResult().getFieldValue("amount"));
		assertEquals("USD", binder.getBindingResult().getFieldValue("unit"));
	}

	@Test
	public void testAmountWithNumberFormat1() {
		DataBinder binder = new DataBinder(new FormattedMoneyHolder1());
		binder.setConversionService(conversionService);

		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("amount", "$10.50");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("$10.50", binder.getBindingResult().getFieldValue("amount"));

		/* TODO: preserve currency from given value
		LocaleContextHolder.setLocale(Locale.CANADA);
		binder.bind(propertyValues);
		LocaleContextHolder.setLocale(Locale.US);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("$10.50", binder.getBindingResult().getFieldValue("amount"));
		*/
	}

	@Test
	public void testAmountWithNumberFormat2() {
		DataBinder binder = new DataBinder(new FormattedMoneyHolder2());
		binder.setConversionService(conversionService);

		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("amount", "10.50");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10.5", binder.getBindingResult().getFieldValue("amount"));
	}

	@Test
	public void testAmountWithNumberFormat3() {
		DataBinder binder = new DataBinder(new FormattedMoneyHolder3());
		binder.setConversionService(conversionService);

		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("amount", "10%");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10%", binder.getBindingResult().getFieldValue("amount"));
	}

	@Test
	public void testAmountWithNumberFormat4() {
		DataBinder binder = new DataBinder(new FormattedMoneyHolder4());
		binder.setConversionService(conversionService);

		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("amount", "010.500");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("010.500", binder.getBindingResult().getFieldValue("amount"));
	}

	@Test
	public void testAmountWithNumberFormat5() {
		DataBinder binder = new DataBinder(new FormattedMoneyHolder5());
		binder.setConversionService(conversionService);

		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("amount", "$ 10.50");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("$ 010.500", binder.getBindingResult().getFieldValue("amount"));

		/* TODO: preserve currency from given value
		LocaleContextHolder.setLocale(Locale.CANADA);
		binder.bind(propertyValues);
		LocaleContextHolder.setLocale(Locale.US);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("$ 010.500", binder.getBindingResult().getFieldValue("amount"));
		*/
	}


	public static class MoneyHolder {

		private MonetaryAmount amount;

		private CurrencyUnit unit;

		public MonetaryAmount getAmount() {
			return amount;
		}

		public void setAmount(MonetaryAmount amount) {
			this.amount = amount;
		}

		public CurrencyUnit getUnit() {
			return unit;
		}

		public void setUnit(CurrencyUnit unit) {
			this.unit = unit;
		}
	}


	public static class FormattedMoneyHolder1 {

		@NumberFormat
		private MonetaryAmount amount;

		public MonetaryAmount getAmount() {
			return amount;
		}

		public void setAmount(MonetaryAmount amount) {
			this.amount = amount;
		}
	}


	public static class FormattedMoneyHolder2 {

		@NumberFormat(style = NumberFormat.Style.NUMBER)
		private MonetaryAmount amount;

		public MonetaryAmount getAmount() {
			return amount;
		}

		public void setAmount(MonetaryAmount amount) {
			this.amount = amount;
		}
	}


	public static class FormattedMoneyHolder3 {

		@NumberFormat(style = NumberFormat.Style.PERCENT)
		private MonetaryAmount amount;

		public MonetaryAmount getAmount() {
			return amount;
		}

		public void setAmount(MonetaryAmount amount) {
			this.amount = amount;
		}
	}


	public static class FormattedMoneyHolder4 {

		@NumberFormat(pattern = "#000.000#")
		private MonetaryAmount amount;

		public MonetaryAmount getAmount() {
			return amount;
		}

		public void setAmount(MonetaryAmount amount) {
			this.amount = amount;
		}
	}


	public static class FormattedMoneyHolder5 {

		@NumberFormat(pattern = "\u00A4 #000.000#")
		private MonetaryAmount amount;

		public MonetaryAmount getAmount() {
			return amount;
		}

		public void setAmount(MonetaryAmount amount) {
			this.amount = amount;
		}
	}

}
