package org.springframework.ui.binding.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.ui.binding.Binding;
import org.springframework.ui.binding.BindingResult;
import org.springframework.ui.binding.BindingResults;
import org.springframework.ui.binding.MissingSourceValuesException;
import org.springframework.ui.binding.NoSuchBindingException;
import org.springframework.ui.format.AnnotationFormatterFactory;
import org.springframework.ui.format.Formatter;
import org.springframework.ui.format.date.DateFormatter;
import org.springframework.ui.format.number.CurrencyFormat;
import org.springframework.ui.format.number.CurrencyFormatter;
import org.springframework.ui.format.number.IntegerFormatter;
import org.springframework.ui.message.MockMessageSource;

import edu.emory.mathcs.backport.java.util.Arrays;

public class GenericBinderTests {

	private TestBean bean;

	private GenericBinder binder;

	@Before
	public void setUp() {
		bean = new TestBean();
		binder = new GenericBinder(bean);
		LocaleContextHolder.setLocale(Locale.US);
	}

	@After
	public void tearDown() {
		LocaleContextHolder.setLocale(null);
	}

	@Test
	public void bindSingleValuesWithDefaultTypeConverterConversion() {
		binder.addBinding("string");
		binder.addBinding("integer");
		binder.addBinding("foo");

		Map<String, String> values = new LinkedHashMap<String, String>();
		values.put("string", "test");
		values.put("integer", "3");
		values.put("foo", "BAR");
		BindingResults results = binder.bind(values);
		assertEquals(3, results.size());

		assertEquals("string", results.get(0).getProperty());
		assertFalse(results.get(0).isFailure());
		assertEquals("test", results.get(0).getSourceValue());

		assertEquals("integer", results.get(1).getProperty());
		assertFalse(results.get(1).isFailure());
		assertEquals("3", results.get(1).getSourceValue());

		assertEquals("foo", results.get(2).getProperty());
		assertFalse(results.get(2).isFailure());
		assertEquals("BAR", results.get(2).getSourceValue());

		assertEquals("test", bean.getString());
		assertEquals(3, bean.getInteger());
		assertEquals(FooEnum.BAR, bean.getFoo());
	}

	@Test
	public void bindSingleValuesWithDefaultTypeConversionFailure() {
		binder.addBinding("string");
		binder.addBinding("integer");
		binder.addBinding("foo");

		Map<String, String> values = new LinkedHashMap<String, String>();
		values.put("string", "test");
		// bad value
		values.put("integer", "bogus");
		values.put("foo", "BAR");
		BindingResults results = binder.bind(values);
		assertEquals(3, results.size());
		assertTrue(results.get(1).isFailure());
		assertEquals("conversionFailed", results.get(1).getAlert().getCode());
	}

	@Test
	public void bindSingleValuePropertyFormatter() throws ParseException {
		binder.addBinding("date").formatWith(new DateFormatter());
		binder.bind(Collections.singletonMap("date", "2009-06-01"));
		assertEquals(new DateFormatter().parse("2009-06-01", Locale.US), bean.getDate());
	}

	@Test
	public void bindSingleValuePropertyFormatterParseException() {
		binder.addBinding("date").formatWith(new DateFormatter());
		binder.bind(Collections.singletonMap("date", "bogus"));
	}

	@Test
	public void bindSingleValueWithFormatterRegistedByType() throws ParseException {
		binder.addBinding("date");
		binder.registerFormatter(Date.class, new DateFormatter());
		binder.bind(Collections.singletonMap("date", "2009-06-01"));
		assertEquals(new DateFormatter().parse("2009-06-01", Locale.US), bean.getDate());
	}

	@Test
	public void bindSingleValueWithFormatterRegisteredByAnnotation() throws ParseException {
		binder.addBinding("currency");
		GenericFormatterRegistry registry = new GenericFormatterRegistry();
		registry.add(CurrencyFormat.class, new CurrencyFormatter());
		binder.setFormatterRegistry(registry);
		binder.bind(Collections.singletonMap("currency", "$23.56"));
		assertEquals(new BigDecimal("23.56"), bean.getCurrency());
	}

	@Test
	public void bindSingleValueWithAnnotationFormatterFactoryRegistered() throws ParseException {
		binder.addBinding("currency");
		binder.registerFormatterFactory(new CurrencyAnnotationFormatterFactory());
		binder.bind(Collections.singletonMap("currency", "$23.56"));
		assertEquals(new BigDecimal("23.56"), bean.getCurrency());
	}

	@Test(expected = NoSuchBindingException.class)
	public void bindSingleValuePropertyNotFound() throws ParseException {
		binder.bind(Collections.singletonMap("bogus", "2009-06-01"));
	}

	@Test(expected=MissingSourceValuesException.class)
	public void bindMissingRequiredSourceValue() {
		binder.addBinding("string");
		binder.addBinding("integer").required();
		Map<String, String> userMap = new LinkedHashMap<String, String>();
		userMap.put("string", "test");
		// missing "integer"
		binder.bind(userMap);
	}

	@Test
	public void getBindingCustomFormatter() {
		binder.addBinding("currency").formatWith(new CurrencyFormatter());		
		Binding b = binder.getBinding("currency");
		assertFalse(b.isCollection());
		assertEquals("", b.getValue());
		b.setValue("$23.56");
		assertEquals("$23.56", b.getValue());
	}

	@Test
	public void getBindingCustomFormatterRequiringTypeCoersion() {
		// IntegerFormatter formats Longs, so conversion from Integer -> Long is performed
		binder.addBinding("integer").formatWith(new IntegerFormatter());
		Binding b = binder.getBinding("integer");
		b.setValue("2,300");
		assertEquals("2,300", b.getValue());
	}

	@Test
	public void invalidFormatBindingResultCustomAlertMessage() {
		MockMessageSource messages = new MockMessageSource();
		messages.addMessage("invalidFormat", Locale.US,
				"Please enter an integer in format ### for the #{label} field; you entered #{value}");
		binder.setMessageSource(messages);
		binder.addBinding("integer").formatWith(new IntegerFormatter());
		Binding b = binder.getBinding("integer");
		BindingResult result = b.setValue("bogus");
		assertEquals("Please enter an integer in format ### for the integer field; you entered bogus", result
				.getAlert().getMessage());
	}

	@Test
	public void getBindingMultiValued() {
		binder.addBinding("foos");
		Binding b = binder.getBinding("foos");
		assertTrue(b.isCollection());
		assertEquals(0, b.getCollectionValues().length);
		b.setValue(new String[] { "BAR", "BAZ", "BOOP" });
		assertEquals(FooEnum.BAR, bean.getFoos().get(0));
		assertEquals(FooEnum.BAZ, bean.getFoos().get(1));
		assertEquals(FooEnum.BOOP, bean.getFoos().get(2));
		String[] values = b.getCollectionValues();
		assertEquals(3, values.length);
		assertEquals("BAR", values[0]);
		assertEquals("BAZ", values[1]);
		assertEquals("BOOP", values[2]);
	}

	@Test
	public void getBindingMultiValuedIndexAccess() {
		binder.addBinding("foos");
		bean.setFoos(Arrays.asList(new FooEnum[] { FooEnum.BAR }));
		Binding b = binder.getBinding("foos[0]");
		assertFalse(b.isCollection());
		assertEquals("BAR", b.getValue());
		b.setValue("BAZ");
		assertEquals("BAZ", b.getValue());
	}

	@Test
	public void getBindingMultiValuedTypeConversionFailure() {
		binder.addBinding("foos");
		Binding b = binder.getBinding("foos");
		assertTrue(b.isCollection());
		assertEquals(0, b.getCollectionValues().length);
		BindingResult result = b.setValue(new String[] { "BAR", "BOGUS", "BOOP" });
		assertTrue(result.isFailure());
		assertEquals("conversionFailed", result.getAlert().getCode());
	}

	@Test
	public void bindHandleNullValueInNestedPath() {
		binder.addBinding("addresses.street");
		binder.addBinding("addresses.city");
		binder.addBinding("addresses.state");
		binder.addBinding("addresses.zip");

		Map<String, String> values = new LinkedHashMap<String, String>();

		// EL configured with some options from SpelExpressionParserConfiguration:
		// (see where Binder creates the parser)
		// - new addresses List is created if null
		// - new entries automatically built if List is currently too short - all new entries
		//   are new instances of the type of the list entry, they are not null.
		// not currently doing anything for maps or arrays

		values.put("addresses[0].street", "4655 Macy Lane");
		values.put("addresses[0].city", "Melbourne");
		values.put("addresses[0].state", "FL");
		values.put("addresses[0].zip", "35452");

		// Auto adds new Address at 1
		values.put("addresses[1].street", "1234 Rostock Circle");
		values.put("addresses[1].city", "Palm Bay");
		values.put("addresses[1].state", "FL");
		values.put("addresses[1].zip", "32901");

		// Auto adds new Address at 5 (plus intermediates 2,3,4)
		values.put("addresses[5].street", "1234 Rostock Circle");
		values.put("addresses[5].city", "Palm Bay");
		values.put("addresses[5].state", "FL");
		values.put("addresses[5].zip", "32901");

		BindingResults results = binder.bind(values);
		Assert.assertEquals(6, bean.addresses.size());
		Assert.assertEquals("Palm Bay", bean.addresses.get(1).city);
		Assert.assertNotNull(bean.addresses.get(2));
		assertEquals(12, results.size());
	}

	@Test
	public void formatPossibleValue() {
		binder.addBinding("currency").formatWith(new CurrencyFormatter());
		Binding b = binder.getBinding("currency");
		assertEquals("$5.00", b.format(new BigDecimal("5")));
	}

	public static enum FooEnum {
		BAR, BAZ, BOOP;
	}

	public static class TestBean {
		private String string;
		private int integer;
		private Date date;
		private FooEnum foo;
		private BigDecimal currency;
		private List<FooEnum> foos;
		private List<Address> addresses;

		public String getString() {
			return string;
		}

		public void setString(String string) {
			this.string = string;
		}

		public int getInteger() {
			return integer;
		}

		public void setInteger(int integer) {
			this.integer = integer;
		}

		public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}

		public FooEnum getFoo() {
			return foo;
		}

		public void setFoo(FooEnum foo) {
			this.foo = foo;
		}

		@CurrencyFormat
		public BigDecimal getCurrency() {
			return currency;
		}

		public void setCurrency(BigDecimal currency) {
			this.currency = currency;
		}

		public List<FooEnum> getFoos() {
			return foos;
		}

		public void setFoos(List<FooEnum> foos) {
			this.foos = foos;
		}

		public List<Address> getAddresses() {
			return addresses;
		}

		public void setAddresses(List<Address> addresses) {
			this.addresses = addresses;
		}

	}

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

	}

	public static class CurrencyAnnotationFormatterFactory implements
			AnnotationFormatterFactory<CurrencyFormat, BigDecimal> {
		public Formatter<BigDecimal> getFormatter(CurrencyFormat annotation) {
			return new CurrencyFormatter();
		}
	}

}
