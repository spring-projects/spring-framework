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

package org.springframework.core.style;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
	}

	@Test
	void stylePlainObject() {
		Object obj = new Object();

		assertThat(styler.style(obj)).isEqualTo(String.valueOf(obj));
	}

	@Test
	void styleMaps() {
		Map<String, Integer> map = Collections.emptyMap();
		assertThat(styler.style(map)).isEqualTo("map[[empty]]");

		map = Collections.singletonMap("key", 1);
		assertThat(styler.style(map)).isEqualTo("map['key' -> 1]");

		map = new HashMap<>();
		map.put("key1", 1);
		map.put("key2", 2);
		assertThat(styler.style(map)).isEqualTo("map['key1' -> 1, 'key2' -> 2]");
	}

	@Test
	void styleMapEntries() {
		Map<String, Integer> map = new LinkedHashMap<>();
		map.put("key1", 1);
		map.put("key2", 2);

		Iterator<Map.Entry<String, Integer>> entries = map.entrySet().iterator();

		assertThat(styler.style(entries.next())).isEqualTo("'key1' -> 1");
		assertThat(styler.style(entries.next())).isEqualTo("'key2' -> 2");
	}

	@Test
	void styleCollections() {
		List<Integer> list = Collections.emptyList();
		assertThat(styler.style(list)).isEqualTo("list[[empty]]");

		list = Collections.singletonList(1);
		assertThat(styler.style(list)).isEqualTo("list[1]");

		list = Arrays.asList(1, 2);
		assertThat(styler.style(list)).isEqualTo("list[1, 2]");
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
