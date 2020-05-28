/*
 * Copyright 2002-2020 the original author or authors.
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

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mihai Dumitrescu
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 */
class MultiValueMapRelatedTests {

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	void add(MultiValueMap<String, String> objectUnderTest) {
		int startingSize = objectUnderTest.size();
		objectUnderTest.add("key", "value1");
		objectUnderTest.add("key", "value2");
		assertThat(objectUnderTest).hasSize(startingSize + 1);
		assertThat(objectUnderTest.get("key")).containsExactly("value1", "value2");
	}

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	void addIfAbsentWhenAbsent(MultiValueMap<String, String> objectUnderTest) {
		objectUnderTest.addIfAbsent("key", "value1");
		assertThat(objectUnderTest.get("key")).containsExactly("value1");
	}

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	void addIfAbsentWhenPresent(MultiValueMap<String, String> objectUnderTest) {
		objectUnderTest.add("key", "value1");
		objectUnderTest.addIfAbsent("key", "value2");
		assertThat(objectUnderTest.get("key")).containsExactly("value1");
	}

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	void set(MultiValueMap<String, String> objectUnderTest) {
		objectUnderTest.set("key", "value1");
		objectUnderTest.set("key", "value2");
		assertThat(objectUnderTest.get("key")).containsExactly("value2");
	}

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	void addAll(MultiValueMap<String, String> objectUnderTest) {
		int  startingSize = objectUnderTest.size();

		objectUnderTest.add("key", "value1");
		objectUnderTest.addAll("key", Arrays.asList("value2", "value3"));
		assertThat(objectUnderTest).hasSize(startingSize + 1);
		assertThat(objectUnderTest.get("key")).containsExactly("value1", "value2", "value3");
	}

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	@Disabled("to be fixed in gh-25140")
	void addAllWithEmptyList(MultiValueMap<String, String> objectUnderTest) {
		objectUnderTest.addAll("key", Collections.emptyList());
		assertThat(objectUnderTest).hasSize(1);
		assertThat(objectUnderTest.get("key")).isEmpty();
		assertThat(objectUnderTest.getFirst("key")).isNull();
	}

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	void getFirst(MultiValueMap<String, String> objectUnderTest) {
		List<String> values = new ArrayList<>(2);
		values.add("value1");
		values.add("value2");
		objectUnderTest.put("key", values);
		assertThat(objectUnderTest.getFirst("key")).isEqualTo("value1");
		assertThat(objectUnderTest.getFirst("other")).isNull();
	}

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	void toSingleValueMap(MultiValueMap<String, String> objectUnderTest) {

		int startingSize = objectUnderTest.size();

		List<String> values = new ArrayList<>(2);
		values.add("value1");
		values.add("value2");
		objectUnderTest.put("key", values);
		Map<String, String> singleValueMap = objectUnderTest.toSingleValueMap();
		assertThat(singleValueMap).hasSize(startingSize + 1);
		assertThat(singleValueMap.get("key")).isEqualTo("value1");
	}

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	@Disabled("to be fixed in gh-25140")
	void toSingleValueMapWithEmptyList(MultiValueMap<String, String> objectUnderTest) {
		objectUnderTest.put("key", Collections.emptyList());
		Map<String, String> singleValueMap = objectUnderTest.toSingleValueMap();
		assertThat(singleValueMap).isEmpty();
		assertThat(singleValueMap.get("key")).isNull();
	}

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	void equalsOnExistingValues(MultiValueMap<String, String> objectUnderTest) {
		objectUnderTest.clear();
		objectUnderTest.set("key1", "value1");
		assertThat(objectUnderTest).isEqualTo(objectUnderTest);
	}

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	void equalsOnEmpty(MultiValueMap<String, String> objectUnderTest) {
		objectUnderTest.clear();
		objectUnderTest.set("key1", "value1");
		MultiValueMap<String, String> o1 = new LinkedMultiValueMap<>();
		o1.set("key1", "value1");
		assertThat(o1).isEqualTo(objectUnderTest);
		assertThat(objectUnderTest).isEqualTo(o1);
		Map<String, List<String>> o2 = new HashMap<>();
		o2.put("key1", Collections.singletonList("value1"));
		assertThat(o2).isEqualTo(objectUnderTest);
		assertThat(objectUnderTest).isEqualTo(o2);
	}

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	void canNotChangeAnUnmodifiableMultiValueMap(MultiValueMap<String, String> objectUnderTest) {
		MultiValueMap<String, String> asUnmodifiableMultiValueMap = CollectionUtils.unmodifiableMultiValueMap(objectUnderTest);
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
						() -> asUnmodifiableMultiValueMap.remove("key1"))
		);

	}

	@NotNull
	private List<String> exampleListOfValues() {
		return Arrays.asList("value1", "value2");
	}

	@NotNull
	private HashMap<String, String> exampleHashMap() {
		return new HashMap<String, String>() {{
			put("key2", "key2.value1");
		}};
	}

	private MultiValueMap<String, String> exampleMultiValueMap() {
		return new LinkedMultiValueMap<String, String>() {{
			put("key1", Arrays.asList("key1.value1", "key1.value2"));
		}};
	}

	static Stream<MultiValueMap<String, String>> objectsUnderTest() {
		return Stream.of(
				new LinkedMultiValueMap<>(),
				new LinkedMultiValueMap<>(new HashMap<>()),
				new LinkedMultiValueMap<>(new LinkedHashMap<>()),
				new LinkedMultiValueMap<>(new HashMap<String, List<String>>(){{
					put("existingkey", Arrays.asList("existingvalue1", "existingvalue2"));
				}}),
				CollectionUtils.toMultiValueMap(new HashMap<>()));
	}

}
