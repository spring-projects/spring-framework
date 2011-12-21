package org.springframework.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import junit.framework.TestCase;

/**
 * Test case for {@link CompositeIterator}.
 * 
 * @author Erwin Vervaet
 */
public class CompositeIteratorTests extends TestCase {

	public void testNoIterators() {
		CompositeIterator it = new CompositeIterator();
		assertFalse(it.hasNext());
		try {
			it.next();
			fail();
		} catch (NoSuchElementException e) {
			// expected
		}
	}

	public void testSingleIterator() {
		CompositeIterator it = new CompositeIterator();
		it.add(Arrays.asList(new String[] { "0", "1" }).iterator());
		for (int i = 0; i < 2; i++) {
			assertTrue(it.hasNext());
			assertEquals(String.valueOf(i), it.next());
		}
		assertFalse(it.hasNext());
		try {
			it.next();
			fail();
		} catch (NoSuchElementException e) {
			// expected
		}
	}

	public void testMultipleIterators() {
		CompositeIterator it = new CompositeIterator();
		it.add(Arrays.asList(new String[] { "0", "1" }).iterator());
		it.add(Arrays.asList(new String[] { "2" }).iterator());
		it.add(Arrays.asList(new String[] { "3", "4" }).iterator());
		for (int i = 0; i < 5; i++) {
			assertTrue(it.hasNext());
			assertEquals(String.valueOf(i), it.next());
		}
		assertFalse(it.hasNext());
		try {
			it.next();
			fail();
		} catch (NoSuchElementException e) {
			// expected
		}
	}

	public void testInUse() {
		List list = Arrays.asList(new String[] { "0", "1" });
		CompositeIterator it = new CompositeIterator();
		it.add(list.iterator());
		it.hasNext();
		try {
			it.add(list.iterator());
			fail();
		} catch (IllegalStateException e) {
			// expected
		}
		it = new CompositeIterator();
		it.add(list.iterator());
		it.next();
		try {
			it.add(list.iterator());
			fail();
		} catch (IllegalStateException e) {
			// expected
		}
	}

	public void testDuplicateIterators() {
		List list = Arrays.asList(new String[] { "0", "1" });
		Iterator iterator = list.iterator();
		CompositeIterator it = new CompositeIterator();
		it.add(iterator);
		it.add(list.iterator());
		try {
			it.add(iterator);
			fail();
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

}
