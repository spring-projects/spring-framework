package org.springframework.ui.lifecycle;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ui.alert.Severity;
import org.springframework.ui.alert.support.DefaultAlertContext;
import org.springframework.ui.binding.support.GenericFormatterRegistry;
import org.springframework.ui.format.number.CurrencyFormat;
import org.springframework.ui.format.number.IntegerFormatter;

public class WebBindAndLifecycleTests {

	private WebBindAndValidateLifecycle lifecycle;

	private TestBean model;
	
	private DefaultAlertContext alertContext;

	@Before
	public void setUp() {
		model = new TestBean();
		alertContext = new DefaultAlertContext();
		lifecycle = new WebBindAndValidateLifecycle(model, alertContext);
	}

	@Test
	public void testExecuteLifecycleNoErrors() {
		Map<String, Object> userMap = new HashMap<String, Object>();
		userMap.put("string", "test");
		userMap.put("integer", "3");
		userMap.put("foo", "BAR");
		lifecycle.execute(userMap);
		assertEquals(0, alertContext.getAlerts().size());
	}

	@Test
	public void testExecuteLifecycleBindingErrors() {
		Map<String, Object> userMap = new HashMap<String, Object>();
		userMap.put("string", "test");
		userMap.put("integer", "bogus");
		userMap.put("foo", "BAR");
		lifecycle.execute(userMap);
		assertEquals(1, alertContext.getAlerts().size());
		assertEquals(Severity.FATAL, alertContext.getAlerts("integer").get(0).getSeverity());
		assertEquals("Failed to bind to property 'integer'; user value 'bogus' could not be converted to property type [java.lang.Integer]", alertContext.getAlerts("integer").get(0).getMessage());
	}
	
	@Test
	public void testExecuteLifecycleInvalidFormatBindingErrors() {
		Map<String, Object> userMap = new HashMap<String, Object>();
		GenericFormatterRegistry registry = new GenericFormatterRegistry();
		registry.add(new IntegerFormatter(), Integer.class);
		lifecycle.setFormatterRegistry(registry);
		userMap.put("string", "test");
		userMap.put("integer", "bogus");
		userMap.put("foo", "BAR");
		lifecycle.execute(userMap);
		assertEquals(1, alertContext.getAlerts().size());
		assertEquals(Severity.ERROR, alertContext.getAlerts("integer").get(0).getSeverity());
		assertEquals("Failed to bind to property 'integer'; the user value 'bogus' has an invalid format and could no be parsed", alertContext.getAlerts("integer").get(0).getMessage());
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
}
