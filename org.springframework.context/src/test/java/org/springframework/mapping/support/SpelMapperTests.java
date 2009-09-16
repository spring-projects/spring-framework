package org.springframework.mapping.support;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.mapping.MappingException;
import org.springframework.mapping.support.SpelMapper;

public class SpelMapperTests {

	private SpelMapper mapper = new SpelMapper();
	
	@Test
	public void mapAutomatic() throws MappingException {
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
		mapper.addMapping("name", "name");
		
		Map<String, Object> source = new HashMap<String, Object>();
		source.put("name", "Keith");
		source.put("age", 31);
		
		Person target = new Person();
		
		mapper.map(source, target);
		
		assertEquals("Keith", target.name);
		assertEquals(0, target.age);
	}
	
	@Test
	public void mapAutomaticWithExplictOverrides() throws MappingException {
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
	public void mapSameSourceFieldToMultipleTargets() throws MappingException {
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


	
	public static class Person {
		
		private String name;
		
		private int age;
		
		private Sport favoriteSport;
		
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
		
	}

	public enum Sport {
		FOOTBALL, BASKETBALL
	}
}
