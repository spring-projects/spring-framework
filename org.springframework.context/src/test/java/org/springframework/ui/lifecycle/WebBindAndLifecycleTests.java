package org.springframework.ui.lifecycle;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ui.format.number.CurrencyFormat;
import org.springframework.ui.message.MockMessageSource;
import org.springframework.ui.message.Severity;
import org.springframework.ui.message.support.DefaultMessageContext;

public class WebBindAndLifecycleTests {
	
	private WebBindAndValidateLifecycle lifecycle;

	private DefaultMessageContext messages;
	
	@Before
	public void setUp() {
		MockMessageSource messageSource = new MockMessageSource();
		messageSource.addMessage("invalidFormat", Locale.US, "#{label} must be a ${objectType} in format #{format}; parsing of your value '#{value}' failed at the #{errorPosition} character");
		messageSource.addMessage("typeConversionFailure", Locale.US, "The value '#{value}' entered into the #{label} field could not be converted");
		messageSource.addMessage("org.springframework.ui.lifecycle.WebBindAndLifecycleTests$TestBean.integer", Locale.US, "Integer");
		messages = new DefaultMessageContext(messageSource);
		TestBean model = new TestBean();
		lifecycle = new WebBindAndValidateLifecycle(model, messages);
	}
	
	@Test
	public void testExecuteLifecycleNoErrors() {
		Map<String, Object> userMap = new HashMap<String, Object>();
		userMap.put("string", "test");
		userMap.put("integer", "3");
		userMap.put("foo", "BAR");
		lifecycle.execute(userMap);
		assertEquals(0, messages.getMessages().size());
	}

	@Test
	public void testExecuteLifecycleBindingErrors() {
		Map<String, Object> userMap = new HashMap<String, Object>();
		userMap.put("string", "test");
		userMap.put("integer", "bogus");
		userMap.put("foo", "BAR");
		lifecycle.execute(userMap);
		assertEquals(1, messages.getMessages().size());
		assertEquals(Severity.ERROR, messages.getMessages("integer").get(0).getSeverity());
		assertEquals("The value 'bogus' entered into the Integer field could not be converted", messages.getMessages("integer").get(0).getText());
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
