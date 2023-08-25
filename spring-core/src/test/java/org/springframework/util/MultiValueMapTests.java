/*
 * Copyright 2002-2023 the original author or authors.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MultiValueMap}.
 *
 * @author Mihai Dumitrescu
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 */
class MultiValueMapTests {

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	void add(MultiValueMap<String, String> map) {
		int initialSize = map.size();
		map.add("key", "value1");
		map.add("key", "value2");
		assertThat(map).hasSize(initialSize + 1);
		assertThat(map.get("key")).containsExactly("value1", "value2");
	}

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	void addIfAbsentWhenAbsent(MultiValueMap<String, String> map) {
		map.addIfAbsent("key", "value1");
		assertThat(map.get("key")).containsExactly("value1");
	}

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	void addIfAbsentWhenPresent(MultiValueMap<String, String> map) {
		map.add("key", "value1");
		map.addIfAbsent("key", "value2");
		assertThat(map.get("key")).containsExactly("value1");
	}

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	void set(MultiValueMap<String, String> map) {
		map.set("key", "value1");
		map.set("key", "value2");
		assertThat(map.get("key")).containsExactly("value2");
	}

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	void addAll(MultiValueMap<String, String> map) {
		int initialSize = map.size();
		map.add("key", "value1");
		map.addAll("key", Arrays.asList("value2", "value3"));
		assertThat(map).hasSize(initialSize + 1);
		assertThat(map.get("key")).containsExactly("value1", "value2", "value3");
	}

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	void addAllWithEmptyList(MultiValueMap<String, String> map) {
		int initialSize = map.size();
		map.addAll("key", Collections.emptyList());
		assertThat(map).hasSize(initialSize + 1);
		assertThat(map.get("key")).isEmpty();
		assertThat(map.getFirst("key")).isNull();
	}

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	void getFirst(MultiValueMap<String, String> map) {
		List<String> values = List.of("value1", "value2");
		map.put("key", values);
		assertThat(map.getFirst("key")).isEqualTo("value1");
		assertThat(map.getFirst("other")).isNull();
	}

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	void toSingleValueMap(MultiValueMap<String, String> map) {
		int initialSize = map.size();
		List<String> values = List.of("value1", "value2");
		map.put("key", values);
		Map<String, String> singleValueMap = map.toSingleValueMap();
		assertThat(singleValueMap).hasSize(initialSize + 1);
		assertThat(singleValueMap.get("key")).isEqualTo("value1");
	}

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	void toSingleValueMapWithEmptyList(MultiValueMap<String, String> map) {
		int initialSize = map.size();
		map.put("key", Collections.emptyList());
		Map<String, String> singleValueMap = map.toSingleValueMap();
		assertThat(singleValueMap).hasSize(initialSize);
		assertThat(singleValueMap.get("key")).isNull();
	}

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	void equalsOnExistingValues(MultiValueMap<String, String> map) {
		map.clear();
		map.set("key1", "value1");
		assertThat(map).isEqualTo(map);
	}

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	void equalsOnEmpty(MultiValueMap<String, String> map) {
		map.clear();
		map.set("key1", "value1");
		MultiValueMap<String, String> o1 = new LinkedMultiValueMap<>();
		o1.set("key1", "value1");
		assertThat(o1).isEqualTo(map);
		assertThat(map).isEqualTo(o1);
		Map<String, List<String>> o2 = new HashMap<>();
		o2.put("key1", Collections.singletonList("value1"));
		assertThat(o2).isEqualTo(map);
		assertThat(map).isEqualTo(o2);
	}

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	void canNotChangeAnUnmodifiableMultiValueMap(MultiValueMap<String, String> map) {
		MultiValueMap<String, String> asUnmodifiableMultiValueMap = CollectionUtils.unmodifiableMultiValueMap(map);
		Assertions.assertAll(
				() -> Assertions.assertThrows(UnsupportedOperationException.class,
						() -> asUnmodifiableMultiValueMap.add("key", "value")),
				() -> Assertions.assertThrows(UnsupportedOperationException.class,
						() -> asUnmodifiableMultiValueMap.addIfAbsent("key", "value")),
				() -> Assertions.assertThrows(UnsupportedOperationException.class,
						() -> asUnmodifiableMultiValueMap.addAll("key", exampleListOfValues())),
				() -> Assertions.assertThrows(UnsupportedOperationException.class,
						() -> asUnmodifiableMultiValueMap.addAll(exampleMultiValueMap())),
				() -> Assertions.assertThrows(UnsupportedOperationException.class,
						() -> asUnmodifiableMultiValueMap.set("key", "value")),
				() -> Assertions.assertThrows(UnsupportedOperationException.class,
						() -> asUnmodifiableMultiValueMap.setAll(exampleHashMap())),
				() -> Assertions.assertThrows(UnsupportedOperationException.class,
						() -> asUnmodifiableMultiValueMap.put("key", exampleListOfValues())),
				() -> Assertions.assertThrows(UnsupportedOperationException.class,
						() -> asUnmodifiableMultiValueMap.putIfAbsent("key", exampleListOfValues())),
				() -> Assertions.assertThrows(UnsupportedOperationException.class,
						() -> asUnmodifiableMultiValueMap.putAll(exampleMultiValueMap())),
				() -> Assertions.assertThrows(UnsupportedOperationException.class,
						() -> asUnmodifiableMultiValueMap.remove("key1")));

	}

	private List<String> exampleListOfValues() {
		return List.of("value1", "value2");
	}

	private Map<String, String> exampleHashMap() {
		return Map.of("key2", "key2.value1");
	}

	private MultiValueMap<String, String> exampleMultiValueMap() {
		LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.put("key1", Arrays.asList("key1.value1", "key1.value2"));
		return map;
	}

	static Stream<MultiValueMap<String, String>> objectsUnderTest() {
		return Stream.of(new LinkedMultiValueMap<>(), new LinkedMultiValueMap<>(new HashMap<>()),
				new LinkedMultiValueMap<>(new LinkedHashMap<>()),
				new LinkedMultiValueMap<>(Map.of("existingkey", Arrays.asList("existingvalue1", "existingvalue2"))),
				CollectionUtils.toMultiValueMap(new HashMap<>()));
	}

}
