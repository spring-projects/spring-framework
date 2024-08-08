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

package org.springframework.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class FilteredMapTests {

	@Test
	void size() {
		Map<String, String> map = Map.of("foo", "bar", "baz", "qux", "quux", "corge");
		FilteredMap<String, String> filtered = new FilteredMap<>(map, s -> !s.equals("baz"));

		assertThat(filtered).hasSize(2);
	}

	@Test
	void entrySet() {
		Map<String, String> map = Map.of("foo", "bar", "baz", "qux", "quux", "corge");
		FilteredMap<String, String> filtered = new FilteredMap<>(map, s -> !s.equals("baz"));

		assertThat(filtered.entrySet()).containsExactlyInAnyOrder(entry("foo", "bar"), entry("quux", "corge"));
	}

	@Test
	void containsKey() {
		Map<String, String> map = Map.of("foo", "bar", "baz", "qux", "quux", "corge");
		FilteredMap<String, String> filtered = new FilteredMap<>(map, s -> !s.equals("baz"));

		boolean contained = filtered.containsKey("baz");
		assertThat(contained).isFalse();
	}

	@Test
	void get() {
		Map<String, String> map = Map.of("foo", "bar", "baz", "qux", "quux", "corge");
		FilteredMap<String, String> filtered = new FilteredMap<>(map, s -> !s.equals("baz"));

		String value = filtered.get("baz");
		assertThat(value).isNull();
	}

	@Test
	void put() {
		Map<String, String> map = new HashMap<>(Map.of("foo", "bar", "quux", "corge"));
		FilteredMap<String, String> filtered = new FilteredMap<>(map, s -> !s.equals("baz"));

		String value = filtered.put("baz", "qux");
		assertThat(value).isNull();
		assertThat(filtered.containsKey("baz")).isFalse();
		assertThat(map.get("baz")).isEqualTo("qux");

		// overwrite
		value = filtered.put("baz", "QUX");
		assertThat(value).isNull();
		assertThat(filtered.containsKey("baz")).isFalse();
		assertThat(map.get("baz")).isEqualTo("QUX");
	}

	@Test
	void remove() {
		Map<String, String> map = new HashMap<>(Map.of("foo", "bar", "baz", "qux", "quux", "corge"));
		FilteredMap<String, String> filtered = new FilteredMap<>(map, s -> !s.equals("baz"));

		String value = filtered.remove("baz");
		assertThat(value).isNull();
		assertThat(filtered.containsKey("baz")).isFalse();
		assertThat(map.containsKey("baz")).isFalse();
	}

	@Test
	void keySet() {
		Map<String, String> map = Map.of("foo", "bar", "baz", "qux", "quux", "corge");
		FilteredMap<String, String> filtered = new FilteredMap<>(map, s -> !s.equals("baz"));

		Set<String> keySet = filtered.keySet();
		assertThat(keySet).containsExactlyInAnyOrder("foo", "quux");
	}
}
