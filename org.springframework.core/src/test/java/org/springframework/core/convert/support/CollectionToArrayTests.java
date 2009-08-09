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

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.core.convert.TypeDescriptor;

/**
 * @author Keith Donald
 */
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
