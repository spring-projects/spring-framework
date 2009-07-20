package org.springframework.ui.binding.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.ui.binding.BindingResults;
import org.springframework.ui.format.AnnotationFormatterFactory;
import org.springframework.ui.format.Formatted;
import org.springframework.ui.format.Formatter;
import org.springframework.ui.format.date.DateFormatter;
import org.springframework.ui.format.number.CurrencyFormat;
import org.springframework.ui.format.number.CurrencyFormatter;

public class GenericBinderTests {

	private TestBean bean;

	@Before
	public void setUp() {
		bean = new TestBean();
		LocaleContextHolder.setLocale(Locale.US);
	}

	@After
	public void tearDown() {
		LocaleContextHolder.setLocale(null);
	}

	@Test
	public void testPlaceholder() {
		
	}
	
	@Test
	public void bindSingleValuesWithDefaultTypeConverterConversion() {
		GenericBinder binder = new GenericBinder(bean);
		
		Map<String, String> values = new LinkedHashMap<String, String>();
		values.put("string", "test");
		values.put("integer", "3");
		values.put("foo", "BAR");
		BindingResults results = binder.bind(values);
		System.out.println(results);
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
		GenericBinder binder = new GenericBinder(bean);
		Map<String, String> values = new LinkedHashMap<String, String>();
		values.put("string", "test");
		// bad value
		values.put("integer", "bogus");
		values.put("foo", "BAR");
		BindingResults results = binder.bind(values);
		assertEquals(3, results.size());
		assertTrue(results.get(1).isFailure());
		assertEquals("typeMismatch", results.get(1).getAlert().getCode());
	}

	@Test
	public void bindSingleValuePropertyFormatter() throws ParseException {
		GenericBinder binder = new GenericBinder(bean);
		binder.bindingRule("date").formatWith(new DateFormatter());
		binder.bind(Collections.singletonMap("date", "2009-06-01"));
		assertEquals(new DateFormatter().parse("2009-06-01", Locale.US), bean.getDate());
	}

	/*
	@Test
	public void bindSingleValuePropertyFormatterParseException() {
		BindingRulesBuilder builder = new BindingRulesBuilder(TestBean.class);
		builder.bind("date").formatWith(new DateFormatter());
		GenericBinder binder = new GenericBinder(bean, builder.getBindingRules());

		BindingResults results = binder.bind(Collections.singletonMap("date", "bogus"));
		assertEquals(1, results.size());
		assertTrue(results.get(0).isFailure());
		assertEquals("invalidFormat", results.get(0).getAlert().getCode());		
	}

	@Test
	public void bindSingleValueWithFormatterRegistedByType() throws ParseException {
		BindingRulesBuilder builder = new BindingRulesBuilder(TestBean.class);
		builder.bind("date").formatWith(new DateFormatter());
		GenericBinder binder = new GenericBinder(bean, builder.getBindingRules());
		
		GenericFormatterRegistry formatterRegistry = new GenericFormatterRegistry();
		formatterRegistry.add(Date.class, new DateFormatter());
		
		binder.bind(Collections.singletonMap("date", "2009-06-01"));
		assertEquals(new DateFormatter().parse("2009-06-01", Locale.US), bean.getDate());
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
		assertFalse(b.isIndexable());
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
		assertTrue(b.isIndexable());
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
		assertFalse(b.isIndexable());
		assertEquals("BAR", b.getValue());
		b.setValue("BAZ");
		assertEquals("BAZ", b.getValue());
	}

	@Test
	public void getBindingMultiValuedTypeConversionFailure() {
		binder.addBinding("foos");
		Binding b = binder.getBinding("foos");
		assertTrue(b.isIndexable());
		assertEquals(0, b.getCollectionValues().length);
		BindingResult result = b.setValue(new String[] { "BAR", "BOGUS", "BOOP" });
		assertTrue(result.isFailure());
		assertEquals("conversionFailed", result.getAlert().getCode());
	}

	@Test
	public void bindToList() {
		binder.addBinding("addresses");
		Map<String, String[]> values = new LinkedHashMap<String, String[]>();
		values.put("addresses", new String[] { "4655 Macy Lane:Melbourne:FL:35452", "1234 Rostock Circle:Palm Bay:FL:32901", "1977 Bel Aire Estates:Coker:AL:12345" });		
		binder.bind(values);
		Assert.assertEquals(3, bean.addresses.size());
		assertEquals("4655 Macy Lane", bean.addresses.get(0).street);
		assertEquals("Melbourne", bean.addresses.get(0).city);
		assertEquals("FL", bean.addresses.get(0).state);
		assertEquals("35452", bean.addresses.get(0).zip);
	}
	
	@Test
	public void bindToListElements() {
		binder.addBinding("addresses");
		Map<String, String> values = new LinkedHashMap<String, String>();
		values.put("addresses[0]", "4655 Macy Lane:Melbourne:FL:35452");		
		values.put("addresses[1]", "1234 Rostock Circle:Palm Bay:FL:32901");	
		values.put("addresses[5]", "1977 Bel Aire Estates:Coker:AL:12345");
		binder.bind(values);
		Assert.assertEquals(6, bean.addresses.size());
		assertEquals("4655 Macy Lane", bean.addresses.get(0).street);
		assertEquals("Melbourne", bean.addresses.get(0).city);
		assertEquals("FL", bean.addresses.get(0).state);
		assertEquals("35452", bean.addresses.get(0).zip);
	}

	@Test
	public void bindToListSingleString() {
		binder.addBinding("addresses");
		binder.registerFormatter(new GenericCollectionPropertyType(List.class, Address.class), new AddressListFormatter());
		Map<String, String> values = new LinkedHashMap<String, String>();
		values.put("addresses", "4655 Macy Lane:Melbourne:FL:35452,1234 Rostock Circle:Palm Bay:FL:32901,1977 Bel Aire Estates:Coker:AL:12345");		
		BindingResults results = binder.bind(values);
		Assert.assertEquals(3, bean.addresses.size());
		assertEquals("4655 Macy Lane", bean.addresses.get(0).street);
		assertEquals("Melbourne", bean.addresses.get(0).city);
		assertEquals("FL", bean.addresses.get(0).state);
		assertEquals("35452", bean.addresses.get(0).zip);
		assertEquals("1234 Rostock Circle", bean.addresses.get(1).street);
		assertEquals("Palm Bay", bean.addresses.get(1).city);
		assertEquals("FL", bean.addresses.get(1).state);
		assertEquals("32901", bean.addresses.get(1).zip);
		assertEquals("1977 Bel Aire Estates", bean.addresses.get(2).street);
		assertEquals("Coker", bean.addresses.get(2).city);
		assertEquals("AL", bean.addresses.get(2).state);
		assertEquals("12345", bean.addresses.get(2).zip);
	}
	
	@Test
	@Ignore
	public void bindToListSingleStringNoListFormatter() {
		binder.addBinding("addresses");
		//binder.registerFormatter(new GenericCollectionPropertyType(List.class, Address.class), new AddressListFormatter());
		Map<String, String> values = new LinkedHashMap<String, String>();
		values.put("addresses", "4655 Macy Lane:Melbourne:FL:35452,1234 Rostock Circle:Palm Bay:FL:32901,1977 Bel Aire Estates:Coker:AL:12345");		
		BindingResults results = binder.bind(values);
		Assert.assertEquals(3, bean.addresses.size());
		assertEquals("4655 Macy Lane", bean.addresses.get(0).street);
		assertEquals("Melbourne", bean.addresses.get(0).city);
		assertEquals("FL", bean.addresses.get(0).state);
		assertEquals("35452", bean.addresses.get(0).zip);
		assertEquals("1234 Rostock Circle", bean.addresses.get(1).street);
		assertEquals("Palm Bay", bean.addresses.get(1).city);
		assertEquals("FL", bean.addresses.get(1).state);
		assertEquals("32901", bean.addresses.get(1).zip);
		assertEquals("1977 Bel Aire Estates", bean.addresses.get(2).street);
		assertEquals("Coker", bean.addresses.get(2).city);
		assertEquals("AL", bean.addresses.get(2).state);
		assertEquals("12345", bean.addresses.get(2).zip);
	}
	
	@Test
	public void getListAsSingleString() {
		binder.addBinding("addresses");
		binder.registerFormatter(new GenericCollectionPropertyType(List.class, Address.class), new AddressListFormatter());
		Address address1 = new Address();
		address1.setStreet("s1");
		address1.setCity("c1");
		address1.setState("st1");
		address1.setZip("z1");
		Address address2 = new Address();
		address2.setStreet("s2");
		address2.setCity("c2");
		address2.setState("st2");
		address2.setZip("z2");
		List<Address> addresses = new ArrayList<Address>(2);
		addresses.add(address1);
		addresses.add(address2);
		bean.addresses = addresses;
		String value = binder.getBinding("addresses").getValue();
		assertEquals("s1:c1:st1:z1,s2:c2:st2:z2,", value);
	}

	@Test
	@Ignore
	public void getListAsSingleStringNoFormatter() {
		binder.addBinding("addresses");
		Address address1 = new Address();
		address1.setStreet("s1");
		address1.setCity("c1");
		address1.setState("st1");
		address1.setZip("z1");
		Address address2 = new Address();
		address2.setStreet("s2");
		address2.setCity("c2");
		address2.setState("st2");
		address2.setZip("z2");
		List<Address> addresses = new ArrayList<Address>(2);
		addresses.add(address1);
		addresses.add(address2);
		bean.addresses = addresses;
		String value = binder.getBinding("addresses").getValue();
		assertEquals("s1:c1:st1:z1,s2:c2:st2:z2,", value);
	}

	@Test
	public void bindToListHandleNullValueInNestedPath() {
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
	public void bindToMap() {
		binder.addBinding("favoriteFoodsByGroup");
		Map<String, String[]> values = new LinkedHashMap<String, String[]>();
		values.put("favoriteFoodsByGroup", new String[] { "DAIRY=Milk", "FRUIT=Peaches", "MEAT=Ham" });		
		BindingResults results = binder.bind(values);
		Assert.assertEquals(3, bean.favoriteFoodsByGroup.size());
		assertEquals("Milk", bean.favoriteFoodsByGroup.get(FoodGroup.DAIRY));
		assertEquals("Peaches", bean.favoriteFoodsByGroup.get(FoodGroup.FRUIT));
		assertEquals("Ham", bean.favoriteFoodsByGroup.get(FoodGroup.MEAT));
	}

	@Test
	public void bindToMapElements() {
		binder.addBinding("favoriteFoodsByGroup");
		Map<String, String> values = new LinkedHashMap<String, String>();
		values.put("favoriteFoodsByGroup['DAIRY']", "Milk");
		values.put("favoriteFoodsByGroup['FRUIT']", "Peaches");
		values.put("favoriteFoodsByGroup['MEAT']", "Ham");
		BindingResults results = binder.bind(values);
		Assert.assertEquals(3, bean.favoriteFoodsByGroup.size());
		assertEquals("Milk", bean.favoriteFoodsByGroup.get(FoodGroup.DAIRY));
		assertEquals("Peaches", bean.favoriteFoodsByGroup.get(FoodGroup.FRUIT));
		assertEquals("Ham", bean.favoriteFoodsByGroup.get(FoodGroup.MEAT));
	}
	
	@Test
	public void bindToMapSingleString() {
		binder.addBinding("favoriteFoodsByGroup");
		Map<String, String> values = new LinkedHashMap<String, String>();
		values.put("favoriteFoodsByGroup", "DAIRY=Milk FRUIT=Peaches MEAT=Ham");
		BindingResults results = binder.bind(values);
		Assert.assertEquals(3, bean.favoriteFoodsByGroup.size());
		assertEquals("Milk", bean.favoriteFoodsByGroup.get(FoodGroup.DAIRY));
		assertEquals("Peaches", bean.favoriteFoodsByGroup.get(FoodGroup.FRUIT));
		assertEquals("Ham", bean.favoriteFoodsByGroup.get(FoodGroup.MEAT));
	}
	
	@Test
	@Ignore
	public void getMapAsSingleString() {
		binder.addBinding("favoriteFoodsByGroup");
		Map<FoodGroup, String> foods = new LinkedHashMap<FoodGroup, String>();
		foods.put(FoodGroup.DAIRY, "Milk");
		foods.put(FoodGroup.FRUIT, "Peaches");
		foods.put(FoodGroup.MEAT, "Ham");
		bean.favoriteFoodsByGroup = foods;
		String value = binder.getBinding("favoriteFoodsByGroup").getValue();
		assertEquals("DAIRY=Milk FRUIT=Peaches MEAT=Ham", value);
	}

	@Test
	public void bindToNullObjectPath() {
		binder.addBinding("primaryAddress.street");
		binder.addBinding("primaryAddress.city");
		binder.addBinding("primaryAddress.state");
		binder.addBinding("primaryAddress.zip");
		Map<String, String> values = new LinkedHashMap<String, String>();
		values.put("primaryAddress.city", "Melbourne");
		binder.bind(values);
		Assert.assertEquals("Melbourne", bean.primaryAddress.city);
	}
	
	@Test
	public void formatPossibleValue() {
		binder.addBinding("currency").formatWith(new CurrencyFormatter());
		Binding b = binder.getBinding("currency");
		assertEquals("$5.00", b.format(new BigDecimal("5")));
	}
	*/
	
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
		private Map<FoodGroup, String> favoriteFoodsByGroup;
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

		public Map<FoodGroup, String> getFavoriteFoodsByGroup() {
			return favoriteFoodsByGroup;
		}

		public void setFavoriteFoodsByGroup(Map<FoodGroup, String> favoriteFoodsByGroup) {
			this.favoriteFoodsByGroup = favoriteFoodsByGroup;
		}

		public Address getPrimaryAddress() {
			return primaryAddress;
		}

		public void setPrimaryAddress(Address primaryAddress) {
			this.primaryAddress = primaryAddress;
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

	}

	public static class CurrencyAnnotationFormatterFactory implements
			AnnotationFormatterFactory<CurrencyFormat, BigDecimal> {
		public Formatter<BigDecimal> getFormatter(CurrencyFormat annotation) {
			return new CurrencyFormatter();
		}
	}

}
