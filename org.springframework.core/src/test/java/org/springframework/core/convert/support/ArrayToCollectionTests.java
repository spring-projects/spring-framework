/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.convert.support;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.core.convert.TypeDescriptor;

/**
 * @author Keith Donald
 */
public class ArrayToCollectionTests {
	
	@Test
	public void testArrayToCollectionConversion() throws Exception {
		DefaultConversionService service = new DefaultConversionService();
		ArrayToCollection c = new ArrayToCollection(TypeDescriptor.valueOf(String[].class), new TypeDescriptor(getClass().getField("bindTarget")), service);
		Collection result = (Collection) c.execute(new String[] { "1", "2", "3" });
		assertEquals(3, result.size());
		assertTrue(result.contains(1));
		assertTrue(result.contains(2));
		assertTrue(result.contains(3));
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
