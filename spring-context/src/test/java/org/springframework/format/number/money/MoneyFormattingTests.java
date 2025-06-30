/*
 * Copyright 2002-present the original author or authors.
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

import javax.money.CurrencyUnit;
import javax.money.MonetaryAmount;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.validation.DataBinder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @since 4.2
 */
class MoneyFormattingTests {

	private final FormattingConversionService conversionService = new DefaultFormattingConversionService();


	@BeforeEach
	void setUp() {
		LocaleContextHolder.setLocale(Locale.US);
	}

	@AfterEach
	void tearDown() {
		LocaleContextHolder.setLocale(null);
	}


	@Test
	void testAmountAndUnit() {
		MoneyHolder bean = new MoneyHolder();
		DataBinder binder = new DataBinder(bean);
		binder.setConversionService(conversionService);

		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("amount", "USD 10.50");
		propertyValues.add("unit", "USD");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("amount")).isEqualTo("USD10.50");
		assertThat(binder.getBindingResult().getFieldValue("unit")).isEqualTo("USD");
		assertThat(bean.getAmount().getNumber().doubleValue()).isEqualTo(10.5d);
		assertThat(bean.getAmount().getCurrency().getCurrencyCode()).isEqualTo("USD");

		LocaleContextHolder.setLocale(Locale.CANADA);
		binder.bind(propertyValues);
		LocaleContextHolder.setLocale(Locale.US);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("amount")).isEqualTo("USD10.50");
		assertThat(binder.getBindingResult().getFieldValue("unit")).isEqualTo("USD");
		assertThat(bean.getAmount().getNumber().doubleValue()).isEqualTo(10.5d);
		assertThat(bean.getAmount().getCurrency().getCurrencyCode()).isEqualTo("USD");
	}

	@Test
	void testAmountWithNumberFormat1() {
		FormattedMoneyHolder1 bean = new FormattedMoneyHolder1();
		DataBinder binder = new DataBinder(bean);
		binder.setConversionService(conversionService);

		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("amount", "$10.50");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("amount")).isEqualTo("$10.50");
		assertThat(bean.getAmount().getNumber().doubleValue()).isEqualTo(10.5d);
		assertThat(bean.getAmount().getCurrency().getCurrencyCode()).isEqualTo("USD");

		LocaleContextHolder.setLocale(Locale.CANADA);
		binder.bind(propertyValues);
		LocaleContextHolder.setLocale(Locale.US);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("amount")).isEqualTo("$10.50");
		assertThat(bean.getAmount().getNumber().doubleValue()).isEqualTo(10.5d);
		assertThat(bean.getAmount().getCurrency().getCurrencyCode()).isEqualTo("CAD");
	}

	@Test
	void testAmountWithNumberFormat2() {
		FormattedMoneyHolder2 bean = new FormattedMoneyHolder2();
		DataBinder binder = new DataBinder(bean);
		binder.setConversionService(conversionService);

		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("amount", "10.50");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("amount")).isEqualTo("10.5");
		assertThat(bean.getAmount().getNumber().doubleValue()).isEqualTo(10.5d);
		assertThat(bean.getAmount().getCurrency().getCurrencyCode()).isEqualTo("USD");
	}

	@Test
	void testAmountWithNumberFormat3() {
		FormattedMoneyHolder3 bean = new FormattedMoneyHolder3();
		DataBinder binder = new DataBinder(bean);
		binder.setConversionService(conversionService);

		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("amount", "10%");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("amount")).isEqualTo("10%");
		assertThat(bean.getAmount().getNumber().doubleValue()).isEqualTo(0.1d);
		assertThat(bean.getAmount().getCurrency().getCurrencyCode()).isEqualTo("USD");
	}

	@Test
	void testAmountWithNumberFormat4() {
		FormattedMoneyHolder4 bean = new FormattedMoneyHolder4();
		DataBinder binder = new DataBinder(bean);
		binder.setConversionService(conversionService);

		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("amount", "010.500");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("amount")).isEqualTo("010.500");
		assertThat(bean.getAmount().getNumber().doubleValue()).isEqualTo(10.5d);
		assertThat(bean.getAmount().getCurrency().getCurrencyCode()).isEqualTo("USD");
	}

	@Test
	void testAmountWithNumberFormat5() {
		FormattedMoneyHolder5 bean = new FormattedMoneyHolder5();
		DataBinder binder = new DataBinder(bean);
		binder.setConversionService(conversionService);

		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("amount", "USD 10.50");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("amount")).isEqualTo("USD 010.500");
		assertThat(bean.getAmount().getNumber().doubleValue()).isEqualTo(10.5d);
		assertThat(bean.getAmount().getCurrency().getCurrencyCode()).isEqualTo("USD");

		LocaleContextHolder.setLocale(Locale.CANADA);
		binder.bind(propertyValues);
		LocaleContextHolder.setLocale(Locale.US);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("amount")).isEqualTo("USD 010.500");
		assertThat(bean.getAmount().getNumber().doubleValue()).isEqualTo(10.5d);
		assertThat(bean.getAmount().getCurrency().getCurrencyCode()).isEqualTo("USD");
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

		@NumberFormat(pattern = "\u00A4\u00A4 #000.000#")
		private MonetaryAmount amount;

		public MonetaryAmount getAmount() {
			return amount;
		}

		public void setAmount(MonetaryAmount amount) {
			this.amount = amount;
		}
	}

}
