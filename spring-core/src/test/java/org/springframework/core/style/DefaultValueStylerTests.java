/*
 * Copyright 2002-2022 the original author or authors.
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

import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DefaultValueStyler}.
 *
 * @since 5.2
 */
class DefaultValueStylerTests {

	private final DefaultValueStyler styler = new DefaultValueStyler();


	@Test
	void styleBasics() throws NoSuchMethodException {
		assertThat(styler.style(null)).isEqualTo("[null]");
		assertThat(styler.style("str")).isEqualTo("'str'");
		assertThat(styler.style(String.class)).isEqualTo("String");
		assertThat(styler.style(String.class.getMethod("toString"))).isEqualTo("toString@String");
		assertThat(styler.style(String.class.getMethod("getBytes", Charset.class))).isEqualTo("getBytes@String");
	}

	@Test
	void stylePlainObject() {
		Object obj = new Object();
		assertThat(styler.style(obj)).isEqualTo(String.valueOf(obj));
	}

	@Test
	void styleMaps() {
		assertThat(styler.style(Map.of())).isEqualTo("map[[empty]]");
		assertThat(styler.style(Map.of("key", 1))).isEqualTo("map['key' -> 1]");

		Map<String, Integer> map = new LinkedHashMap<>() {{
			put("key1", 1);
			put("key2", 2);
		}};
		assertThat(styler.style(map)).isEqualTo("map['key1' -> 1, 'key2' -> 2]");
	}

	@Test
	void styleMapEntries() {
		Map<String, Integer> map = Map.of("key1", 1, "key2", 2);
		assertThat(map.entrySet()).map(styler::style).containsExactlyInAnyOrder("'key1' -> 1", "'key2' -> 2");
	}

	@Test
	void styleLists() {
		assertThat(styler.style(List.of())).isEqualTo("list[[empty]]");
		assertThat(styler.style(List.of(1))).isEqualTo("list[1]");
		assertThat(styler.style(List.of(1, 2))).isEqualTo("list[1, 2]");
	}

	@Test
	void stylePrimitiveArrays() {
		int[] array = new int[0];
		assertThat(styler.style(array)).isEqualTo("array<Object>[[empty]]");

		array = new int[] { 1 };
		assertThat(styler.style(array)).isEqualTo("array<Integer>[1]");

		array = new int[] { 1, 2 };
		assertThat(styler.style(array)).isEqualTo("array<Integer>[1, 2]");
	}

	@Test
	void styleObjectArrays() {
		String[] array = new String[0];
		assertThat(styler.style(array)).isEqualTo("array<String>[[empty]]");

		array = new String[] { "str1" };
		assertThat(styler.style(array)).isEqualTo("array<String>['str1']");

		array = new String[] { "str1", "str2" };
		assertThat(styler.style(array)).isEqualTo("array<String>['str1', 'str2']");
	}

}
