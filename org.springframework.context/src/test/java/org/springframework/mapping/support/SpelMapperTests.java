package org.springframework.mapping.support;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.mapping.MappingException;

public class SpelMapperTests {

	private SpelMapper mapper = new SpelMapper();

	@Test
	public void mapAutomatic() {
		Map<String, Object> source = new HashMap<String, Object>();
		source.put("name", "Keith");
		source.put("age", 31);

		Person target = new Person();

		mapper.map(source, target);

		assertEquals("Keith", target.name);
		assertEquals(31, target.age);
	}

	@Test
	public void mapExplicit() throws MappingException {
		mapper.setAutoMappingEnabled(false);
		mapper.addMapping("name");

		Map<String, Object> source = new HashMap<String, Object>();
		source.put("name", "Keith");
		source.put("age", 31);

		Person target = new Person();

		mapper.map(source, target);

		assertEquals("Keith", target.name);
		assertEquals(0, target.age);
	}

	@Test
	public void mapAutomaticWithExplictOverrides() {
		mapper.addMapping("test", "age");

		Map<String, Object> source = new HashMap<String, Object>();
		source.put("name", "Keith");
		source.put("test", "3");
		source.put("favoriteSport", "FOOTBALL");

		Person target = new Person();

		mapper.map(source, target);

		assertEquals("Keith", target.name);
		assertEquals(3, target.age);
		assertEquals(Sport.FOOTBALL, target.favoriteSport);
	}

	@Test
	public void mapSameSourceFieldToMultipleTargets() {
		mapper.addMapping("test", "name");
		mapper.addMapping("test", "favoriteSport");

		Map<String, Object> source = new HashMap<String, Object>();
		source.put("test", "FOOTBALL");

		Person target = new Person();

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

		mapper.addMapping("fullName", "name");
		mapper.addMapping("sport", "favoriteSport");

		mapper.map(source, target);

		assertEquals("Keith Donald", target.name);
		assertEquals(31, target.age);
		assertEquals(Sport.FOOTBALL, target.favoriteSport);
	}

	@Test
	public void mapBeanNested() {
		PersonDto source = new PersonDto();
		source.setFullName("Keith Donald");
		source.setAge("31");
		source.setSport("FOOTBALL");

		Person target = new Person();

		mapper.addMapping("fullName", "nested.fullName");
		mapper.addMapping("age", "nested.age");
		mapper.addMapping("sport", "nested.sport");

		mapper.map(source, target);

		assertEquals("Keith Donald", target.getNested().getFullName());
		assertEquals("31", target.nested.age);
		assertEquals("FOOTBALL", target.nested.sport);
	}

	@Test
	public void mapList() {
		PersonDto source = new PersonDto();
		List<String> sports = new ArrayList<String>();
		sports.add("FOOTBALL");
		sports.add("BASKETBALL");
		source.setSports(sports);

		Person target = new Person();

		mapper.setAutoMappingEnabled(false);
		mapper.addMapping("sports", "favoriteSports");

		mapper.map(source, target);

		assertEquals(Sport.FOOTBALL, target.favoriteSports.get(0));
		assertEquals(Sport.BASKETBALL, target.favoriteSports.get(1));
	}

	@Test
	public void mapMap() {
		PersonDto source = new PersonDto();
		Map<String, String> friendRankings = new HashMap<String, String>();
		friendRankings.put("Keri", "1");
		friendRankings.put("Alf", "2");
		source.setFriendRankings(friendRankings);

		Person target = new Person();

		mapper.setAutoMappingEnabled(false);
		mapper.addMapping("friendRankings", "friendRankings");
		mapper.getConverterRegistry().addConverter(new Converter<String, Person>() {
			public Person convert(String source) {
				return new Person(source);
			}
		});
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

		mapper.addMapping("name").setConverter(new Converter<String, String>() {
			public String convert(String source) {
				String[] names = source.split(" ");
				return names[0] + " P. " + names[1];
			}
		});

		mapper.map(source, target);

		assertEquals("Keith P. Donald", target.name);
		assertEquals(31, target.age);
	}

	@Test
	public void mapFailure() {
		Map<String, Object> source = new HashMap<String, Object>();
		source.put("name", "Keith");
		source.put("age", "bogus");
		Person target = new Person();
		try {
			mapper.map(source, target);
		} catch (MappingException e) {
			assertEquals(1, e.getMappingFailureCount());
		}
	}

	public static class PersonDto {

		private String fullName;

		private String age;

		private String sport;

		private List<String> sports;

		private Map<String, String> friendRankings;

		private NestedDto nestedDto;

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

		public NestedDto getNestedDto() {
			return nestedDto;
		}

		public void setNestedDto(NestedDto nestedDto) {
			this.nestedDto = nestedDto;
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

		private PersonDto nested;

		// private Person cyclic;

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

		public PersonDto getNested() {
			return nested;
		}

		public void setNested(PersonDto nested) {
			this.nested = nested;
		}

		/*
		public Person getCyclic() {
			return cyclic;
		}

		public void setCyclic(Person cyclic) {
			this.cyclic = cyclic;
		}
		*/

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

	public enum Sport {
		FOOTBALL, BASKETBALL
	}
}
