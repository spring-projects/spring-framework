package org.springframework.core.convert.service;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.junit.Test;
import org.springframework.core.convert.TypeDescriptor;

public class ArrayToCollectionTests {
	
	@Test
	public void testArrayToCollectionConversion() throws Exception {
		DefaultConversionService service = new DefaultConversionService();
		ArrayToCollection c = new ArrayToCollection(TypeDescriptor.valueOf(String[].class), new TypeDescriptor(getClass().getField("bindTarget")), service);
		List result = (List) c.execute(new String[] { "1", "2", "3" });
		assertEquals(new Integer(1), result.get(0));
		assertEquals(new Integer(2), result.get(1));
		assertEquals(new Integer(3), result.get(2));
	}
	
	@Test
	public void testArrayToSetConversion() throws Exception {
		DefaultConversionService service = new DefaultConversionService();
		ArrayToCollection c = new ArrayToCollection(TypeDescriptor.valueOf(String[].class), new TypeDescriptor(getClass().getField("setTarget")), service);
		Set result = (Set) c.execute(new String[] { "1" });
		assertEquals("1", result.iterator().next());
	}
	
	@Test
	public void testArrayToSortedSetConversion() throws Exception {
		DefaultConversionService service = new DefaultConversionService();
		ArrayToCollection c = new ArrayToCollection(TypeDescriptor.valueOf(String[].class), new TypeDescriptor(getClass().getField("sortedSetTarget")), service);
		SortedSet result = (SortedSet) c.execute(new String[] { "1" });
		assertEquals(new Integer(1), result.iterator().next());
	}
	
	@Test
	public void testArrayToCollectionImplConversion() throws Exception {
		DefaultConversionService service = new DefaultConversionService();
		ArrayToCollection c = new ArrayToCollection(TypeDescriptor.valueOf(String[].class), new TypeDescriptor(getClass().getField("implTarget")), service);
		LinkedList result = (LinkedList) c.execute(new String[] { "1" });
		assertEquals("1", result.iterator().next());
	}
	
	@Test
	public void testArrayToNonGenericCollectionConversionNullElement() throws Exception {
		DefaultConversionService service = new DefaultConversionService();
		ArrayToCollection c = new ArrayToCollection(TypeDescriptor.valueOf(String[].class), new TypeDescriptor(getClass().getField("listTarget")), service);
		List result = (List) c.execute(new Integer[] { null, new Integer(1) });
		assertEquals(null, result.get(0));
		assertEquals(new Integer(1), result.get(1));
	}
	
	public Collection<Integer> bindTarget;
	public List listTarget;
	public Set setTarget;
	public SortedSet<Integer> sortedSetTarget;
	public LinkedList<String> implTarget;
	
}
