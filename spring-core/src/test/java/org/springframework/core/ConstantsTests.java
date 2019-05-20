/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.Locale;
import java.util.Set;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

		assertEquals(A.DOG, c.asNumber("DOG").intValue());
		assertEquals(A.DOG, c.asNumber("dog").intValue());
		assertEquals(A.CAT, c.asNumber("cat").intValue());

		assertThatExceptionOfType(Constants.ConstantException.class).isThrownBy(() ->
				c.asNumber("bogus"));

		assertTrue(c.asString("S1").equals(A.S1));
		assertThatExceptionOfType(Constants.ConstantException.class).as("wrong type").isThrownBy(() ->
				c.asNumber("S1"));
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
		assertTrue(values.contains(Integer.valueOf(0)));
		assertTrue(values.contains(Integer.valueOf(66)));
		assertTrue(values.contains(""));

		values = c.getValues("D");
		assertEquals(1, values.size());
		assertTrue(values.contains(Integer.valueOf(0)));

		values = c.getValues("prefix");
		assertEquals(2, values.size());
		assertTrue(values.contains(Integer.valueOf(1)));
		assertTrue(values.contains(Integer.valueOf(2)));

		values = c.getValuesForProperty("myProperty");
		assertEquals(2, values.size());
		assertTrue(values.contains(Integer.valueOf(1)));
		assertTrue(values.contains(Integer.valueOf(2)));
	}

	@Test
	public void getValuesInTurkey() {
		Locale oldLocale = Locale.getDefault();
		Locale.setDefault(new Locale("tr", ""));
		try {
			Constants c = new Constants(A.class);

			Set<?> values = c.getValues("");
			assertEquals(7, values.size());
			assertTrue(values.contains(Integer.valueOf(0)));
			assertTrue(values.contains(Integer.valueOf(66)));
			assertTrue(values.contains(""));

			values = c.getValues("D");
			assertEquals(1, values.size());
			assertTrue(values.contains(Integer.valueOf(0)));

			values = c.getValues("prefix");
			assertEquals(2, values.size());
			assertTrue(values.contains(Integer.valueOf(1)));
			assertTrue(values.contains(Integer.valueOf(2)));

			values = c.getValuesForProperty("myProperty");
			assertEquals(2, values.size());
			assertTrue(values.contains(Integer.valueOf(1)));
			assertTrue(values.contains(Integer.valueOf(2)));
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
		assertTrue(values.contains(Integer.valueOf(3)));
		assertTrue(values.contains(Integer.valueOf(4)));
	}

	@Test
	public void toCode() {
		Constants c = new Constants(A.class);

		assertEquals("DOG", c.toCode(Integer.valueOf(0), ""));
		assertEquals("DOG", c.toCode(Integer.valueOf(0), "D"));
		assertEquals("DOG", c.toCode(Integer.valueOf(0), "DO"));
		assertEquals("DOG", c.toCode(Integer.valueOf(0), "DoG"));
		assertEquals("DOG", c.toCode(Integer.valueOf(0), null));
		assertEquals("CAT", c.toCode(Integer.valueOf(66), ""));
		assertEquals("CAT", c.toCode(Integer.valueOf(66), "C"));
		assertEquals("CAT", c.toCode(Integer.valueOf(66), "ca"));
		assertEquals("CAT", c.toCode(Integer.valueOf(66), "cAt"));
		assertEquals("CAT", c.toCode(Integer.valueOf(66), null));
		assertEquals("S1", c.toCode("", ""));
		assertEquals("S1", c.toCode("", "s"));
		assertEquals("S1", c.toCode("", "s1"));
		assertEquals("S1", c.toCode("", null));
		assertThatExceptionOfType(Constants.ConstantException.class).isThrownBy(() ->
				c.toCode("bogus", "bogus"));
		assertThatExceptionOfType(Constants.ConstantException.class).isThrownBy(() ->
				c.toCode("bogus", null));

		assertEquals("MY_PROPERTY_NO", c.toCodeForProperty(Integer.valueOf(1), "myProperty"));
		assertEquals("MY_PROPERTY_YES", c.toCodeForProperty(Integer.valueOf(2), "myProperty"));
		assertThatExceptionOfType(Constants.ConstantException.class).isThrownBy(() ->
				c.toCodeForProperty("bogus", "bogus"));

		assertEquals("DOG", c.toCodeForSuffix(Integer.valueOf(0), ""));
		assertEquals("DOG", c.toCodeForSuffix(Integer.valueOf(0), "G"));
		assertEquals("DOG", c.toCodeForSuffix(Integer.valueOf(0), "OG"));
		assertEquals("DOG", c.toCodeForSuffix(Integer.valueOf(0), "DoG"));
		assertEquals("DOG", c.toCodeForSuffix(Integer.valueOf(0), null));
		assertEquals("CAT", c.toCodeForSuffix(Integer.valueOf(66), ""));
		assertEquals("CAT", c.toCodeForSuffix(Integer.valueOf(66), "T"));
		assertEquals("CAT", c.toCodeForSuffix(Integer.valueOf(66), "at"));
		assertEquals("CAT", c.toCodeForSuffix(Integer.valueOf(66), "cAt"));
		assertEquals("CAT", c.toCodeForSuffix(Integer.valueOf(66), null));
		assertEquals("S1", c.toCodeForSuffix("", ""));
		assertEquals("S1", c.toCodeForSuffix("", "1"));
		assertEquals("S1", c.toCodeForSuffix("", "s1"));
		assertEquals("S1", c.toCodeForSuffix("", null));
		assertThatExceptionOfType(Constants.ConstantException.class).isThrownBy(() ->
				c.toCodeForSuffix("bogus", "bogus"));
		assertThatExceptionOfType(Constants.ConstantException.class).isThrownBy(() ->
				c.toCodeForSuffix("bogus", null));
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
		assertThatIllegalArgumentException().isThrownBy(() ->
				new Constants(null));
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
