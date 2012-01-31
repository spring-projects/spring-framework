/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.dao.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import junit.framework.TestCase;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.TypeMismatchDataAccessException;

/**
 * @author Juergen Hoeller
 * @since 20.10.2004
 */
public class DataAccessUtilsTests extends TestCase {

	public void testWithEmptyCollection() {
		Collection col = new HashSet();

		assertNull(DataAccessUtils.uniqueResult(col));

		try {
			DataAccessUtils.requiredUniqueResult(col);
			fail("Should have thrown IncorrectResultSizeDataAccessException");
		}
		catch (IncorrectResultSizeDataAccessException ex) {
			// expected
			assertEquals(1, ex.getExpectedSize());
			assertEquals(0, ex.getActualSize());
		}

		try {
			DataAccessUtils.objectResult(col, String.class);
			fail("Should have thrown IncorrectResultSizeDataAccessException");
		}
		catch (IncorrectResultSizeDataAccessException ex) {
			// expected
			assertEquals(1, ex.getExpectedSize());
			assertEquals(0, ex.getActualSize());
		}

		try {
			DataAccessUtils.intResult(col);
			fail("Should have thrown IncorrectResultSizeDataAccessException");
		}
		catch (IncorrectResultSizeDataAccessException ex) {
			// expected
			assertEquals(1, ex.getExpectedSize());
			assertEquals(0, ex.getActualSize());
		}

		try {
			DataAccessUtils.longResult(col);
			fail("Should have thrown IncorrectResultSizeDataAccessException");
		}
		catch (IncorrectResultSizeDataAccessException ex) {
			// expected
			assertEquals(1, ex.getExpectedSize());
			assertEquals(0, ex.getActualSize());
		}
	}

	public void testWithTooLargeCollection() {
		Collection col = new HashSet();
		col.add("test1");
		col.add("test2");

		try {
			DataAccessUtils.uniqueResult(col);
			fail("Should have thrown IncorrectResultSizeDataAccessException");
		}
		catch (IncorrectResultSizeDataAccessException ex) {
			// expected
			assertEquals(1, ex.getExpectedSize());
			assertEquals(2, ex.getActualSize());
		}

		try {
			DataAccessUtils.requiredUniqueResult(col);
			fail("Should have thrown IncorrectResultSizeDataAccessException");
		}
		catch (IncorrectResultSizeDataAccessException ex) {
			// expected
			assertEquals(1, ex.getExpectedSize());
			assertEquals(2, ex.getActualSize());
		}

		try {
			DataAccessUtils.objectResult(col, String.class);
			fail("Should have thrown IncorrectResultSizeDataAccessException");
		}
		catch (IncorrectResultSizeDataAccessException ex) {
			// expected
			assertEquals(1, ex.getExpectedSize());
			assertEquals(2, ex.getActualSize());
		}

		try {
			DataAccessUtils.intResult(col);
			fail("Should have thrown IncorrectResultSizeDataAccessException");
		}
		catch (IncorrectResultSizeDataAccessException ex) {
			// expected
			assertEquals(1, ex.getExpectedSize());
			assertEquals(2, ex.getActualSize());
		}

		try {
			DataAccessUtils.longResult(col);
			fail("Should have thrown IncorrectResultSizeDataAccessException");
		}
		catch (IncorrectResultSizeDataAccessException ex) {
			// expected
			assertEquals(1, ex.getExpectedSize());
			assertEquals(2, ex.getActualSize());
		}
	}

	public void testWithInteger() {
		Collection col = new HashSet();
		col.add(new Integer(5));

		assertEquals(new Integer(5), DataAccessUtils.uniqueResult(col));
		assertEquals(new Integer(5), DataAccessUtils.requiredUniqueResult(col));
		assertEquals(new Integer(5), DataAccessUtils.objectResult(col, Integer.class));
		assertEquals("5", DataAccessUtils.objectResult(col, String.class));
		assertEquals(5, DataAccessUtils.intResult(col));
		assertEquals(5, DataAccessUtils.longResult(col));
	}

	public void testWithSameIntegerInstanceTwice() {
		Integer i = new Integer(5);
		Collection col = new ArrayList();
		col.add(i);
		col.add(i);

		assertEquals(new Integer(5), DataAccessUtils.uniqueResult(col));
		assertEquals(new Integer(5), DataAccessUtils.requiredUniqueResult(col));
		assertEquals(new Integer(5), DataAccessUtils.objectResult(col, Integer.class));
		assertEquals("5", DataAccessUtils.objectResult(col, String.class));
		assertEquals(5, DataAccessUtils.intResult(col));
		assertEquals(5, DataAccessUtils.longResult(col));
	}

	public void testWithEquivalentIntegerInstanceTwice() {
		Collection col = new ArrayList();
		col.add(new Integer(5));
		col.add(new Integer(5));

		try {
			DataAccessUtils.uniqueResult(col);
			fail("Should have thrown IncorrectResultSizeDataAccessException");
		}
		catch (IncorrectResultSizeDataAccessException ex) {
			// expected
			assertEquals(1, ex.getExpectedSize());
			assertEquals(2, ex.getActualSize());
		}
	}

	public void testWithLong() {
		Collection col = new HashSet();
		col.add(new Long(5));

		assertEquals(new Long(5), DataAccessUtils.uniqueResult(col));
		assertEquals(new Long(5), DataAccessUtils.requiredUniqueResult(col));
		assertEquals(new Long(5), DataAccessUtils.objectResult(col, Long.class));
		assertEquals("5", DataAccessUtils.objectResult(col, String.class));
		assertEquals(5, DataAccessUtils.intResult(col));
		assertEquals(5, DataAccessUtils.longResult(col));
	}

	public void testWithString() {
		Collection col = new HashSet();
		col.add("test1");

		assertEquals("test1", DataAccessUtils.uniqueResult(col));
		assertEquals("test1", DataAccessUtils.requiredUniqueResult(col));
		assertEquals("test1", DataAccessUtils.objectResult(col, String.class));

		try {
			DataAccessUtils.intResult(col);
			fail("Should have thrown TypeMismatchDataAccessException");
		}
		catch (TypeMismatchDataAccessException ex) {
			// expected
		}

		try {
			DataAccessUtils.longResult(col);
			fail("Should have thrown TypeMismatchDataAccessException");
		}
		catch (TypeMismatchDataAccessException ex) {
			// expected
		}
	}

	public void testWithDate() {
		Date date = new Date();
		Collection col = new HashSet();
		col.add(date);

		assertEquals(date, DataAccessUtils.uniqueResult(col));
		assertEquals(date, DataAccessUtils.requiredUniqueResult(col));
		assertEquals(date, DataAccessUtils.objectResult(col, Date.class));
		assertEquals(date.toString(), DataAccessUtils.objectResult(col, String.class));

		try {
			DataAccessUtils.intResult(col);
			fail("Should have thrown TypeMismatchDataAccessException");
		}
		catch (TypeMismatchDataAccessException ex) {
			// expected
		}

		try {
			DataAccessUtils.longResult(col);
			fail("Should have thrown TypeMismatchDataAccessException");
		}
		catch (TypeMismatchDataAccessException ex) {
			// expected
		}
	}

	public void testExceptionTranslationWithNoTranslation() {
		MapPersistenceExceptionTranslator mpet = new MapPersistenceExceptionTranslator();
		RuntimeException in = new RuntimeException();
		assertSame(in, DataAccessUtils.translateIfNecessary(in, mpet));
	}

	public void testExceptionTranslationWithTranslation() {
		MapPersistenceExceptionTranslator mpet = new MapPersistenceExceptionTranslator();
		RuntimeException in = new RuntimeException("in");
		InvalidDataAccessApiUsageException out = new InvalidDataAccessApiUsageException("out");
		mpet.addTranslation(in, out);
		assertSame(out, DataAccessUtils.translateIfNecessary(in, mpet));
	}


	public static class MapPersistenceExceptionTranslator implements PersistenceExceptionTranslator {

		/**
		 * Map<RuntimeException,RuntimeException>: in to out
		 */
		private Map translations = new HashMap();

		public void addTranslation(RuntimeException in, RuntimeException out) {
			this.translations.put(in, out);
		}

		public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
			return (DataAccessException) translations.get(ex);
		}
	}

}
