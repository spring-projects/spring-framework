/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.core.style;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SimpleValueStyler}.
 *
 * @author Sam Brannen
 * @since 6.0
 */
class SimpleValueStylerTests {

	@Nested
	class CommonStyling {

		private final SimpleValueStyler styler = new SimpleValueStyler();

		@Test
		void styleBasics() {
			assertThat(styler.style(null)).isEqualTo("null");
			assertThat(styler.style(true)).isEqualTo("true");
			assertThat(styler.style(99.9)).isEqualTo("99.9");
			assertThat(styler.style("str")).isEqualTo("\"str\"");
		}

		@Test
		void stylePlainObject() {
			Object obj = new Object();

			assertThat(styler.style(obj)).isEqualTo(String.valueOf(obj));
		}

		@Test
		void styleMaps() {
			assertThat(styler.style(Map.of())).isEqualTo("{}");
			assertThat(styler.style(Map.of("key", 1))).isEqualTo("{\"key\" -> 1}");

			Map<String, Integer> map = new LinkedHashMap<>() {{
				put("key1", 1);
				put("key2", 2);
			}};
			assertThat(styler.style(map)).isEqualTo("{\"key1\" -> 1, \"key2\" -> 2}");
		}

		@Test
		void styleMapEntries() {
			Map<String, Integer> map = Map.of("key1", 1, "key2", 2);

			assertThat(map.entrySet()).map(styler::style).containsExactlyInAnyOrder("\"key1\" -> 1", "\"key2\" -> 2");
		}

		@Test
		void styleLists() {
			assertThat(styler.style(List.of())).isEqualTo("[]");
			assertThat(styler.style(List.of(1))).isEqualTo("[1]");
			assertThat(styler.style(List.of(1, 2))).isEqualTo("[1, 2]");
		}

		@Test
		void stylePrimitiveArrays() {
			int[] array = new int[0];
			assertThat(styler.style(array)).isEqualTo("[]");

			array = new int[] { 1 };
			assertThat(styler.style(array)).isEqualTo("[1]");

			array = new int[] { 1, 2 };
			assertThat(styler.style(array)).isEqualTo("[1, 2]");
		}

		@Test
		void styleObjectArrays() {
			String[] array = new String[0];
			assertThat(styler.style(array)).isEqualTo("[]");

			array = new String[] { "str1" };
			assertThat(styler.style(array)).isEqualTo("[\"str1\"]");

			array = new String[] { "str1", "str2" };
			assertThat(styler.style(array)).isEqualTo("[\"str1\", \"str2\"]");
		}

	}

	@Nested
	class DefaultClassAndMethodStylers {

		private final SimpleValueStyler styler = new SimpleValueStyler();

		@Test
		void styleClass() {
			assertThat(styler.style(String.class)).isEqualTo("java.lang.String");
			assertThat(styler.style(getClass())).isEqualTo(getClass().getCanonicalName());
			assertThat(styler.style(String[].class)).isEqualTo("java.lang.String[]");
			assertThat(styler.style(int[][].class)).isEqualTo("int[][]");
		}

		@Test
		void styleMethod() throws NoSuchMethodException {
			assertThat(styler.style(String.class.getMethod("toString"))).isEqualTo("toString()");
			assertThat(styler.style(String.class.getMethod("getBytes", Charset.class))).isEqualTo("getBytes(Charset)");
		}

		@Test
		void styleClassMap() {
			Map<String, Class<?>> map = new LinkedHashMap<>() {{
				put("key1", Integer.class);
				put("key2", DefaultClassAndMethodStylers.class);
			}};
			assertThat(styler.style(map)).isEqualTo(
					"{\"key1\" -> java.lang.Integer, \"key2\" -> %s}",
					DefaultClassAndMethodStylers.class.getCanonicalName());
		}

		@Test
		void styleClassList() {
			assertThat(styler.style(List.of(Integer.class, String.class)))
					.isEqualTo("[java.lang.Integer, java.lang.String]");
		}

		@Test
		void styleClassArray() {
			Class<?>[] array = new Class<?>[] { Integer.class, getClass() };
			assertThat(styler.style(array))
					.isEqualTo("[%s, %s]", Integer.class.getCanonicalName(), getClass().getCanonicalName());
		}

	}

	@Nested
	class CustomClassAndMethodStylers {

		private final SimpleValueStyler styler = new SimpleValueStyler(Class::getSimpleName, Method::toGenericString);

		@Test
		void styleClass() {
			assertThat(styler.style(String.class)).isEqualTo("String");
			assertThat(styler.style(getClass())).isEqualTo(getClass().getSimpleName());
			assertThat(styler.style(String[].class)).isEqualTo("String[]");
			assertThat(styler.style(int[][].class)).isEqualTo("int[][]");
		}

		@Test
		void styleMethod() throws NoSuchMethodException {
			Method method = String.class.getMethod("toString");
			assertThat(styler.style(method)).isEqualTo(method.toGenericString());
		}

		@Test
		void styleClassMap() {
			Map<String, Class<?>> map = new LinkedHashMap<>() {{
				put("key1", Integer.class);
				put("key2", CustomClassAndMethodStylers.class);
			}};
			assertThat(styler.style(map)).isEqualTo(
					"{\"key1\" -> %s, \"key2\" -> %s}",
					Integer.class.getSimpleName(), CustomClassAndMethodStylers.class.getSimpleName());
		}
		@Test
		void styleClassList() {
			assertThat(styler.style(List.of(Integer.class, String.class))).isEqualTo("[Integer, String]");
		}

		@Test
		void styleClassArray() {
			Class<?>[] array = new Class<?>[] { Integer.class, getClass() };
			assertThat(styler.style(array)).isEqualTo("[%s, %s]", Integer.class.getSimpleName(), getClass().getSimpleName());
		}

	}

}
