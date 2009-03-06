/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.core.convert.service;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.core.convert.ConversionExecutor;
import org.springframework.core.convert.ConversionExecutorNotFoundException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.StringToBoolean;

/**
 * Test case for the default conversion service.
 * 
 * @author Keith Donald
 */
public class DefaultConversionServiceTests extends TestCase {

	ConversionService service = new DefaultConversionService();

	public void testConversionForwardIndex() {
		ConversionExecutor<String, Integer> executor = service.getConversionExecutor(String.class, Integer.class);
		Integer three = executor.execute("3");
		assertEquals(3, three.intValue());
	}

	public void testConversionReverseIndex() {
		ConversionExecutor<Integer, String> executor = service.getConversionExecutor(Integer.class, String.class);
		String threeString = executor.execute(new Integer(3));
		assertEquals("3", threeString);
	}

	public void testConversionCompatibleTypes() {
		ArrayList source = new ArrayList();
		assertSame(source, service.getConversionExecutor(ArrayList.class, List.class).execute(source));
	}

	public void testConversionOverrideDefaultConverter() {
		Converter<String, Boolean> customConverter = new StringToBoolean("ja", "nee");
		((GenericConversionService) service).addConverter(customConverter);
		ConversionExecutor<String, Boolean> executor = service.getConversionExecutor(String.class, Boolean.class);
		assertTrue(executor.execute("ja").booleanValue());
	}

	public void testConverterLookupTargetClassNotSupported() {
		try {
			service.getConversionExecutor(String.class, HashMap.class);
			fail("Should have thrown an exception");
		} catch (ConversionExecutorNotFoundException e) {
		}
	}

	public void testConversionToPrimitive() {
		DefaultConversionService service = new DefaultConversionService();
		ConversionExecutor executor = service.getConversionExecutor(String.class, int.class);
		Integer three = (Integer) executor.execute("3");
		assertEquals(3, three.intValue());
	}

	public void testConversionArrayToArray() {
		ConversionExecutor executor = service.getConversionExecutor(String[].class, Integer[].class);
		Integer[] result = (Integer[]) executor.execute(new String[] { "1", "2", "3" });
		assertEquals(new Integer(1), result[0]);
		assertEquals(new Integer(2), result[1]);
		assertEquals(new Integer(3), result[2]);
	}

	public void testConversionArrayToPrimitiveArray() {
		ConversionExecutor executor = service.getConversionExecutor(String[].class, int[].class);
		int[] result = (int[]) executor.execute(new String[] { "1", "2", "3" });
		assertEquals(1, result[0]);
		assertEquals(2, result[1]);
		assertEquals(3, result[2]);
	}

	public void testConversionArrayToList() {
		ConversionExecutor executor = service.getConversionExecutor(String[].class, List.class);
		List result = (List) executor.execute(new String[] { "1", "2", "3" });
		assertEquals("1", result.get(0));
		assertEquals("2", result.get(1));
		assertEquals("3", result.get(2));
	}

	public void testConversionToArray() {
		ConversionExecutor executor = service.getConversionExecutor(Collection.class, String[].class);
		List list = new ArrayList();
		list.add("1");
		list.add("2");
		list.add("3");
		String[] result = (String[]) executor.execute(list);
		assertEquals("1", result[0]);
		assertEquals("2", result[1]);
		assertEquals("3", result[2]);
	}

	public void testConversionListToArrayWithComponentConversion() {
		ConversionExecutor executor = service.getConversionExecutor(Collection.class, Integer[].class);
		List list = new ArrayList();
		list.add("1");
		list.add("2");
		list.add("3");
		Integer[] result = (Integer[]) executor.execute(list);
		assertEquals(new Integer(1), result[0]);
		assertEquals(new Integer(2), result[1]);
		assertEquals(new Integer(3), result[2]);
	}

	public void testConversionArrayToConcreteList() {
		ConversionExecutor executor = service.getConversionExecutor(String[].class, LinkedList.class);
		LinkedList result = (LinkedList) executor.execute(new String[] { "1", "2", "3" });
		assertEquals("1", result.get(0));
		assertEquals("2", result.get(1));
		assertEquals("3", result.get(2));
	}

	public void testConversionArrayToAbstractList() {
		try {
			service.getConversionExecutor(String[].class, AbstractList.class);
		} catch (IllegalArgumentException e) {

		}
	}

	public void testConversionStringToArray() {
		ConversionExecutor executor = service.getConversionExecutor(String.class, String[].class);
		String[] result = (String[]) executor.execute("1,2,3");
		assertEquals(1, result.length);
		assertEquals("1,2,3", result[0]);
	}

	public void testConversionStringToArrayWithElementConversion() {
		ConversionExecutor executor = service.getConversionExecutor(String.class, Integer[].class);
		Integer[] result = (Integer[]) executor.execute("123");
		assertEquals(1, result.length);
		assertEquals(new Integer(123), result[0]);
	}

}