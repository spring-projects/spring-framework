package org.springframework.core.convert.support;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class EntityConverterTests {
	
	private GenericConversionService conversionService = new GenericConversionService();
	
	@Before
	public void setUp() {
		conversionService.addConverter(new ObjectToStringConverter());
		conversionService.addGenericConverter(new EntityConverter(conversionService));
	}
	
	@Test
	public void testToEntityReference() {
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		TestEntity e = conversionService.convert("1", TestEntity.class);
		assertEquals(new Long(1), e.getId());
	}
	
	@Test
	public void testToEntityId() {
		String id = conversionService.convert(new TestEntity(1L), String.class);
		assertEquals("1", id);		
	}
	
	public static class TestEntity {

		private Long id;
		
		public TestEntity(Long id) {
			this.id = id;
		}
		
		public Long getId() {
			return id;
		}
		
		public static TestEntity findTestEntity(Long id) {
			return new TestEntity(id);
		}
	}
}
