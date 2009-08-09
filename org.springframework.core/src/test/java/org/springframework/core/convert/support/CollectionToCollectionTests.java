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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.core.convert.TypeDescriptor;

/**
 * @author Keith Donald
 */
public class CollectionToCollectionTests {

	@Test
	public void testCollectionToCollectionConversion() throws Exception {
		DefaultConversionService service = new DefaultConversionService();
		CollectionToCollection c = new CollectionToCollection(new TypeDescriptor(getClass().getField("bindTarget")),
				new TypeDescriptor(getClass().getField("integerTarget")), service);
		bindTarget.add("1");
		bindTarget.add("2");
		bindTarget.add("3");
		Collection result = (Collection) c.execute(bindTarget);
		assertEquals(3, result.size());
		assertTrue(result.contains(1));
		assertTrue(result.contains(2));
		assertTrue(result.contains(3));
	}

	@Test
	public void testCollectionToCollectionConversionNoGenericInfo() throws Exception {
		DefaultConversionService service = new DefaultConversionService();
		CollectionToCollection c = new CollectionToCollection(TypeDescriptor.valueOf(Collection.class),
				TypeDescriptor.valueOf(List.class), service);
		bindTarget.add("1");
		bindTarget.add("2");
		bindTarget.add("3");
		Collection result = (Collection) c.execute(bindTarget);
		assertEquals(3, result.size());
		assertTrue(result.contains("1"));
		assertTrue(result.contains("2"));
		assertTrue(result.contains("3"));
	}

	@Test
	public void testCollectionToCollectionConversionNoGenericInfoSource() throws Exception {
		DefaultConversionService service = new DefaultConversionService();
		CollectionToCollection c = new CollectionToCollection(TypeDescriptor.valueOf(Collection.class),
				new TypeDescriptor(getClass().getField("integerTarget")), service);
		bindTarget.add("1");
		bindTarget.add("2");
		bindTarget.add("3");
		Collection result = (Collection) c.execute(bindTarget);
		assertEquals(3, result.size());
		assertTrue(result.contains(1));
		assertTrue(result.contains(2));
		assertTrue(result.contains(3));
	}

	@Test
	public void testCollectionToCollectionConversionNoGenericInfoSourceNullValue() throws Exception {
		DefaultConversionService service = new DefaultConversionService();
		CollectionToCollection c = new CollectionToCollection(TypeDescriptor.valueOf(Collection.class),
				new TypeDescriptor(getClass().getField("integerTarget")), service);
		bindTarget.add(null);
		bindTarget.add("1");
		bindTarget.add("2");
		bindTarget.add("3");
		Collection result = (Collection) c.execute(bindTarget);
		Iterator it = result.iterator();
		assertEquals(null, it.next());
		assertEquals(new Integer(1), it.next());
		assertEquals(new Integer(2), it.next());
		assertEquals(new Integer(3), it.next());
	}

	@Test
	public void testCollectionToCollectionConversionNoGenericInfoSourceEmpty() throws Exception {
		DefaultConversionService service = new DefaultConversionService();
		CollectionToCollection c = new CollectionToCollection(TypeDescriptor.valueOf(Collection.class),
				new TypeDescriptor(getClass().getField("integerTarget")), service);
		Collection result = (Collection) c.execute(bindTarget);
		assertTrue(result.isEmpty());
	}


	public Collection<String> bindTarget = new ArrayList<String>();
	public List<Integer> integerTarget = new ArrayList<Integer>();

}
