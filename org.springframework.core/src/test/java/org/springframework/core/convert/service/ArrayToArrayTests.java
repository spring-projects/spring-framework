package org.springframework.core.convert.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.core.convert.TypeDescriptor;

public class ArrayToArrayTests {
	
	@Test
	public void testArrayToArrayConversion() {
		DefaultConversionService service = new DefaultConversionService();
		ArrayToArray c = new ArrayToArray(TypeDescriptor.valueOf(String[].class), TypeDescriptor.valueOf(Integer[].class), service);
		Integer[] result = (Integer[]) c.execute(new String[] { "1", "2", "3" });
		assertEquals(new Integer(1), result[0]);
		assertEquals(new Integer(2), result[1]);
		assertEquals(new Integer(3), result[2]);
	}
}
