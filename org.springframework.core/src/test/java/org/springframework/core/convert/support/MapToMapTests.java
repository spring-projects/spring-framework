package org.springframework.core.convert.support;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.core.convert.BindingPoint;
import org.springframework.core.convert.support.DefaultTypeConverter;
import org.springframework.core.convert.support.MapToMap;

public class MapToMapTests {

	@Test
	public void testMapToMapConversion() throws Exception {
		DefaultTypeConverter converter = new DefaultTypeConverter();
		MapToMap c = new MapToMap(new BindingPoint<Map<String, String>>(getClass().getField("source")),
				new BindingPoint<Map<String, FooEnum>>(getClass().getField("bindTarget")), converter);
		source.put("1", "BAR");
		source.put("2", "BAZ");
		Map<String, FooEnum> result = (Map<String, FooEnum>) c.execute(source);
		assertEquals(FooEnum.BAR, result.get(1));
		assertEquals(FooEnum.BAZ, result.get(2));
	}

	@Test
	public void testMapToMapConversionNoGenericInfoOnSource() throws Exception {
		DefaultTypeConverter service = new DefaultTypeConverter();
		MapToMap c = new MapToMap(BindingPoint.valueOf(Map.class),
				new BindingPoint(getClass().getField("bindTarget")), service);
		source.put("1", "BAR");
		source.put("2", "BAZ");
		Map result = (Map) c.execute(source);
		assertEquals(FooEnum.BAR, result.get(1));
		assertEquals(FooEnum.BAZ, result.get(2));
	}
	
	@Test
	public void testMapToMapConversionNoGenericInfo() throws Exception {
		DefaultTypeConverter service = new DefaultTypeConverter();
		MapToMap c = new MapToMap(BindingPoint.valueOf(Map.class),
				BindingPoint.valueOf(Map.class), service);
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
