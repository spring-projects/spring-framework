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
		CompositeIterator<String> it = new CompositeIterator<String>();
		assertFalse(it.hasNext());
		try {
			it.next();
			fail();
		} catch (NoSuchElementException e) {
			// expected
		}
	}

	public void testSingleIterator() {
		CompositeIterator<String> it = new CompositeIterator<String>();
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
		CompositeIterator<String> it = new CompositeIterator<String>();
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
		List<String> list = Arrays.asList(new String[] { "0", "1" });
		CompositeIterator<String> it = new CompositeIterator<String>();
		it.add(list.iterator());
		it.hasNext();
		try {
			it.add(list.iterator());
			fail();
		} catch (IllegalStateException e) {
			// expected
		}
		it = new CompositeIterator<String>();
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
		List<String> list = Arrays.asList(new String[] { "0", "1" });
		Iterator<String> iterator = list.iterator();
		CompositeIterator<String> it = new CompositeIterator<String>();
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
