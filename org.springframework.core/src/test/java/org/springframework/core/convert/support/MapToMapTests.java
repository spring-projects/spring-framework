package org.springframework.core.convert.support;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.MapToMap;

public class MapToMapTests {

	@Test
	public void testMapToMapConversion() throws Exception {
		DefaultConversionService service = new DefaultConversionService();
		MapToMap c = new MapToMap(new TypeDescriptor(getClass().getField("source")),
				new TypeDescriptor(getClass().getField("bindTarget")), service);
		source.put("1", "BAR");
		source.put("2", "BAZ");
		Map result = (Map) c.execute(source);
		assertEquals(FooEnum.BAR, result.get(1));
		assertEquals(FooEnum.BAZ, result.get(2));
	}

	@Test
	public void testMapToMapConversionNoGenericInfoOnSource() throws Exception {
		DefaultConversionService service = new DefaultConversionService();
		MapToMap c = new MapToMap(TypeDescriptor.valueOf(Map.class),
				new TypeDescriptor(getClass().getField("bindTarget")), service);
		source.put("1", "BAR");
		source.put("2", "BAZ");
		Map result = (Map) c.execute(source);
		assertEquals(FooEnum.BAR, result.get(1));
		assertEquals(FooEnum.BAZ, result.get(2));
	}
	
	@Test
	public void testMapToMapConversionNoGenericInfo() throws Exception {
		DefaultConversionService service = new DefaultConversionService();
		MapToMap c = new MapToMap(TypeDescriptor.valueOf(Map.class),
				TypeDescriptor.valueOf(Map.class), service);
		source.put("1", "BAR");
		source.put("2", "BAZ");
		Map result = (Map) c.execute(source);
		assertEquals("BAR", result.get("1"));
		assertEquals("BAZ", result.get("2"));
	}


	public Map<String, String> source = new HashMap<String, String>();
	public Map<Integer, FooEnum> bindTarget = new HashMap<Integer, FooEnum>();
	
	public static enum FooEnum {
		BAR, BAZ;
	}

}
