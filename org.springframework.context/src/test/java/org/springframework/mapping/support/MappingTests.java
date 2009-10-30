package org.springframework.mapping.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.mapping.Mapper;
import org.springframework.mapping.MappingException;

public class MappingTests {

	@Test
	public void testDefaultMapper() {
		EmployeeDto dto = new EmployeeDto();
		dto.setFirstName("Keith");
		dto.setLastName("Donald");
		Employee emp = (Employee) MapperFactory.defaultMapper().map(dto, new Employee());
		assertEquals("Keith", emp.getFirstName());
		assertEquals("Donald", emp.getLastName());
	}

	@Test
	public void mapAutomatic() {
		Map<String, Object> source = new HashMap<String, Object>();
		source.put("name", "Keith");
		source.put("age", 31);

		Person target = new Person();

		MapperFactory.defaultMapper().map(source, target);

		assertEquals("Keith", target.name);
		assertEquals(31, target.age);
	}

	@Test
	public void mapExplicit() throws MappingException {
		Map<String, Object> source = new HashMap<String, Object>();
		source.put("name", "Keith");
		source.put("age", 31);

		Person target = new Person();

		Mapper<Object, Object> mapper = MapperFactory.mapperBuilder().setAutoMappingEnabled(false).addMapping("name")
				.getMapper();
		mapper.map(source, target);

		assertEquals("Keith", target.name);
		assertEquals(0, target.age);
	}

	@Test
	public void mapAutomaticWithExplictOverrides() {
		Map<String, Object> source = new HashMap<String, Object>();
		source.put("name", "Keith");
		source.put("test", "3");
		source.put("favoriteSport", "FOOTBALL");

		Person target = new Person();

		Mapper<Object, Object> mapper = MapperFactory.mapperBuilder().addMapping("test", "age").getMapper();
		mapper.map(source, target);

		assertEquals("Keith", target.name);
		assertEquals(3, target.age);
		assertEquals(Sport.FOOTBALL, target.favoriteSport);
	}

	@Test
	public void mapAutomaticIgnoreUnknownField() {
		Map<String, Object> source = new HashMap<String, Object>();
		source.put("name", "Keith");
		source.put("age", 31);
		source.put("unknown", "foo");

		Person target = new Person();

		MapperFactory.defaultMapper().map(source, target);

		assertEquals("Keith", target.name);
		assertEquals(31, target.age);
	}

	@Test
	public void mapSameSourceFieldToMultipleTargets() {
		Map<String, Object> source = new HashMap<String, Object>();
		source.put("test", "FOOTBALL");

		Person target = new Person();

		Mapper<Object, Object> mapper = MapperFactory.mapperBuilder().addMapping("test", "name").addMapping("test",
				"favoriteSport").getMapper();
		mapper.map(source, target);

		assertEquals("FOOTBALL", target.name);
		assertEquals(0, target.age);
		assertEquals(Sport.FOOTBALL, target.favoriteSport);
	}

	@Test
	public void mapBean() {
		PersonDto source = new PersonDto();
		source.setFullName("Keith Donald");
		source.setAge("31");
		source.setSport("FOOTBALL");

		Person target = new Person();

		Mapper<Object, Object> mapper = MapperFactory.mapperBuilder().addMapping("fullName", "name").addMapping(
				"sport", "favoriteSport").getMapper();
		mapper.map(source, target);

		assertEquals("Keith Donald", target.name);
		assertEquals(31, target.age);
		assertEquals(Sport.FOOTBALL, target.favoriteSport);
	}

	@Test
	public void mapBeanDeep() {
		PersonDto source = new PersonDto();
		source.age = "0";
		NestedDto nested = new NestedDto();
		nested.foo = "bar";
		source.setNested(nested);

		Person target = new Person();

		Mapper<PersonDto, Person> mapper = MapperFactory.mapperBuilder(PersonDto.class, Person.class).addMapping(
				"nested.foo").getMapper();
		mapper.map(source, target);

		assertEquals("bar", target.nested.foo);
	}

	@Test
	public void mapBeanNested() {
		PersonDto source = new PersonDto();
		NestedDto nested = new NestedDto();
		nested.foo = "bar";
		source.setNested(nested);

		Person target = new Person();

		Mapper<PersonDto, Person> mapper = MapperFactory.mapperBuilder(PersonDto.class, Person.class)
				.setAutoMappingEnabled(false).addMapping("nested").getMapper();
		mapper.map(source, target);

		assertEquals("bar", target.nested.foo);
	}

	@Test
	public void mapBeanNestedCustomNestedMapper() {
		PersonDto source = new PersonDto();
		NestedDto nested = new NestedDto();
		nested.foo = "bar";
		source.setNested(nested);

		Person target = new Person();

		Mapper<NestedDto, Nested> nestedMapper = MapperFactory.mapperBuilder(NestedDto.class, Nested.class).addMapping(
				"foo", new Converter<String, String>() {
					public String convert(String source) {
						return source + " and baz";
					}
				}).getMapper();

		Mapper<PersonDto, Person> mapper = MapperFactory.mapperBuilder(PersonDto.class, Person.class)
				.setAutoMappingEnabled(false).addMapping("nested").addNestedMapper(nestedMapper).getMapper();

		mapper.map(source, target);

		assertEquals("bar and baz", target.nested.foo);
	}

	@Test
	public void mapBeanNestedCustomNestedMapperHandCoded() {
		PersonDto source = new PersonDto();
		NestedDto nested = new NestedDto();
		nested.foo = "bar";
		source.setNested(nested);

		Person target = new Person();

		Mapper<NestedDto, Nested> nestedMapper = new Mapper<NestedDto, Nested>() {
			public Nested map(NestedDto source, Nested target) {
				target.setFoo(source.getFoo() + " and baz");
				return target;
			}
		};

		Mapper<PersonDto, Person> mapper = MapperFactory.mapperBuilder(PersonDto.class, Person.class)
				.setAutoMappingEnabled(false).addMapping("nested").addNestedMapper(nestedMapper).getMapper();

		mapper.map(source, target);

		assertEquals("bar and baz", target.nested.foo);
	}

	@Test
	public void mapBeanNestedCustomNestedMapperConverterAsTargetFactory() {
		PersonDto source = new PersonDto();
		final NestedDto nested = new NestedDto();
		nested.foo = "bar";
		source.setNested(nested);

		Person target = new Person();

		Mapper<NestedDto, Nested> nestedMapper = MapperFactory.mapperBuilder(NestedDto.class, Nested.class).addMapping(
				"foo", new Converter<String, String>() {
					public String convert(String source) {
						return source + " and baz";
					}
				}).getMapper();

		Mapper<PersonDto, Person> mapper = MapperFactory.mapperBuilder(PersonDto.class, Person.class)
				.setAutoMappingEnabled(false).addMapping("nested").addNestedMapper(nestedMapper,
						new Converter<NestedDto, Nested>() {
							public Nested convert(NestedDto source) {
								assertEquals(nested, source);
								return new Nested();
							}
						}).getMapper();

		mapper.map(source, target);

		assertEquals("bar and baz", target.nested.foo);
	}

	@Test
	public void mapBeanNestedCustomConverterDelegatingToMapper() {
		PersonDto source = new PersonDto();
		final NestedDto nested = new NestedDto();
		nested.foo = "bar";
		source.setNested(nested);

		Person target = new Person();

		Mapper<PersonDto, Person> mapper = MapperFactory.mapperBuilder(PersonDto.class, Person.class)
				.setAutoMappingEnabled(false).addMapping("nested", new Converter<NestedDto, Nested>() {
					public Nested convert(NestedDto source) {
						Mapper<NestedDto, Nested> nestedMapper = MapperFactory.mapperBuilder(NestedDto.class,
								Nested.class).addMapping("foo", new Converter<String, String>() {
							public String convert(String source) {
								return source + " and baz";
							}
						}).getMapper();
						return nestedMapper.map(source, new Nested());
					}
				}).getMapper();

		mapper.map(source, target);

		assertEquals("bar and baz", target.nested.foo);
	}

	@Test
	public void testCustomMapper() {
		Mapper<CreateAccountDto, Account> mapper = MapperFactory.mapperBuilder(CreateAccountDto.class, Account.class)
				.setAutoMappingEnabled(false)
				// field to field of different name
				.addMapping("accountNumber", "number")
				// field to multiple fields
				.addMapping("name", new Mapper<String, Account>() {
					public Account map(String name, Account account) {
						String[] names = name.split(" ");
						account.setFirstName(names[0]);
						account.setLastName(names[1]);
						return account;
					}
				})
				// field to field with type conversion
				.addMapping("address", new Converter<String, Address>() {
					public Address convert(String address) {
						String[] fields = address.split(" ");
						Address addr = new Address();
						addr.setStreet(fields[0]);
						addr.setCity(fields[1]);
						addr.setState(fields[2]);
						addr.setZip(fields[3]);
						return addr;
					}
				})
				// multiple fields to field
				.addMapping(new String[] { "activationDay", "activationTime " },
						new Mapper<CreateAccountDto, Account>() {
							public Account map(CreateAccountDto source, Account target) {
								DateTime dateTime = ISODateTimeFormat.dateTime().parseDateTime(
										source.getActivationDay() + "T" + source.getActivationTime());
								target.setActivationDateTime(dateTime);
								return target;
							}
						}).getMapper();
		CreateAccountDto dto = new CreateAccountDto();
		dto.setAccountNumber("123456789");
		dto.setName("Keith Donald");
		dto.setActivationDay("2009-10-12");
		dto.setActivationTime("12:00:00.000Z");
		dto.setAddress("2009BelAireEstates PalmBay FL 35452");
		Account account = mapper.map(dto, new Account());
		assertEquals("Keith", account.getFirstName());
		assertEquals("Donald", account.getLastName());
		assertEquals("2009BelAireEstates", account.getAddress().getStreet());
		assertEquals("PalmBay", account.getAddress().getCity());
		assertEquals("FL", account.getAddress().getState());
		assertEquals("35452", account.getAddress().getZip());
		assertEquals(ISODateTimeFormat.dateTime().parseDateTime("2009-10-12T12:00:00.000Z"), account
				.getActivationDateTime());
	}

	@Test
	public void testMultiFieldToFieldMappingWithAssembler() {
		Mapper<Map, Account> mapper = MapperFactory.mapperBuilder(Map.class, Account.class)
				.setAutoMappingEnabled(false)
				// field to multiple fields
				.addAssemblerMapping("activationDateTime", new Converter<Map<String, String>, DateTime>() {
					public DateTime convert(Map<String, String> source) {
						MutableDateTime dateTime = new MutableDateTime(DateTimeZone.UTC);
						dateTime.setYear(Integer.parseInt(source.get("year")));
						dateTime.setMonthOfYear(Integer.parseInt(source.get("month")));
						dateTime.setDayOfMonth(Integer.parseInt(source.get("day")));
						dateTime.setHourOfDay(Integer.parseInt(source.get("hour")));
						dateTime.setMinuteOfHour(Integer.parseInt(source.get("minute")));
						dateTime.setSecondOfMinute(0);
						dateTime.setMillisOfSecond(0);
						return dateTime.toDateTime();
					}
				}).getMapper();
		Map<String, Object> source = new HashMap<String, Object>();
		source.put("activationDateTime.year", "2009");
		source.put("activationDateTime.month", "10");
		source.put("activationDateTime.day", "12");
		source.put("activationDateTime.hour", "12");
		source.put("activationDateTime.minute", "0");
		Account account = mapper.map(source, new Account());	
		assertEquals(ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC).parseDateTime("2009-10-12T12:00:00.000-04:00"), account
				.getActivationDateTime());		
	}

	@Test
	public void conditionalMapping() {
		Map<String, String> domestic = new HashMap<String, String>();
		domestic.put("international", "false");
		domestic.put("areaCode", "205");
		domestic.put("prefix", "339");
		domestic.put("line", "1234");
		domestic.put("countryCode", "whatever");
		domestic.put("cityCode", "whatever");

		Mapper<Map, PhoneNumber> mapper = MapperFactory.mapperBuilder(Map.class, PhoneNumber.class)
				.addConditionalMapping("countryCode", "international == 'true'").addConditionalMapping("cityCode",
						"international == 'true'").getMapper();

		PhoneNumber number = mapper.map(domestic, new PhoneNumber());
		assertEquals("205", number.getAreaCode());
		assertEquals("339", number.getPrefix());
		assertEquals("1234", number.getLine());
		assertNull(number.getCountryCode());
		assertNull(number.getCityCode());

		Map<String, String> international = new HashMap<String, String>();
		international.put("international", "true");
		international.put("areaCode", "205");
		international.put("prefix", "339");
		international.put("line", "1234");
		international.put("countryCode", "1");
		international.put("cityCode", "2");

		PhoneNumber number2 = mapper.map(international, new PhoneNumber());

		assertEquals("205", number2.getAreaCode());
		assertEquals("339", number2.getPrefix());
		assertEquals("1234", number2.getLine());
		assertEquals("1", number2.getCountryCode());
		assertEquals("2", number2.getCityCode());
	}

	@Test
	public void mapList() {
		PersonDto source = new PersonDto();
		List<String> sports = new ArrayList<String>();
		sports.add("FOOTBALL");
		sports.add("BASKETBALL");
		source.setSports(sports);

		Person target = new Person();

		Mapper<PersonDto, Person> mapper = MapperFactory.mapperBuilder(PersonDto.class, Person.class)
				.setAutoMappingEnabled(false).addMapping("sports", "favoriteSports").getMapper();
		mapper.map(source, target);

		assertEquals(Sport.FOOTBALL, target.favoriteSports.get(0));
		assertEquals(Sport.BASKETBALL, target.favoriteSports.get(1));
	}

	@Test
	public void mapListFlatten() {
		PersonDto source = new PersonDto();
		List<String> sports = new ArrayList<String>();
		sports.add("FOOTBALL");
		sports.add("BASKETBALL");
		source.setSports(sports);

		Person target = new Person();

		Mapper<PersonDto, Person> mapper = MapperFactory.mapperBuilder(PersonDto.class, Person.class)
				.setAutoMappingEnabled(false).addMapping("sports[0]", "favoriteSport").getMapper();
		mapper.map(source, target);

		assertEquals(Sport.FOOTBALL, target.favoriteSport);
		assertNull(target.favoriteSports);
	}

	@Test
	public void mapMap() {
		PersonDto source = new PersonDto();
		Map<String, String> friendRankings = new HashMap<String, String>();
		friendRankings.put("Keri", "1");
		friendRankings.put("Alf", "2");
		source.setFriendRankings(friendRankings);

		Person target = new Person();

		Mapper<PersonDto, Person> mapper = MapperFactory.mapperBuilder(PersonDto.class, Person.class)
				.setAutoMappingEnabled(false).addMapping("friendRankings").addConverter(
						new Converter<String, Person>() {
							public Person convert(String source) {
								return new Person(source);
							}
						}).getMapper();
		mapper.map(source, target);

		mapper.map(source, target);

		assertEquals(new Integer(1), target.friendRankings.get(new Person("Keri")));
		assertEquals(new Integer(2), target.friendRankings.get(new Person("Alf")));
	}

	@Test
	public void mapFieldConverter() {
		Map<String, Object> source = new HashMap<String, Object>();
		source.put("name", "Keith Donald");
		source.put("age", 31);

		Person target = new Person();

		Mapper<Object, Object> mapper = MapperFactory.mapperBuilder().addMapping("name",
				new Converter<String, String>() {
					public String convert(String source) {
						String[] names = source.split(" ");
						return names[0] + " P. " + names[1];
					}
				}).getMapper();
		mapper.map(source, target);

		assertEquals("Keith P. Donald", target.name);
		assertEquals(31, target.age);
	}

	@Test
	public void mapFailure() {
		Map<String, Object> source = new HashMap<String, Object>();
		source.put("name", "Keith");
		source.put("age", "invalid");
		Person target = new Person();
		try {
			MapperFactory.defaultMapper().map(source, target);
		} catch (MappingException e) {
			assertEquals(1, e.getMappingFailureCount());
		}
	}

	@Test
	public void mapCyclic() {
		Person source = new Person();
		source.setName("Keith");
		source.setAge(3);
		source.setFavoriteSport(Sport.FOOTBALL);
		source.cyclic = source;
		Person target = new Person();
		MapperFactory.defaultMapper().map(source, target);
		assertEquals("Keith", target.getName());
		assertEquals(3, target.getAge());
		assertEquals(Sport.FOOTBALL, target.getFavoriteSport());
		assertEquals(source.cyclic, target.cyclic);
	}

	@Test
	public void mapCyclicTypicalHibernateDomainModel() {
		Order source = new Order();
		source.setNumber(1);
		LineItem item = new LineItem();
		item.setAmount(new BigDecimal("30.00"));
		item.setOrder(source);
		source.setLineItem(item);

		Order target = new Order();

		MapperFactory.defaultMapper().map(source, target);
		assertEquals(1, target.getNumber());
		assertTrue(item == target.getLineItem());
		assertEquals(new BigDecimal("30.00"), target.getLineItem().getAmount());
		assertEquals(source, target.getLineItem().getOrder());
	}

	public static class EmployeeDto {

		private String firstName;

		private String lastName;

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

	}

	public static class Employee {

		private String firstName;

		private String lastName;

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

	}

	public static class CreateAccountDto {

		private String accountNumber;

		private String name;

		private String address;

		private String activationDay;

		private String activationTime;

		public String getAccountNumber() {
			return accountNumber;
		}

		public void setAccountNumber(String accountNumber) {
			this.accountNumber = accountNumber;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public String getActivationDay() {
			return activationDay;
		}

		public void setActivationDay(String activationDay) {
			this.activationDay = activationDay;
		}

		public String getActivationTime() {
			return activationTime;
		}

		public void setActivationTime(String activationTime) {
			this.activationTime = activationTime;
		}

	}

	public static class Account {

		private String number;

		private String firstName;

		private String lastName;

		private Address address;

		private DateTime activationDateTime;

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}

		public DateTime getActivationDateTime() {
			return activationDateTime;
		}

		public void setActivationDateTime(DateTime activationDateTime) {
			this.activationDateTime = activationDateTime;
		}

	}

	public static class Address {

		private String street;

		private String city;

		private String state;

		private String zip;

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

	}

	public static class PersonDto {

		private String fullName;

		private String age;

		private String sport;

		private List<String> sports;

		private Map<String, String> friendRankings;

		private NestedDto nested;

		public String getFullName() {
			return fullName;
		}

		public void setFullName(String fullName) {
			this.fullName = fullName;
		}

		public String getAge() {
			return age;
		}

		public void setAge(String age) {
			this.age = age;
		}

		public String getSport() {
			return sport;
		}

		public void setSport(String sport) {
			this.sport = sport;
		}

		public List<String> getSports() {
			return sports;
		}

		public void setSports(List<String> sports) {
			this.sports = sports;
		}

		public Map<String, String> getFriendRankings() {
			return friendRankings;
		}

		public void setFriendRankings(Map<String, String> friendRankings) {
			this.friendRankings = friendRankings;
		}

		public NestedDto getNested() {
			return nested;
		}

		public void setNested(NestedDto nested) {
			this.nested = nested;
		}

	}

	public static class NestedDto {

		private String foo;

		public String getFoo() {
			return foo;
		}
	}

	public static class Person {

		private String name;

		private int age;

		private Sport favoriteSport;

		private Nested nested;

		private Person cyclic;

		private List<Sport> favoriteSports;

		private Map<Person, Integer> friendRankings;

		public Person() {

		}

		public Person(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		public Sport getFavoriteSport() {
			return favoriteSport;
		}

		public void setFavoriteSport(Sport favoriteSport) {
			this.favoriteSport = favoriteSport;
		}

		public Nested getNested() {
			return nested;
		}

		public void setNested(Nested nested) {
			this.nested = nested;
		}

		public Person getCyclic() {
			return cyclic;
		}

		public void setCyclic(Person cyclic) {
			this.cyclic = cyclic;
		}

		public List<Sport> getFavoriteSports() {
			return favoriteSports;
		}

		public void setFavoriteSports(List<Sport> favoriteSports) {
			this.favoriteSports = favoriteSports;
		}

		public Map<Person, Integer> getFriendRankings() {
			return friendRankings;
		}

		public void setFriendRankings(Map<Person, Integer> friendRankings) {
			this.friendRankings = friendRankings;
		}

		public int hashCode() {
			return name.hashCode();
		}

		public boolean equals(Object o) {
			if (!(o instanceof Person)) {
				return false;
			}
			Person p = (Person) o;
			return name.equals(p.name);
		}
	}

	public static class Nested {

		private String foo;

		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

	}

	public enum Sport {
		FOOTBALL, BASKETBALL
	}

	public static class Order {

		private int number;

		private LineItem lineItem;

		public int getNumber() {
			return number;
		}

		public void setNumber(int number) {
			this.number = number;
		}

		public LineItem getLineItem() {
			return lineItem;
		}

		public void setLineItem(LineItem lineItem) {
			this.lineItem = lineItem;
		}

	}

	public static class LineItem {

		private BigDecimal amount;

		private Order order;

		public BigDecimal getAmount() {
			return amount;
		}

		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}

		public Order getOrder() {
			return order;
		}

		public void setOrder(Order order) {
			this.order = order;
		}

	}

	public static class PhoneNumber {

		private String areaCode;

		private String prefix;

		private String line;

		private String countryCode;

		private String cityCode;

		public String getAreaCode() {
			return areaCode;
		}

		public void setAreaCode(String areaCode) {
			this.areaCode = areaCode;
		}

		public String getPrefix() {
			return prefix;
		}

		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}

		public String getLine() {
			return line;
		}

		public void setLine(String line) {
			this.line = line;
		}

		public String getCountryCode() {
			return countryCode;
		}

		public void setCountryCode(String countryCode) {
			this.countryCode = countryCode;
		}

		public String getCityCode() {
			return cityCode;
		}

		public void setCityCode(String cityCode) {
			this.cityCode = cityCode;
		}

	}

}
