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

package org.springframework.ui.format;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.Locale;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.style.ToStringCreator;
import org.springframework.ui.format.number.CurrencyFormatter;
import org.springframework.ui.format.number.IntegerFormatter;
import org.springframework.ui.format.support.GenericFormatterRegistry;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 */
public class GenericFormatterRegistryTests {

	private GenericFormatterRegistry registry;

	@Before
	public void setUp() {
		registry = new GenericFormatterRegistry();
	}

	@Test
	public void testAdd() throws ParseException {
		registry.addFormatterByType(new IntegerFormatter());
		Formatter formatter = registry.getFormatter(Integer.class);
		String formatted = formatter.format(new Integer(3), Locale.US);
		assertEquals("3", formatted);
		Integer i = (Integer) formatter.parse("3", Locale.US);
		assertEquals(new Integer(3), i);
	}

	@Test
	public void testAddByObjectType() {
		registry.addFormatterByType(BigInteger.class, new IntegerFormatter());
		Formatter formatter = registry.getFormatter(BigInteger.class);
		String formatted = formatter.format(new BigInteger("3"), Locale.US);
		assertEquals("3", formatted);
	}

	@Test
	public void testAddByAnnotation() throws Exception {
		registry.addFormatterByAnnotation(Currency.class, new CurrencyFormatter());
		Formatter formatter = registry.getFormatter(new TypeDescriptor(getClass().getField("currencyField")));
		String formatted = formatter.format(new BigDecimal("5.00"), Locale.US);
		assertEquals("$5.00", formatted);
	}

	@Test
	public void testAddAnnotationFormatterFactory() throws Exception {
		registry.addFormatterByAnnotation(new CurrencyAnnotationFormatterFactory());
		Formatter formatter = registry.getFormatter(new TypeDescriptor(getClass().getField("currencyField")));
		String formatted = formatter.format(new BigDecimal("5.00"), Locale.US);
		assertEquals("$5.00", formatted);
	}

	@Test
	public void testGetDefaultFormatterFromMetaAnnotation() throws Exception {
		Formatter formatter = registry.getFormatter(new TypeDescriptor(getClass().getField("smartCurrencyField")));
		String formatted = formatter.format(new BigDecimal("5.00"), Locale.US);
		assertEquals("$5.00", formatted);
	}

	@Test
	public void testGetDefaultFormatterForType() {
		Formatter formatter = registry.getFormatter(Address.class);
		Address address = new Address();
		address.street = "12345 Bel Aire Estates";
		address.city = "Palm Bay";
		address.state = "FL";
		address.zip = "12345";
		String formatted = formatter.format(address, Locale.US);
		assertEquals("12345 Bel Aire Estates:Palm Bay:FL:12345", formatted);
	}
	
	@Test
	public void testGetNoFormatterForType() {
		assertNull(registry.getFormatter(Integer.class));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetFormatterCannotConvert() {
		registry.addFormatterByType(Integer.class, new AddressFormatter());
	}
	

	@Currency
	public BigDecimal currencyField;

	@SmartCurrency
	public BigDecimal smartCurrencyField;

	@Target({ElementType.METHOD, ElementType.FIELD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Currency {
	}

	@Target({ElementType.METHOD, ElementType.FIELD})
	@Retention(RetentionPolicy.RUNTIME)
	@Formatted(CurrencyFormatter.class)
	public @interface SmartCurrency {
	}

	public static class CurrencyAnnotationFormatterFactory implements AnnotationFormatterFactory<Currency, Number> {
		
		private final CurrencyFormatter currencyFormatter = new CurrencyFormatter();
		
		public Formatter<Number> getFormatter(Currency annotation) {
			return this.currencyFormatter;
		}
	}
	
	@Formatted(AddressFormatter.class)
	public static class Address {

		private String street;
		private String city;
		private String state;
		private String zip;
		private String country;

		public String getStreet() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}

		public String getState() {
			return state;
		}

		public void setState(String state) {
			this.state = state;
		}

		public String getZip() {
			return zip;
		}

		public void setZip(String zip) {
			this.zip = zip;
		}

		public String getCountry() {
			return country;
		}

		public void setCountry(String country) {
			this.country = country;
		}

		public String toString() {
			return new ToStringCreator(this).append("street", street).append("city", city).append("state", state)
					.append("zip", zip).toString();
		}
	}

	public static class AddressFormatter implements Formatter<Address> {

		public String format(Address address, Locale locale) {
			return address.getStreet() + ":" + address.getCity() + ":" + address.getState() + ":" + address.getZip();
		}

		public Address parse(String formatted, Locale locale) throws ParseException {
			Address address = new Address();
			String[] fields = formatted.split(":");
			address.setStreet(fields[0]);
			address.setCity(fields[1]);
			address.setState(fields[2]);
			address.setZip(fields[3]);
			return address;
		}
	}

}
