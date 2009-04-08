package org.springframework.core.convert.service;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.springframework.core.convert.TypeDescriptor;

public class CollectionToArrayTests {

	@Test
	public void testCollectionToArrayConversion() throws Exception {
		DefaultConversionService service = new DefaultConversionService();
		CollectionToArray c = new CollectionToArray(new TypeDescriptor(getClass().getField("bindTarget")),
				TypeDescriptor.valueOf(Integer[].class), service);
		bindTarget.add("1");
		bindTarget.add("2");
		bindTarget.add("3");
		Integer[] result = (Integer[]) c.execute(bindTarget);
		assertEquals(new Integer(1), result[0]);
		assertEquals(new Integer(2), result[1]);
		assertEquals(new Integer(3), result[2]);
	}

	@Test
	public void testCollectionToArrayConversionNoGenericInfo() throws Exception {
		DefaultConversionService service = new DefaultConversionService();
		CollectionToArray c = new CollectionToArray(TypeDescriptor.valueOf(Collection.class), TypeDescriptor
				.valueOf(Integer[].class), service);
		bindTarget.add("1");
		bindTarget.add("2");
		bindTarget.add("3");
		Integer[] result = (Integer[]) c.execute(bindTarget);
		assertEquals(new Integer(1), result[0]);
		assertEquals(new Integer(2), result[1]);
		assertEquals(new Integer(3), result[2]);
	}
	
	@Test
	public void testCollectionToArrayConversionNoGenericInfoNullElement() throws Exception {
		DefaultConversionService service = new DefaultConversionService();
		CollectionToArray c = new CollectionToArray(TypeDescriptor.valueOf(Collection.class), TypeDescriptor
				.valueOf(Integer[].class), service);
		bindTarget.add(null);
		bindTarget.add("1");
		bindTarget.add("2");
		bindTarget.add("3");
		Integer[] result = (Integer[]) c.execute(bindTarget);
		assertEquals(null, result[0]);
		assertEquals(new Integer(1), result[1]);
		assertEquals(new Integer(2), result[2]);
		assertEquals(new Integer(3), result[3]);
	}

	public Collection<String> bindTarget = new ArrayList<String>();


}
