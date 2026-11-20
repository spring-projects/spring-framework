/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Yanming Zhou
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

	@Test
	void nullable() {
		Map<String, @Nullable String> map = new HashMap<>();
		map.put("foo", null);
		map.put("bar", "bar");
		map.put("baz", "baz");
		FilteredMap<String, @Nullable String> filtered = new FilteredMap<>(map, s -> !s.equals("baz"));

		assertThat(filtered).containsEntry("foo", null).containsEntry("bar", "bar").doesNotContainKeys("baz");
	}
}
