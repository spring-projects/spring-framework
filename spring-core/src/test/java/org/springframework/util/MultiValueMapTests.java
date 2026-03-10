/*
 * Copyright 2002-present the original author or authors.
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

/**
 * Tests for {@link MultiValueMap}.
 *
 * @author Mihai Dumitrescu
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
class MultiValueMapTests {

	@Nested
	@ParameterizedClass
	@MethodSource("mapsUnderTest")
	class ParameterizedMultiValueMapTests {

		private final MultiValueMap<String, String> map;

		public ParameterizedMultiValueMapTests(MultiValueMap<String, String> map) {
			this.map = new LinkedMultiValueMap<>(map);
		}

		@Test
		void add() {
			int initialSize = map.size();
			map.add("key", "value1");
			map.add("key", "value2");
			assertThat(map).hasSize(initialSize + 1);
			assertThat(map.get("key")).containsExactly("value1", "value2");
		}

		@Test
		void addIfAbsentWhenAbsent() {
			map.addIfAbsent("key", "value1");
			assertThat(map.get("key")).containsExactly("value1");
		}

		@Test
		void addIfAbsentWhenPresent() {
			map.add("key", "value1");
			map.addIfAbsent("key", "value2");
			assertThat(map.get("key")).containsExactly("value1");
		}

		@Test
		void set() {
			map.set("key", "value1");
			map.set("key", "value2");
			assertThat(map.get("key")).containsExactly("value2");
		}

		@Test
		void addAll() {
			int initialSize = map.size();
			map.add("key", "value1");
			map.addAll("key", Arrays.asList("value2", "value3"));
			assertThat(map).hasSize(initialSize + 1);
			assertThat(map.get("key")).containsExactly("value1", "value2", "value3");
		}

		@Test
		void addAllWithEmptyList() {
			int initialSize = map.size();
			map.addAll("key", List.of());
			assertThat(map).hasSize(initialSize + 1);
			assertThat(map.get("key")).isEmpty();
			assertThat(map.getFirst("key")).isNull();
		}

		@Test
		void getFirst() {
			List<String> values = List.of("value1", "value2");
			map.put("key", values);
			assertThat(map.getFirst("key")).isEqualTo("value1");
			assertThat(map.getFirst("other")).isNull();
		}

		@Test
		void toSingleValueMap() {
			int initialSize = map.size();
			List<String> values = List.of("value1", "value2");
			map.put("key", values);
			Map<String, String> singleValueMap = map.toSingleValueMap();
			assertThat(singleValueMap).hasSize(initialSize + 1);
			assertThat(singleValueMap.get("key")).isEqualTo("value1");
		}

		@Test
		void toSingleValueMapWithEmptyList() {
			int initialSize = map.size();
			map.put("key", List.of());
			Map<String, String> singleValueMap = map.toSingleValueMap();
			assertThat(singleValueMap).hasSize(initialSize);
			assertThat(singleValueMap.get("key")).isNull();
		}

		@Test
		void equalsOnExistingValues() {
			map.clear();
			map.set("key1", "value1");
			assertThat(map).isEqualTo(map);
		}

		@Test
		void equalsOnEmpty() {
			map.clear();
			map.set("key1", "value1");
			MultiValueMap<String, String> map1 = new LinkedMultiValueMap<>();
			map1.set("key1", "value1");
			assertThat(map1).isEqualTo(map);
			assertThat(map).isEqualTo(map1);
			Map<String, List<String>> map2 = Map.of("key1", List.of("value1"));
			assertThat(map2).isEqualTo(map);
			assertThat(map).isEqualTo(map2);
		}

		private static Stream<Arguments> mapsUnderTest() {
			return Stream.of(
					argumentSet("new LinkedMultiValueMap<>()", new LinkedMultiValueMap<>()),
					argumentSet("new LinkedMultiValueMap<>(new HashMap<>())", new LinkedMultiValueMap<>(new HashMap<>())),
					argumentSet("new LinkedMultiValueMap<>(new LinkedHashMap<>())", new LinkedMultiValueMap<>(new LinkedHashMap<>())),
					argumentSet("new LinkedMultiValueMap<>(Map.of(...))", new LinkedMultiValueMap<>(Map.of("existingkey", List.of("existingvalue1", "existingvalue2")))),
					argumentSet("CollectionUtils.toMultiValueMap", CollectionUtils.toMultiValueMap(new HashMap<>()))
			);
		}
	}

	@Test
	void canNotChangeAnUnmodifiableMultiValueMap() {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		MultiValueMap<String, String> unmodifiableMap = CollectionUtils.unmodifiableMultiValueMap(map);
		assertSoftly(softly -> {
			softly.assertThatExceptionOfType(UnsupportedOperationException.class)
					.isThrownBy(() -> unmodifiableMap.add("key", "value"));
			softly.assertThatExceptionOfType(UnsupportedOperationException.class)
					.isThrownBy(() -> unmodifiableMap.addIfAbsent("key", "value"));
			softly.assertThatExceptionOfType(UnsupportedOperationException.class)
					.isThrownBy(() -> unmodifiableMap.addAll("key", exampleListOfValues()));
			softly.assertThatExceptionOfType(UnsupportedOperationException.class)
					.isThrownBy(() -> unmodifiableMap.addAll(exampleMultiValueMap()));
			softly.assertThatExceptionOfType(UnsupportedOperationException.class)
					.isThrownBy(() -> unmodifiableMap.set("key", "value"));
			softly.assertThatExceptionOfType(UnsupportedOperationException.class)
					.isThrownBy(() -> unmodifiableMap.setAll(exampleHashMap()));
			softly.assertThatExceptionOfType(UnsupportedOperationException.class)
					.isThrownBy(() -> unmodifiableMap.put("key", exampleListOfValues()));
			softly.assertThatExceptionOfType(UnsupportedOperationException.class)
					.isThrownBy(() -> unmodifiableMap.putIfAbsent("key", exampleListOfValues()));
			softly.assertThatExceptionOfType(UnsupportedOperationException.class)
					.isThrownBy(() -> unmodifiableMap.putAll(exampleMultiValueMap()));
			softly.assertThatExceptionOfType(UnsupportedOperationException.class)
					.isThrownBy(() -> unmodifiableMap.remove("key1"));
		});
	}

	private static List<String> exampleListOfValues() {
		return List.of("value1", "value2");
	}

	private static Map<String, String> exampleHashMap() {
		return Map.of("key2", "key2.value1");
	}

	private static MultiValueMap<String, String> exampleMultiValueMap() {
		LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.put("key1", Arrays.asList("key1.value1", "key1.value2"));
		return map;
	}

}
