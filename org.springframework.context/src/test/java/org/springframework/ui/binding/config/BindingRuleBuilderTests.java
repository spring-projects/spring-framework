package org.springframework.ui.binding.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.ui.format.Formatter;
import org.springframework.ui.format.number.CurrencyFormatter;

public class BindingRuleBuilderTests {
	
	@Test
	@Ignore
	public void createBindingRules() {
		BindingRulesBuilder builder = new BindingRulesBuilder(TestBean.class);
		// TODO ability to add nested rules?
		// TODO ability to format map keys and values?
		builder.bind("string");
		builder.bind("integer").required();
		builder.bind("currency").formatWith(new CurrencyFormatter()).required();
		builder.bind("addresses").formatWith(new AddressListFormatter()).formatElementsWith(new AddressFormatter()).required();
		builder.bind("addresses.street");
		builder.bind("addresses.city");
		builder.bind("addresses.state");
		builder.bind("addresses.zip");
		builder.bind("favoriteFoodsByGroup").formatWith(new FavoriteFoodGroupMapFormatter()).formatElementsWith(new FoodEntryFormatter());
		builder.bind("favoriteFoodsByGroup.name");
		List<BindingRule> rules = builder.getBindingRules();
		assertEquals(10, rules.size());
		assertEquals("string", rules.get(0).getPropertyPath());
		assertNull(rules.get(0).getFormatter());
		assertFalse(rules.get(0).isRequired());
		assertFalse(rules.get(0).isCollectionBinding());
		assertNull(rules.get(0).getValueFormatter());
		assertEquals("integer", rules.get(1).getPropertyPath());
		assertNull(rules.get(1).getFormatter());
		assertTrue(rules.get(1).isRequired());
		assertFalse(rules.get(1).isCollectionBinding());
		assertNull(rules.get(1).getValueFormatter());
		assertEquals("currency", rules.get(2).getPropertyPath());
		assertTrue(rules.get(2).getFormatter() instanceof CurrencyFormatter);
		assertFalse(rules.get(2).isRequired());
		assertFalse(rules.get(2).isCollectionBinding());
		assertNull(rules.get(2).getValueFormatter());
		assertEquals("addresses", rules.get(3).getPropertyPath());
		assertTrue(rules.get(3).getFormatter() instanceof AddressListFormatter);
		assertFalse(rules.get(3).isRequired());
		assertTrue(rules.get(3).isCollectionBinding());
		assertTrue(rules.get(3).getValueFormatter() instanceof AddressFormatter);
		assertTrue(rules.get(8).getFormatter() instanceof FavoriteFoodGroupMapFormatter);
		assertFalse(rules.get(8).isRequired());
		assertTrue(rules.get(8).isCollectionBinding());
		assertTrue(rules.get(8).getValueFormatter() instanceof FoodEntryFormatter);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void createBindingRulesInvalidProperty() {
		BindingRulesBuilder builder = new BindingRulesBuilder(TestBean.class);
		builder.bind("bogus");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void createBindingRulesInvalidNestedCollectionProperty() {
		BindingRulesBuilder builder = new BindingRulesBuilder(TestBean.class);
		builder.bind("addresses.bogus");
	}

	@Test(expected=IllegalArgumentException.class)
	public void createBindingRulesInvalidNestedMapProperty() {
		BindingRulesBuilder builder = new BindingRulesBuilder(TestBean.class);
		builder.bind("favoriteFoodsByGroup.bogus");
	}

	public static enum FooEnum {
		BAR, BAZ, BOOP;
	}

	public static enum FoodGroup {
		DAIRY, VEG, FRUIT, BREAD, MEAT
	}
	
	public static class TestBean {
		private String string;
		private int integer;
		private Date date;
		private FooEnum foo;
		private BigDecimal currency;
		private List<FooEnum> foos;
		private List<Address> addresses;
		private Map<FoodGroup, Food> favoriteFoodsByGroup;
		private Address primaryAddress;
		
		public TestBean() {
		}
		
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

		public Map<FoodGroup, Food> getFavoriteFoodsByGroup() {
			return favoriteFoodsByGroup;
		}

		public void setFavoriteFoodsByGroup(Map<FoodGroup, Food> favoriteFoodsByGroup) {
			this.favoriteFoodsByGroup = favoriteFoodsByGroup;
		}

		public Address getPrimaryAddress() {
			return primaryAddress;
		}

		public void setPrimaryAddress(Address primaryAddress) {
			this.primaryAddress = primaryAddress;
		}
		
	}
	
	public static class Food {
		private String name;

		public Food(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
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
	
	public static class AddressListFormatter implements Formatter<List<Address>> {

		public String format(List<Address> addresses, Locale locale) {
			StringBuilder builder = new StringBuilder();
			for (Address address : addresses) {
				builder.append(new AddressFormatter().format(address, locale));
				builder.append(",");
			}
			return builder.toString();
		}

		public List<Address> parse(String formatted, Locale locale) throws ParseException {
			String[] fields = formatted.split(",");
			List<Address> addresses = new ArrayList<Address>(fields.length);
			for (String field : fields) {
				addresses.add(new AddressFormatter().parse(field, locale));
			}
			return addresses;
		}
		
	}
	
	public static class FavoriteFoodGroupMapFormatter implements Formatter<Map<FoodGroup, Food>> {

		public String format(Map<FoodGroup, Food> map, Locale locale) {
			StringBuilder builder = new StringBuilder();
			return builder.toString();
		}

		public Map<FoodGroup, Food> parse(String formatted, Locale locale) throws ParseException {
			Map<FoodGroup, Food> map = new HashMap<FoodGroup, Food>();
			return map;
		}
		
	}
	
	public static class FoodEntryFormatter implements Formatter<Map.Entry<FoodGroup, Food>> {

		public String format(Map.Entry<FoodGroup, Food> food, Locale locale) {
			return null;
		}

		public Map.Entry<FoodGroup, Food> parse(String formatted, Locale locale) throws ParseException {
			return null;
		}
		
	}
}
