package org.springframework.core.convert.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.springframework.core.convert.TypeDescriptor;

public class CollectionToCollectionTests {

	@Test
	public void testCollectionToCollectionConversion() throws Exception {
		DefaultConversionService service = new DefaultConversionService();
		CollectionToCollection c = new CollectionToCollection(new TypeDescriptor(getClass().getField("bindTarget")),
				new TypeDescriptor(getClass().getField("integerTarget")), service);
		bindTarget.add("1");
		bindTarget.add("2");
		bindTarget.add("3");
		List result = (List) c.execute(bindTarget);
		assertEquals(new Integer(1), result.get(0));
		assertEquals(new Integer(2), result.get(1));
		assertEquals(new Integer(3), result.get(2));
	}

	@Test
	public void testCollectionToCollectionConversionNoGenericInfo() throws Exception {
		DefaultConversionService service = new DefaultConversionService();
		CollectionToCollection c = new CollectionToCollection(TypeDescriptor.valueOf(Collection.class),
				TypeDescriptor.valueOf(List.class), service);
		bindTarget.add("1");
		bindTarget.add("2");
		bindTarget.add("3");
		List result = (List) c.execute(bindTarget);
		assertEquals("1", result.get(0));
		assertEquals("2", result.get(1));
		assertEquals("3", result.get(2));
	}
	
	@Test
	public void testCollectionToCollectionConversionNoGenericInfoSource() throws Exception {
		DefaultConversionService service = new DefaultConversionService();
		CollectionToCollection c = new CollectionToCollection(TypeDescriptor.valueOf(Collection.class),
				new TypeDescriptor(getClass().getField("integerTarget")), service);
		bindTarget.add("1");
		bindTarget.add("2");
		bindTarget.add("3");
		List result = (List) c.execute(bindTarget);
		assertEquals(new Integer(1), result.get(0));
		assertEquals(new Integer(2), result.get(1));
		assertEquals(new Integer(3), result.get(2));
	}
	
	@Test
	public void testCollectionToCollectionConversionNoGenericInfoSourceNullValues() throws Exception {
		DefaultConversionService service = new DefaultConversionService();
		CollectionToCollection c = new CollectionToCollection(TypeDescriptor.valueOf(Collection.class),
				new TypeDescriptor(getClass().getField("integerTarget")), service);
		bindTarget.add(null);
		bindTarget.add("1");
		bindTarget.add("2");
		bindTarget.add(null);
		bindTarget.add("3");
		List result = (List) c.execute(bindTarget);
		assertEquals(null, result.get(0));
		assertEquals(new Integer(1), result.get(1));
		assertEquals(new Integer(2), result.get(2));
		assertEquals(null, result.get(3));
		assertEquals(new Integer(3), result.get(4));
	}
	
	@Test
	public void testCollectionToCollectionConversionNoGenericInfoSourceEmpty() throws Exception {
		DefaultConversionService service = new DefaultConversionService();
		CollectionToCollection c = new CollectionToCollection(TypeDescriptor.valueOf(Collection.class),
				new TypeDescriptor(getClass().getField("integerTarget")), service);
		List result = (List) c.execute(bindTarget);
		assertTrue(result.isEmpty());
	}


	public Collection<String> bindTarget = new ArrayList<String>();
	public List<Integer> integerTarget = new ArrayList<Integer>();


}
