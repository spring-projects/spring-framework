package org.springframework.core.convert.support;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.core.convert.BindingPoint;
import org.springframework.core.convert.support.ArrayToArray;
import org.springframework.core.convert.support.DefaultTypeConverter;

public class ArrayToArrayTests {
	
	@Test
	public void testArrayToArrayConversion() {
		DefaultTypeConverter service = new DefaultTypeConverter();
		ArrayToArray c = new ArrayToArray(BindingPoint.valueOf(String[].class), BindingPoint.valueOf(Integer[].class), service);
		Integer[] result = (Integer[]) c.execute(new String[] { "1", "2", "3" });
		assertEquals(new Integer(1), result[0]);
		assertEquals(new Integer(2), result[1]);
		assertEquals(new Integer(3), result[2]);
	}
}
