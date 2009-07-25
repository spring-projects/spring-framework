package org.springframework.model.ui.binder;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.model.binder.BindingResults;
import org.springframework.model.ui.format.date.DateFormatter;
import org.springframework.model.ui.format.number.CurrencyFormat;
import org.springframework.model.ui.format.number.CurrencyFormatter;
import org.springframework.model.ui.support.DefaultPresentationModel;
import org.springframework.model.ui.support.GenericFormatterRegistry;
import org.springframework.model.ui.support.WebBinder;

public class WebBinderTests {

	TestBean bean = new TestBean();

	DefaultPresentationModel presentationModel;
	
	WebBinder binder;
	
	@Before
	public void setUp() {
		LocaleContextHolder.setLocale(Locale.US);
		presentationModel = new DefaultPresentationModel(bean);
		binder = new WebBinder(presentationModel);		
	}

	@After
	public void tearDown() {
		LocaleContextHolder.setLocale(null);
	}

	@Test
	public void bindUserValuesCreatedFromUserMap() throws ParseException {
		GenericFormatterRegistry registry = new GenericFormatterRegistry();
		registry.add(Date.class, new DateFormatter());
		registry.add(CurrencyFormat.class, new CurrencyFormatter());
		presentationModel.setFormatterRegistry(registry);
		Map<String, String> userMap = new LinkedHashMap<String, String>();
		userMap.put("string", "test");
		userMap.put("_integer", "doesn't matter");
		userMap.put("_bool", "doesn't matter");
		userMap.put("!date", "2009-06-10");
		userMap.put("!currency", "$5.00");
		userMap.put("_currency", "doesn't matter");
		userMap.put("_addresses", "doesn't matter");
		BindingResults results = binder.bind(userMap);
		assertEquals(6, results.size());
		assertEquals("test", results.get(0).getSubmittedValue());
		assertEquals(null, results.get(1).getSubmittedValue());
		assertEquals(Boolean.FALSE, results.get(2).getSubmittedValue());
		assertEquals("2009-06-10", results.get(3).getSubmittedValue());
		assertEquals("$5.00", results.get(4).getSubmittedValue());
		assertEquals(null, results.get(5).getSubmittedValue());

		assertEquals("test", bean.getString());
		assertEquals(0, bean.getInteger());
		assertEquals(new DateFormatter().parse("2009-06-10", Locale.US), bean.getDate());
		assertEquals(false, bean.isBool());
		assertEquals(new BigDecimal("5.00"), bean.getCurrency());
		assertEquals(null, bean.getAddresses());
	}

	public static enum FooEnum {
		BAR, BAZ, BOOP;
	}

	public static class TestBean {
		
		private String string;
		
		private int integer;
		
		private boolean bool;
		
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

		public boolean isBool() {
			return bool;
		}

		public void setBool(boolean bool) {
			this.bool = bool;
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
