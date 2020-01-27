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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rick Evans
 * @since 28.04.2003
 */
class ConstantsTests {

	@Test
	void constants() {
		Constants c = new Constants(A.class);
		assertThat(c.getClassName()).isEqualTo(A.class.getName());
		assertThat(c.getSize()).isEqualTo(9);

		assertThat(c.asNumber("DOG").intValue()).isEqualTo(A.DOG);
		assertThat(c.asNumber("dog").intValue()).isEqualTo(A.DOG);
		assertThat(c.asNumber("cat").intValue()).isEqualTo(A.CAT);

		assertThatExceptionOfType(Constants.ConstantException.class).isThrownBy(() ->
				c.asNumber("bogus"));

		assertThat(c.asString("S1").equals(A.S1)).isTrue();
		assertThatExceptionOfType(Constants.ConstantException.class).as("wrong type").isThrownBy(() ->
				c.asNumber("S1"));
	}

	@Test
	void getNames() {
		Constants c = new Constants(A.class);

		Set<?> names = c.getNames("");
		assertThat(names.size()).isEqualTo(c.getSize());
		assertThat(names.contains("DOG")).isTrue();
		assertThat(names.contains("CAT")).isTrue();
		assertThat(names.contains("S1")).isTrue();

		names = c.getNames("D");
		assertThat(names.size()).isEqualTo(1);
		assertThat(names.contains("DOG")).isTrue();

		names = c.getNames("d");
		assertThat(names.size()).isEqualTo(1);
		assertThat(names.contains("DOG")).isTrue();
	}

	@Test
	void getValues() {
		Constants c = new Constants(A.class);

		Set<?> values = c.getValues("");
		assertThat(values.size()).isEqualTo(7);
		assertThat(values.contains(Integer.valueOf(0))).isTrue();
		assertThat(values.contains(Integer.valueOf(66))).isTrue();
		assertThat(values.contains("")).isTrue();

		values = c.getValues("D");
		assertThat(values.size()).isEqualTo(1);
		assertThat(values.contains(Integer.valueOf(0))).isTrue();

		values = c.getValues("prefix");
		assertThat(values.size()).isEqualTo(2);
		assertThat(values.contains(Integer.valueOf(1))).isTrue();
		assertThat(values.contains(Integer.valueOf(2))).isTrue();

		values = c.getValuesForProperty("myProperty");
		assertThat(values.size()).isEqualTo(2);
		assertThat(values.contains(Integer.valueOf(1))).isTrue();
		assertThat(values.contains(Integer.valueOf(2))).isTrue();
	}

	@Test
	void getValuesInTurkey() {
		Locale oldLocale = Locale.getDefault();
		Locale.setDefault(new Locale("tr", ""));
		try {
			Constants c = new Constants(A.class);

			Set<?> values = c.getValues("");
			assertThat(values.size()).isEqualTo(7);
			assertThat(values.contains(Integer.valueOf(0))).isTrue();
			assertThat(values.contains(Integer.valueOf(66))).isTrue();
			assertThat(values.contains("")).isTrue();

			values = c.getValues("D");
			assertThat(values.size()).isEqualTo(1);
			assertThat(values.contains(Integer.valueOf(0))).isTrue();

			values = c.getValues("prefix");
			assertThat(values.size()).isEqualTo(2);
			assertThat(values.contains(Integer.valueOf(1))).isTrue();
			assertThat(values.contains(Integer.valueOf(2))).isTrue();

			values = c.getValuesForProperty("myProperty");
			assertThat(values.size()).isEqualTo(2);
			assertThat(values.contains(Integer.valueOf(1))).isTrue();
			assertThat(values.contains(Integer.valueOf(2))).isTrue();
		}
		finally {
			Locale.setDefault(oldLocale);
		}
	}

	@Test
	void suffixAccess() {
		Constants c = new Constants(A.class);

		Set<?> names = c.getNamesForSuffix("_PROPERTY");
		assertThat(names.size()).isEqualTo(2);
		assertThat(names.contains("NO_PROPERTY")).isTrue();
		assertThat(names.contains("YES_PROPERTY")).isTrue();

		Set<?> values = c.getValuesForSuffix("_PROPERTY");
		assertThat(values.size()).isEqualTo(2);
		assertThat(values.contains(Integer.valueOf(3))).isTrue();
		assertThat(values.contains(Integer.valueOf(4))).isTrue();
	}

	@Test
	void toCode() {
		Constants c = new Constants(A.class);

		assertThat(c.toCode(Integer.valueOf(0), "")).isEqualTo("DOG");
		assertThat(c.toCode(Integer.valueOf(0), "D")).isEqualTo("DOG");
		assertThat(c.toCode(Integer.valueOf(0), "DO")).isEqualTo("DOG");
		assertThat(c.toCode(Integer.valueOf(0), "DoG")).isEqualTo("DOG");
		assertThat(c.toCode(Integer.valueOf(0), null)).isEqualTo("DOG");
		assertThat(c.toCode(Integer.valueOf(66), "")).isEqualTo("CAT");
		assertThat(c.toCode(Integer.valueOf(66), "C")).isEqualTo("CAT");
		assertThat(c.toCode(Integer.valueOf(66), "ca")).isEqualTo("CAT");
		assertThat(c.toCode(Integer.valueOf(66), "cAt")).isEqualTo("CAT");
		assertThat(c.toCode(Integer.valueOf(66), null)).isEqualTo("CAT");
		assertThat(c.toCode("", "")).isEqualTo("S1");
		assertThat(c.toCode("", "s")).isEqualTo("S1");
		assertThat(c.toCode("", "s1")).isEqualTo("S1");
		assertThat(c.toCode("", null)).isEqualTo("S1");
		assertThatExceptionOfType(Constants.ConstantException.class).isThrownBy(() ->
				c.toCode("bogus", "bogus"));
		assertThatExceptionOfType(Constants.ConstantException.class).isThrownBy(() ->
				c.toCode("bogus", null));

		assertThat(c.toCodeForProperty(Integer.valueOf(1), "myProperty")).isEqualTo("MY_PROPERTY_NO");
		assertThat(c.toCodeForProperty(Integer.valueOf(2), "myProperty")).isEqualTo("MY_PROPERTY_YES");
		assertThatExceptionOfType(Constants.ConstantException.class).isThrownBy(() ->
				c.toCodeForProperty("bogus", "bogus"));

		assertThat(c.toCodeForSuffix(Integer.valueOf(0), "")).isEqualTo("DOG");
		assertThat(c.toCodeForSuffix(Integer.valueOf(0), "G")).isEqualTo("DOG");
		assertThat(c.toCodeForSuffix(Integer.valueOf(0), "OG")).isEqualTo("DOG");
		assertThat(c.toCodeForSuffix(Integer.valueOf(0), "DoG")).isEqualTo("DOG");
		assertThat(c.toCodeForSuffix(Integer.valueOf(0), null)).isEqualTo("DOG");
		assertThat(c.toCodeForSuffix(Integer.valueOf(66), "")).isEqualTo("CAT");
		assertThat(c.toCodeForSuffix(Integer.valueOf(66), "T")).isEqualTo("CAT");
		assertThat(c.toCodeForSuffix(Integer.valueOf(66), "at")).isEqualTo("CAT");
		assertThat(c.toCodeForSuffix(Integer.valueOf(66), "cAt")).isEqualTo("CAT");
		assertThat(c.toCodeForSuffix(Integer.valueOf(66), null)).isEqualTo("CAT");
		assertThat(c.toCodeForSuffix("", "")).isEqualTo("S1");
		assertThat(c.toCodeForSuffix("", "1")).isEqualTo("S1");
		assertThat(c.toCodeForSuffix("", "s1")).isEqualTo("S1");
		assertThat(c.toCodeForSuffix("", null)).isEqualTo("S1");
		assertThatExceptionOfType(Constants.ConstantException.class).isThrownBy(() ->
				c.toCodeForSuffix("bogus", "bogus"));
		assertThatExceptionOfType(Constants.ConstantException.class).isThrownBy(() ->
				c.toCodeForSuffix("bogus", null));
	}

	@Test
	void getValuesWithNullPrefix() throws Exception {
		Constants c = new Constants(A.class);
		Set<?> values = c.getValues(null);
		assertThat(values.size()).as("Must have returned *all* public static final values").isEqualTo(7);
	}

	@Test
	void getValuesWithEmptyStringPrefix() throws Exception {
		Constants c = new Constants(A.class);
		Set<Object> values = c.getValues("");
		assertThat(values.size()).as("Must have returned *all* public static final values").isEqualTo(7);
	}

	@Test
	void getValuesWithWhitespacedStringPrefix() throws Exception {
		Constants c = new Constants(A.class);
		Set<?> values = c.getValues(" ");
		assertThat(values.size()).as("Must have returned *all* public static final values").isEqualTo(7);
	}

	@Test
	void withClassThatExposesNoConstants() throws Exception {
		Constants c = new Constants(NoConstants.class);
		assertThat(c.getSize()).isEqualTo(0);
		final Set<?> values = c.getValues("");
		assertThat(values).isNotNull();
		assertThat(values.size()).isEqualTo(0);
	}

	@Test
	void ctorWithNullClass() throws Exception {
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
