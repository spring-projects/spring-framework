/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Test case for {@link CompositeIterator}.
 *
 * @author Erwin Vervaet
 * @author Juergen Hoeller
 */
public class CompositeIteratorTests {

	@Test
	public void testNoIterators() {
		CompositeIterator<String> it = new CompositeIterator<String>();
		assertFalse(it.hasNext());
		try {
			it.next();
			fail();
		}
		catch (NoSuchElementException ex) {
			// expected
		}
	}

	@Test
	public void testSingleIterator() {
		CompositeIterator<String> it = new CompositeIterator<String>();
		it.add(Arrays.asList("0", "1").iterator());
		for (int i = 0; i < 2; i++) {
			assertTrue(it.hasNext());
			assertEquals(String.valueOf(i), it.next());
		}
		assertFalse(it.hasNext());
		try {
			it.next();
			fail();
		}
		catch (NoSuchElementException ex) {
			// expected
		}
	}

	@Test
	public void testMultipleIterators() {
		CompositeIterator<String> it = new CompositeIterator<String>();
		it.add(Arrays.asList("0", "1").iterator());
		it.add(Arrays.asList("2").iterator());
		it.add(Arrays.asList("3", "4").iterator());
		for (int i = 0; i < 5; i++) {
			assertTrue(it.hasNext());
			assertEquals(String.valueOf(i), it.next());
		}
		assertFalse(it.hasNext());
		try {
			it.next();
			fail();
		}
		catch (NoSuchElementException ex) {
			// expected
		}
	}

	@Test
	public void testInUse() {
		List<String> list = Arrays.asList("0", "1");
		CompositeIterator<String> it = new CompositeIterator<String>();
		it.add(list.iterator());
		it.hasNext();
		try {
			it.add(list.iterator());
			fail();
		}
		catch (IllegalStateException ex) {
			// expected
		}
		it = new CompositeIterator<String>();
		it.add(list.iterator());
		it.next();
		try {
			it.add(list.iterator());
			fail();
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	public void testDuplicateIterators() {
		List<String> list = Arrays.asList("0", "1");
		Iterator<String> iterator = list.iterator();
		CompositeIterator<String> it = new CompositeIterator<String>();
		it.add(iterator);
		it.add(list.iterator());
		try {
			it.add(iterator);
			fail();
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

}
