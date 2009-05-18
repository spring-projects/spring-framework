package org.springframework.core.convert.support;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.junit.Test;
import org.springframework.core.convert.ConversionContext;
import org.springframework.core.convert.support.ArrayToCollection;
import org.springframework.core.convert.support.DefaultTypeConverter;

public class ArrayToCollectionTests {
	
	@Test
	public void testArrayToCollectionConversion() throws Exception {
		DefaultTypeConverter service = new DefaultTypeConverter();
		ArrayToCollection c = new ArrayToCollection(ConversionContext.valueOf(String[].class), new ConversionContext(getClass().getField("bindTarget")), service);
		List result = (List) c.execute(new String[] { "1", "2", "3" });
		assertEquals(new Integer(1), result.get(0));
		assertEquals(new Integer(2), result.get(1));
		assertEquals(new Integer(3), result.get(2));
	}
	
	@Test
	public void testArrayToSetConversion() throws Exception {
		DefaultTypeConverter service = new DefaultTypeConverter();
		ArrayToCollection c = new ArrayToCollection(ConversionContext.valueOf(String[].class), new ConversionContext(getClass().getField("setTarget")), service);
		Set result = (Set) c.execute(new String[] { "1" });
		assertEquals("1", result.iterator().next());
	}
	
	@Test
	public void testArrayToSortedSetConversion() throws Exception {
		DefaultTypeConverter service = new DefaultTypeConverter();
		ArrayToCollection c = new ArrayToCollection(ConversionContext.valueOf(String[].class), new ConversionContext(getClass().getField("sortedSetTarget")), service);
		SortedSet result = (SortedSet) c.execute(new String[] { "1" });
		assertEquals(new Integer(1), result.iterator().next());
	}
	
	@Test
	public void testArrayToCollectionImplConversion() throws Exception {
		DefaultTypeConverter service = new DefaultTypeConverter();
		ArrayToCollection c = new ArrayToCollection(ConversionContext.valueOf(String[].class), new ConversionContext(getClass().getField("implTarget")), service);
		LinkedList result = (LinkedList) c.execute(new String[] { "1" });
		assertEquals("1", result.iterator().next());
	}
	
	@Test
	public void testArrayToNonGenericCollectionConversionNullElement() throws Exception {
		DefaultTypeConverter service = new DefaultTypeConverter();
		ArrayToCollection c = new ArrayToCollection(ConversionContext.valueOf(String[].class), new ConversionContext(getClass().getField("listTarget")), service);
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
