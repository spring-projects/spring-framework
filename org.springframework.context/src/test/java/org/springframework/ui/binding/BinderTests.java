package org.springframework.ui.binding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.ui.format.DateFormatter;
import org.springframework.ui.format.number.CurrencyFormatter;

public class BinderTests {

	@Before
	public void setUp() {
		LocaleContextHolder.setLocale(Locale.US);
	}
	
	@After
	public void tearDown() {
		LocaleContextHolder.setLocale(null);
	}
	
	@Test
	public void bindSingleValuesWithDefaultTypeConverterConversion() {
		Binder<TestBean> binder = new Binder<TestBean>(new TestBean());
		Map<String, String> propertyValues = new HashMap<String, String>();
		propertyValues.put("string", "test");
		propertyValues.put("integer", "3");
		propertyValues.put("foo", "BAR");
		binder.bind(propertyValues);
		assertEquals("test", binder.getModel().getString());
		assertEquals(3, binder.getModel().getInteger());
		assertEquals(FooEnum.BAR, binder.getModel().getFoo());
	}

	// TODO should update error context, not throw exception
	@Test(expected=IllegalArgumentException.class)
	public void bindSingleValuesWithDefaultTypeCoversionFailures() {
		Binder<TestBean> binder = new Binder<TestBean>(new TestBean());
		Map<String, String> propertyValues = new HashMap<String, String>();
		propertyValues.put("string", "test");
		propertyValues.put("integer", "bogus");
		propertyValues.put("foo", "bogus");
		binder.bind(propertyValues);
	}

	@Test
	public void bindSingleValuePropertyFormatterParsing() throws ParseException {
		Binder<TestBean> binder = new Binder<TestBean>(new TestBean());
		binder.add(new BindingConfiguration("date", new DateFormatter(), false));
		Map<String, String> propertyValues = new HashMap<String, String>();
		propertyValues.put("date", "2009-06-01");
		binder.bind(propertyValues);
		assertEquals(new DateFormatter().parse("2009-06-01", Locale.US), binder.getModel().getDate());
	}

	// TODO should update error context, not throw exception
	@Test(expected=IllegalArgumentException.class)	
	public void bindSingleValuePropertyFormatterParseException() {
		Binder<TestBean> binder = new Binder<TestBean>(new TestBean());
		binder.add(new BindingConfiguration("date", new DateFormatter(), false));
		Map<String, String> propertyValues = new HashMap<String, String>();
		propertyValues.put("date", "bogus");
		binder.bind(propertyValues);
	}

	@Test
	public void bindSingleValueTypeFormatterParsing() throws ParseException {
		Binder<TestBean> binder = new Binder<TestBean>(new TestBean());
		binder.add(new DateFormatter(), Date.class);
		Map<String, String> propertyValues = new HashMap<String, String>();
		propertyValues.put("date", "2009-06-01");
		binder.bind(propertyValues);
		assertEquals(new DateFormatter().parse("2009-06-01", Locale.US), binder.getModel().getDate());
	}
	
	@Test
	public void bindSingleValueAnnotationFormatterParsing() throws ParseException {
		Binder<TestBean> binder = new Binder<TestBean>(new TestBean());
		binder.addAnnotationBasedFormatter(new CurrencyFormatter(), Currency.class);
		Map<String, String> propertyValues = new HashMap<String, String>();
		propertyValues.put("currency", "$23.56");
		binder.bind(propertyValues);
		assertEquals(new BigDecimal("23.56"), binder.getModel().getCurrency());
	}
	
	@Test
	public void getBindingOptimistic() {
		Binder<TestBean> binder = new Binder<TestBean>(new TestBean());
		Binding b = binder.getBinding("integer");
		assertFalse(b.isCollection());
		assertEquals("0", b.getValue());
		b.setValue("5");
		assertEquals("5", b.getValue());
	}

	@Test
	public void getBindingCustomFormatter() {
		Binder<TestBean> binder = new Binder<TestBean>(new TestBean());
		binder.add(new BindingConfiguration("currency", new CurrencyFormatter(), false));
		Binding b = binder.getBinding("currency");
		assertFalse(b.isCollection());
		assertEquals("", b.getValue());
		b.setValue("$23.56");
		assertEquals("$23.56", b.getValue());
	}
	
	@Test
	public void getBindingMultiValued() {
		Binder<TestBean> binder = new Binder<TestBean>(new TestBean());
		Binding b = binder.getBinding("foos");
		// TODO should work - assertTrue(b.isCollection());
		assertEquals(0, b.getValues().length);
		b.setValues(new String[] { "BAR", "BAZ", "BOOP" });
		assertEquals(FooEnum.BAR, binder.getModel().getFoos().get(0));
		assertEquals(FooEnum.BAZ, binder.getModel().getFoos().get(1));
		assertEquals(FooEnum.BOOP, binder.getModel().getFoos().get(2));
		String[] values = b.getValues();
		assertEquals(3, values.length);
		assertEquals("BAR", values[0]);
		assertEquals("BAZ", values[1]);
		assertEquals("BOOP", values[2]);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void getBindingMultiValuedTypeConversionError() {
		Binder<TestBean> binder = new Binder<TestBean>(new TestBean());
		Binding b = binder.getBinding("foos");
		// TODO should work -- assertTrue(b.isCollection());
		assertEquals(0, b.getValues().length);
		b.setValues(new String[] { "BAR", "BOGUS", "BOOP" });
	}

	@Test
	@Ignore
	public void bindHandleNullValueInNestedPath() {
		Binder<TestBean> binder = new Binder<TestBean>(new TestBean());
		Map<String, String> propertyValues = new HashMap<String, String>();
		// TODO should auto add(new Address) at 0
		propertyValues.put("addresses[0].street", "4655 Macy Lane");
		propertyValues.put("addresses[0].city", "Melbourne");
		propertyValues.put("addresses[0].state", "FL");
		propertyValues.put("addresses[0].state", "35452");
		// TODO should auto add(new Address) at 1
		propertyValues.put("addresses[1].street", "1234 Rostock Circle");
		propertyValues.put("addresses[1].city", "Palm Bay");
		propertyValues.put("addresses[1].state", "FL");
		propertyValues.put("addresses[1].state", "32901");
		binder.bind(propertyValues);
	}

	@Test
	public void formatPossibleValue() {
		Binder<TestBean> binder = new Binder<TestBean>(new TestBean());
		binder.add(new BindingConfiguration("currency", new CurrencyFormatter(), false));
		Binding b = binder.getBinding("currency");
		assertEquals("$5.00", b.format(new BigDecimal("5")));
	}
	
	@Test
	public void getBindingRequiredConstraint() {
		Binder<TestBean> binder = new Binder<TestBean>(new TestBean());
		// TODO add constraint API
		binder.add(new BindingConfiguration("string", null, true));
		Binding b = binder.getBinding("string");
		assertEquals("", b.getValue());
		b.setValue("");
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

		@Currency
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

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Currency {

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
}
