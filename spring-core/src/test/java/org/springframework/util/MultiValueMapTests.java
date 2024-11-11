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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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

	@ParameterizedMultiValueMapTest
	void add(MultiValueMap<String, String> map) {
		int initialSize = map.size();
		map.add("key", "value1");
		map.add("key", "value2");
		assertThat(map).hasSize(initialSize + 1);
		assertThat(map.get("key")).containsExactly("value1", "value2");
	}

	@ParameterizedMultiValueMapTest
	void addIfAbsentWhenAbsent(MultiValueMap<String, String> map) {
		map.addIfAbsent("key", "value1");
		assertThat(map.get("key")).containsExactly("value1");
	}

	@ParameterizedMultiValueMapTest
	void addIfAbsentWhenPresent(MultiValueMap<String, String> map) {
		map.add("key", "value1");
		map.addIfAbsent("key", "value2");
		assertThat(map.get("key")).containsExactly("value1");
	}

	@ParameterizedMultiValueMapTest
	void set(MultiValueMap<String, String> map) {
		map.set("key", "value1");
		map.set("key", "value2");
		assertThat(map.get("key")).containsExactly("value2");
	}

	@ParameterizedMultiValueMapTest
	void addAll(MultiValueMap<String, String> map) {
		int initialSize = map.size();
		map.add("key", "value1");
		map.addAll("key", Arrays.asList("value2", "value3"));
		assertThat(map).hasSize(initialSize + 1);
		assertThat(map.get("key")).containsExactly("value1", "value2", "value3");
	}

	@ParameterizedMultiValueMapTest
	void addAllWithEmptyList(MultiValueMap<String, String> map) {
		int initialSize = map.size();
		map.addAll("key", List.of());
		assertThat(map).hasSize(initialSize + 1);
		assertThat(map.get("key")).isEmpty();
		assertThat(map.getFirst("key")).isNull();
	}

	@ParameterizedMultiValueMapTest
	void getFirst(MultiValueMap<String, String> map) {
		List<String> values = List.of("value1", "value2");
		map.put("key", values);
		assertThat(map.getFirst("key")).isEqualTo("value1");
		assertThat(map.getFirst("other")).isNull();
	}

	@ParameterizedMultiValueMapTest
	void toSingleValueMap(MultiValueMap<String, String> map) {
		int initialSize = map.size();
		List<String> values = List.of("value1", "value2");
		map.put("key", values);
		Map<String, String> singleValueMap = map.toSingleValueMap();
		assertThat(singleValueMap).hasSize(initialSize + 1);
		assertThat(singleValueMap.get("key")).isEqualTo("value1");
	}

	@ParameterizedMultiValueMapTest
	void toSingleValueMapWithEmptyList(MultiValueMap<String, String> map) {
		int initialSize = map.size();
		map.put("key", List.of());
		Map<String, String> singleValueMap = map.toSingleValueMap();
		assertThat(singleValueMap).hasSize(initialSize);
		assertThat(singleValueMap.get("key")).isNull();
	}

	@ParameterizedMultiValueMapTest
	void equalsOnExistingValues(MultiValueMap<String, String> map) {
		map.clear();
		map.set("key1", "value1");
		assertThat(map).isEqualTo(map);
	}

	@ParameterizedMultiValueMapTest
	void equalsOnEmpty(MultiValueMap<String, String> map) {
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


	@Retention(RetentionPolicy.RUNTIME)
	@ParameterizedTest
	@MethodSource("mapsUnderTest")
	@interface ParameterizedMultiValueMapTest {
	}

	static Stream<Arguments> mapsUnderTest() {
		return Stream.of(
				argumentSet("new LinkedMultiValueMap<>()", new LinkedMultiValueMap<>()),
				argumentSet("new LinkedMultiValueMap<>(new HashMap<>())", new LinkedMultiValueMap<>(new HashMap<>())),
				argumentSet("new LinkedMultiValueMap<>(new LinkedHashMap<>())", new LinkedMultiValueMap<>(new LinkedHashMap<>())),
				argumentSet("new LinkedMultiValueMap<>(Map.of(...))", new LinkedMultiValueMap<>(Map.of("existingkey", List.of("existingvalue1", "existingvalue2")))),
				argumentSet("CollectionUtils.toMultiValueMap", CollectionUtils.toMultiValueMap(new HashMap<>()))
			);
	}

}
