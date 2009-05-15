package org.springframework.core.convert.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.springframework.core.convert.ConversionPoint;
import org.springframework.core.convert.support.CollectionToCollection;
import org.springframework.core.convert.support.DefaultTypeConverter;

public class CollectionToCollectionTests {

	@Test
	public void testCollectionToCollectionConversion() throws Exception {
		DefaultTypeConverter service = new DefaultTypeConverter();
		CollectionToCollection c = new CollectionToCollection(new ConversionPoint(getClass().getField("bindTarget")),
				new ConversionPoint(getClass().getField("integerTarget")), service);
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
		DefaultTypeConverter service = new DefaultTypeConverter();
		CollectionToCollection c = new CollectionToCollection(ConversionPoint.valueOf(Collection.class),
				ConversionPoint.valueOf(List.class), service);
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
		DefaultTypeConverter service = new DefaultTypeConverter();
		CollectionToCollection c = new CollectionToCollection(ConversionPoint.valueOf(Collection.class),
				new ConversionPoint(getClass().getField("integerTarget")), service);
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
		DefaultTypeConverter service = new DefaultTypeConverter();
		CollectionToCollection c = new CollectionToCollection(ConversionPoint.valueOf(Collection.class),
				new ConversionPoint(getClass().getField("integerTarget")), service);
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
		DefaultTypeConverter service = new DefaultTypeConverter();
		CollectionToCollection c = new CollectionToCollection(ConversionPoint.valueOf(Collection.class),
				new ConversionPoint(getClass().getField("integerTarget")), service);
		List result = (List) c.execute(bindTarget);
		assertTrue(result.isEmpty());
	}


	public Collection<String> bindTarget = new ArrayList<String>();
	public List<Integer> integerTarget = new ArrayList<Integer>();


}
