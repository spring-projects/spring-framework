/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core;

import org.junit.Test;

import java.util.Locale;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rick Evans
 * @since 28.04.2003
 */
public class ConstantsTests {

	@Test
	public void constants() {
		Constants c = new Constants(A.class);
		assertEquals(A.class.getName(), c.getClassName());
		assertEquals(9, c.getSize());

		assertEquals(c.asNumber("DOG").intValue(), A.DOG);
		assertEquals(c.asNumber("dog").intValue(), A.DOG);
		assertEquals(c.asNumber("cat").intValue(), A.CAT);

		try {
			c.asNumber("bogus");
			fail("Can't get bogus field");
		}
		catch (ConstantException expected) {
		}

		assertTrue(c.asString("S1").equals(A.S1));
		try {
			c.asNumber("S1");
			fail("Wrong type");
		}
		catch (ConstantException expected) {
		}
	}

	@Test
	public void getNames() {
		Constants c = new Constants(A.class);

		Set<?> names = c.getNames("");
		assertEquals(c.getSize(), names.size());
		assertTrue(names.contains("DOG"));
		assertTrue(names.contains("CAT"));
		assertTrue(names.contains("S1"));

		names = c.getNames("D");
		assertEquals(1, names.size());
		assertTrue(names.contains("DOG"));

		names = c.getNames("d");
		assertEquals(1, names.size());
		assertTrue(names.contains("DOG"));
	}

	@Test
	public void getValues() {
		Constants c = new Constants(A.class);

		Set<?> values = c.getValues("");
		assertEquals(7, values.size());
		assertTrue(values.contains(new Integer(0)));
		assertTrue(values.contains(new Integer(66)));
		assertTrue(values.contains(""));

		values = c.getValues("D");
		assertEquals(1, values.size());
		assertTrue(values.contains(new Integer(0)));

		values = c.getValues("prefix");
		assertEquals(2, values.size());
		assertTrue(values.contains(new Integer(1)));
		assertTrue(values.contains(new Integer(2)));

		values = c.getValuesForProperty("myProperty");
		assertEquals(2, values.size());
		assertTrue(values.contains(new Integer(1)));
		assertTrue(values.contains(new Integer(2)));
	}

	@Test
	public void getValuesInTurkey() {
		Locale oldLocale = Locale.getDefault();
		Locale.setDefault(new Locale("tr", ""));
		try {
			Constants c = new Constants(A.class);

			Set<?> values = c.getValues("");
			assertEquals(7, values.size());
			assertTrue(values.contains(new Integer(0)));
			assertTrue(values.contains(new Integer(66)));
			assertTrue(values.contains(""));

			values = c.getValues("D");
			assertEquals(1, values.size());
			assertTrue(values.contains(new Integer(0)));

			values = c.getValues("prefix");
			assertEquals(2, values.size());
			assertTrue(values.contains(new Integer(1)));
			assertTrue(values.contains(new Integer(2)));

			values = c.getValuesForProperty("myProperty");
			assertEquals(2, values.size());
			assertTrue(values.contains(new Integer(1)));
			assertTrue(values.contains(new Integer(2)));
		}
		finally {
			Locale.setDefault(oldLocale);
		}
	}

	@Test
	public void suffixAccess() {
		Constants c = new Constants(A.class);

		Set<?> names = c.getNamesForSuffix("_PROPERTY");
		assertEquals(2, names.size());
		assertTrue(names.contains("NO_PROPERTY"));
		assertTrue(names.contains("YES_PROPERTY"));

		Set<?> values = c.getValuesForSuffix("_PROPERTY");
		assertEquals(2, values.size());
		assertTrue(values.contains(new Integer(3)));
		assertTrue(values.contains(new Integer(4)));
	}

	@Test
	public void toCode() {
		Constants c = new Constants(A.class);

		assertEquals(c.toCode(new Integer(0), ""), "DOG");
		assertEquals(c.toCode(new Integer(0), "D"), "DOG");
		assertEquals(c.toCode(new Integer(0), "DO"), "DOG");
		assertEquals(c.toCode(new Integer(0), "DoG"), "DOG");
		assertEquals(c.toCode(new Integer(0), null), "DOG");
		assertEquals(c.toCode(new Integer(66), ""), "CAT");
		assertEquals(c.toCode(new Integer(66), "C"), "CAT");
		assertEquals(c.toCode(new Integer(66), "ca"), "CAT");
		assertEquals(c.toCode(new Integer(66), "cAt"), "CAT");
		assertEquals(c.toCode(new Integer(66), null), "CAT");
		assertEquals(c.toCode("", ""), "S1");
		assertEquals(c.toCode("", "s"), "S1");
		assertEquals(c.toCode("", "s1"), "S1");
		assertEquals(c.toCode("", null), "S1");
		try {
			c.toCode("bogus", "bogus");
			fail("Should have thrown ConstantException");
		}
		catch (ConstantException expected) {
		}
		try {
			c.toCode("bogus", null);
			fail("Should have thrown ConstantException");
		}
		catch (ConstantException expected) {
		}

		assertEquals(c.toCodeForProperty(new Integer(1), "myProperty"), "MY_PROPERTY_NO");
		assertEquals(c.toCodeForProperty(new Integer(2), "myProperty"), "MY_PROPERTY_YES");
		try {
			c.toCodeForProperty("bogus", "bogus");
			fail("Should have thrown ConstantException");
		}
		catch (ConstantException expected) {
		}

		assertEquals(c.toCodeForSuffix(new Integer(0), ""), "DOG");
		assertEquals(c.toCodeForSuffix(new Integer(0), "G"), "DOG");
		assertEquals(c.toCodeForSuffix(new Integer(0), "OG"), "DOG");
		assertEquals(c.toCodeForSuffix(new Integer(0), "DoG"), "DOG");
		assertEquals(c.toCodeForSuffix(new Integer(0), null), "DOG");
		assertEquals(c.toCodeForSuffix(new Integer(66), ""), "CAT");
		assertEquals(c.toCodeForSuffix(new Integer(66), "T"), "CAT");
		assertEquals(c.toCodeForSuffix(new Integer(66), "at"), "CAT");
		assertEquals(c.toCodeForSuffix(new Integer(66), "cAt"), "CAT");
		assertEquals(c.toCodeForSuffix(new Integer(66), null), "CAT");
		assertEquals(c.toCodeForSuffix("", ""), "S1");
		assertEquals(c.toCodeForSuffix("", "1"), "S1");
		assertEquals(c.toCodeForSuffix("", "s1"), "S1");
		assertEquals(c.toCodeForSuffix("", null), "S1");
		try {
			c.toCodeForSuffix("bogus", "bogus");
			fail("Should have thrown ConstantException");
		}
		catch (ConstantException expected) {
		}
		try {
			c.toCodeForSuffix("bogus", null);
			fail("Should have thrown ConstantException");
		}
		catch (ConstantException expected) {
		}
	}

	@Test
	public void getValuesWithNullPrefix() throws Exception {
		Constants c = new Constants(A.class);
		Set<?> values = c.getValues(null);
		assertEquals("Must have returned *all* public static final values", 7, values.size());
	}

	@Test
	public void getValuesWithEmptyStringPrefix() throws Exception {
		Constants c = new Constants(A.class);
		Set<Object> values = c.getValues("");
		assertEquals("Must have returned *all* public static final values", 7, values.size());
	}

	@Test
	public void getValuesWithWhitespacedStringPrefix() throws Exception {
		Constants c = new Constants(A.class);
		Set<?> values = c.getValues(" ");
		assertEquals("Must have returned *all* public static final values", 7, values.size());
	}

	@Test
	public void withClassThatExposesNoConstants() throws Exception {
		Constants c = new Constants(NoConstants.class);
		assertEquals(0, c.getSize());
		final Set<?> values = c.getValues("");
		assertNotNull(values);
		assertEquals(0, values.size());
	}

	@Test
	public void ctorWithNullClass() throws Exception {
		try {
			new Constants(null);
			fail("Must have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException expected) {}
	}


	private static final class NoConstants {
	}


	@SuppressWarnings("unused")
	private static final class A {

		public static final int DOG = 0;
		public static final int CAT = 66;
		public static final String S1 = "";

		public static final int PREFIX_NO = 1;
		public static final int PREFIX_YES = 2;

		public static final int MY_PROPERTY_NO = 1;
		public static final int MY_PROPERTY_YES = 2;

		public static final int NO_PROPERTY = 3;
		public static final int YES_PROPERTY = 4;

		/** ignore these */
		protected static final int P = -1;
		protected boolean f;
		static final Object o = new Object();
	}

}
