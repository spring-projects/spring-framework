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

package org.springframework.util;

import java.util.Iterator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LinkedCaseInsensitiveMap}.
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 */
class LinkedCaseInsensitiveMapTests {

	private final LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>();


	@Test
	void putAndGet() {
		assertThat(map.put("key", "value1")).isNull();
		assertThat(map.put("key", "value2")).isEqualTo("value1");
		assertThat(map.put("key", "value3")).isEqualTo("value2");
		assertThat(map).hasSize(1);
		assertThat(map.get("key")).isEqualTo("value3");
		assertThat(map.get("KEY")).isEqualTo("value3");
		assertThat(map.get("Key")).isEqualTo("value3");
		assertThat(map.containsKey("key")).isTrue();
		assertThat(map.containsKey("KEY")).isTrue();
		assertThat(map.containsKey("Key")).isTrue();
		assertThat(map.keySet().contains("key")).isTrue();
		assertThat(map.keySet().contains("KEY")).isTrue();
		assertThat(map.keySet().contains("Key")).isTrue();
	}

	@Test
	void putWithOverlappingKeys() {
		assertThat(map.put("key", "value1")).isNull();
		assertThat(map.put("KEY", "value2")).isEqualTo("value1");
		assertThat(map.put("Key", "value3")).isEqualTo("value2");
		assertThat(map).hasSize(1);
		assertThat(map.get("key")).isEqualTo("value3");
		assertThat(map.get("KEY")).isEqualTo("value3");
		assertThat(map.get("Key")).isEqualTo("value3");
		assertThat(map.containsKey("key")).isTrue();
		assertThat(map.containsKey("KEY")).isTrue();
		assertThat(map.containsKey("Key")).isTrue();
		assertThat(map.keySet().contains("key")).isTrue();
		assertThat(map.keySet().contains("KEY")).isTrue();
		assertThat(map.keySet().contains("Key")).isTrue();
	}

	@Test
	void getOrDefault() {
		assertThat(map.put("key", "value1")).isNull();
		assertThat(map.put("KEY", "value2")).isEqualTo("value1");
		assertThat(map.put("Key", "value3")).isEqualTo("value2");
		assertThat(map.getOrDefault("key", "N")).isEqualTo("value3");
		assertThat(map.getOrDefault("KEY", "N")).isEqualTo("value3");
		assertThat(map.getOrDefault("Key", "N")).isEqualTo("value3");
		assertThat(map.getOrDefault("keeeey", "N")).isEqualTo("N");
		assertThat(map.getOrDefault(new Object(), "N")).isEqualTo("N");
	}

	@Test
	void getOrDefaultWithNullValue() {
		assertThat(map.put("key", null)).isNull();
		assertThat(map.put("KEY", null)).isNull();
		assertThat(map.put("Key", null)).isNull();
		assertThat(map.getOrDefault("key", "N")).isNull();
		assertThat(map.getOrDefault("KEY", "N")).isNull();
		assertThat(map.getOrDefault("Key", "N")).isNull();
		assertThat(map.getOrDefault("keeeey", "N")).isEqualTo("N");
		assertThat(map.getOrDefault(new Object(), "N")).isEqualTo("N");
	}

	@Test
	void computeIfAbsentWithExistingValue() {
		assertThat(map.putIfAbsent("key", "value1")).isNull();
		assertThat(map.putIfAbsent("KEY", "value2")).isEqualTo("value1");
		assertThat(map.put("Key", "value3")).isEqualTo("value1");
		assertThat(map.computeIfAbsent("key", key2 -> "value1")).isEqualTo("value3");
		assertThat(map.computeIfAbsent("KEY", key1 -> "value2")).isEqualTo("value3");
		assertThat(map.computeIfAbsent("Key", key -> "value3")).isEqualTo("value3");

		assertThat(map.put("null", null)).isNull();
		assertThat(map.putIfAbsent("NULL", "value")).isNull();
		assertThat(map.put("null", null)).isEqualTo("value");
		assertThat(map.computeIfAbsent("NULL", s -> "value")).isEqualTo("value");
		assertThat(map.get("null")).isEqualTo("value");
	}

	@Test
	void computeIfAbsentWithComputedValue() {
		assertThat(map.computeIfAbsent("key", key2 -> "value1")).isEqualTo("value1");
		assertThat(map.computeIfAbsent("KEY", key1 -> "value2")).isEqualTo("value1");
		assertThat(map.computeIfAbsent("Key", key -> "value3")).isEqualTo("value1");
	}

	@Test
	void mapClone() {
		assertThat(map.put("key", "value1")).isNull();
		LinkedCaseInsensitiveMap<String> copy = map.clone();

		assertThat(copy.getLocale()).isEqualTo(map.getLocale());
		assertThat(map.get("key")).isEqualTo("value1");
		assertThat(map.get("KEY")).isEqualTo("value1");
		assertThat(map.get("Key")).isEqualTo("value1");
		assertThat(copy.get("key")).isEqualTo("value1");
		assertThat(copy.get("KEY")).isEqualTo("value1");
		assertThat(copy.get("Key")).isEqualTo("value1");

		copy.put("Key", "value2");
		assertThat(map).hasSize(1);
		assertThat(copy).hasSize(1);
		assertThat(map.get("key")).isEqualTo("value1");
		assertThat(map.get("KEY")).isEqualTo("value1");
		assertThat(map.get("Key")).isEqualTo("value1");
		assertThat(copy.get("key")).isEqualTo("value2");
		assertThat(copy.get("KEY")).isEqualTo("value2");
		assertThat(copy.get("Key")).isEqualTo("value2");
	}


	@Test
	void clearFromKeySet() {
		map.put("key", "value");
		map.keySet().clear();
		map.computeIfAbsent("key", k -> "newvalue");
		assertThat(map.get("key")).isEqualTo("newvalue");
	}

	@Test
	void removeFromKeySet() {
		map.put("key", "value");
		map.keySet().remove("key");
		map.computeIfAbsent("key", k -> "newvalue");
		assertThat(map.get("key")).isEqualTo("newvalue");
	}

	@Test
	void removeFromKeySetViaIterator() {
		map.put("key", "value");
		nextAndRemove(map.keySet().iterator());
		assertThat(map).isEmpty();
		map.computeIfAbsent("key", k -> "newvalue");
		assertThat(map.get("key")).isEqualTo("newvalue");
	}

	@Test
	void clearFromValues() {
		map.put("key", "value");
		map.values().clear();
		assertThat(map).isEmpty();
		map.computeIfAbsent("key", k -> "newvalue");
		assertThat(map.get("key")).isEqualTo("newvalue");
	}

	@Test
	void removeFromValues() {
		map.put("key", "value");
		map.values().remove("value");
		assertThat(map).isEmpty();
		map.computeIfAbsent("key", k -> "newvalue");
		assertThat(map.get("key")).isEqualTo("newvalue");
	}

	@Test
	void removeFromValuesViaIterator() {
		map.put("key", "value");
		nextAndRemove(map.values().iterator());
		assertThat(map).isEmpty();
		map.computeIfAbsent("key", k -> "newvalue");
		assertThat(map.get("key")).isEqualTo("newvalue");
	}

	@Test
	void clearFromEntrySet() {
		map.put("key", "value");
		map.entrySet().clear();
		assertThat(map).isEmpty();
		map.computeIfAbsent("key", k -> "newvalue");
		assertThat(map.get("key")).isEqualTo("newvalue");
	}

	@Test
	void removeFromEntrySet() {
		map.put("key", "value");
		map.entrySet().remove(map.entrySet().iterator().next());
		assertThat(map).isEmpty();
		map.computeIfAbsent("key", k -> "newvalue");
		assertThat(map.get("key")).isEqualTo("newvalue");
	}

	@Test
	void removeFromEntrySetViaIterator() {
		map.put("key", "value");
		nextAndRemove(map.entrySet().iterator());
		assertThat(map).isEmpty();
		map.computeIfAbsent("key", k -> "newvalue");
		assertThat(map.get("key")).isEqualTo("newvalue");
	}

	private void nextAndRemove(Iterator<?> iterator) {
		iterator.next();
		iterator.remove();
	}

}
