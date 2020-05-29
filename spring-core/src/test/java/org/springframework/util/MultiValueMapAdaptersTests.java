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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mihai Dumitrescu
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 */
class MultiValueMapAdaptersTests {

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
		int startingSize = objectUnderTest.size();

		objectUnderTest.add("key", "value1");
		objectUnderTest.addAll("key", Arrays.asList("value2", "value3"));
		assertThat(objectUnderTest).hasSize(startingSize + 1);
		assertThat(objectUnderTest.get("key")).containsExactly("value1", "value2", "value3");
	}

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	void addAllWithEmptyList(MultiValueMap<String, String> objectUnderTest) {
		int startingSize = objectUnderTest.size();

		objectUnderTest.addAll("key", Collections.emptyList());
		assertThat(objectUnderTest).hasSize(startingSize + 1);
		assertThat(objectUnderTest.get("key")).isEmpty();
		assertThat(objectUnderTest.getFirst("key")).isNull();
	}

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	void getFirst(MultiValueMap<String, String> objectUnderTest) {
		objectUnderTest.put("key", Arrays.asList("value1", "value2"));
		assertThat(objectUnderTest.getFirst("key")).isEqualTo("value1");
		assertThat(objectUnderTest.getFirst("other")).isNull();
	}

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	void toSingleValueMap(MultiValueMap<String, String> objectUnderTest) {

		int startingSize = objectUnderTest.size();

		objectUnderTest.put("key", Arrays.asList("value1", "value2"));
		Map<String, String> singleValueMap = objectUnderTest.toSingleValueMap();
		assertThat(singleValueMap).hasSize(startingSize + 1);
		assertThat(singleValueMap.get("key")).isEqualTo("value1");
	}

	@ParameterizedTest
	@MethodSource("emptyObjectsUnderTest")
	void toSingleValueMapWithEmptyList(MultiValueMap<String, String> objectUnderTest) {
		objectUnderTest.put("key", Collections.emptyList());
		Map<String, String> singleValueMap = objectUnderTest.toSingleValueMap();
		assertThat(singleValueMap).isEmpty();
		assertThat(singleValueMap.get("key")).isNull();
	}

	@ParameterizedTest
	@MethodSource("emptyObjectsUnderTest")
	void equalsOnExistingValues(MultiValueMap<String, String> objectUnderTest) {
		objectUnderTest.set("key1", "value1");
		assertThat(objectUnderTest).isEqualTo(objectUnderTest);
	}

	@ParameterizedTest
	@MethodSource("emptyObjectsUnderTest")
	void equalsOnEmpty(MultiValueMap<String, String> objectUnderTest) {
		objectUnderTest.set("key1", "value1");
		MultiValueMap<String, String> o1 = new LinkedMultiValueMap<>();
		o1.set("key1", "value1");
		assertThat(o1).isEqualTo(objectUnderTest);
		assertThat(objectUnderTest).isEqualTo(o1);
		Map<String, List<String>> o2 = Collections.singletonMap("key1", Collections.singletonList("value1"));
		assertThat(o2).isEqualTo(objectUnderTest);
		assertThat(objectUnderTest).isEqualTo(o2);
	}

	@ParameterizedTest
	@MethodSource("objectsUnderTest")
	void canNotChangeAnUnmodifiableMultiValueMap(MultiValueMap<String, String> objectUnderTest) {
		MultiValueMap<String, String> asUnmodifiableMultiValueMap = CollectionUtils.unmodifiableMultiValueMap(objectUnderTest);

		SoftAssertions bundle = new SoftAssertions();

		bundle.assertThatThrownBy(() -> asUnmodifiableMultiValueMap.add("key", "value"))
				.isExactlyInstanceOf(UnsupportedOperationException.class);
		bundle.assertThatThrownBy(() -> asUnmodifiableMultiValueMap.addIfAbsent("key", "value"))
				.isExactlyInstanceOf(UnsupportedOperationException.class);
		bundle.assertThatThrownBy(() -> asUnmodifiableMultiValueMap.addAll("key", Arrays.asList("value1", "value2")))
				.isExactlyInstanceOf(UnsupportedOperationException.class);
		bundle.assertThatThrownBy(() -> asUnmodifiableMultiValueMap.addAll(
				new LinkedMultiValueMap<String, String>() {{
					put("key1", Arrays.asList("key1.value1", "key1.value2"));
				}})).isExactlyInstanceOf(UnsupportedOperationException.class);
		bundle.assertThatThrownBy(() -> asUnmodifiableMultiValueMap.set("key", "value"))
				.isExactlyInstanceOf(UnsupportedOperationException.class);
		bundle.assertThatThrownBy(() -> asUnmodifiableMultiValueMap.setAll(Collections.singletonMap("key2", "key2.value")))
				.isExactlyInstanceOf(UnsupportedOperationException.class);
		bundle.assertThatThrownBy(() -> asUnmodifiableMultiValueMap.put("key", Arrays.asList("value1", "value2")))
				.isExactlyInstanceOf(UnsupportedOperationException.class);
		bundle.assertThatThrownBy(() -> asUnmodifiableMultiValueMap.putIfAbsent("key", Arrays.asList("value1", "value2")))
				.isExactlyInstanceOf(UnsupportedOperationException.class);
		bundle.assertThatThrownBy(() -> asUnmodifiableMultiValueMap.putAll(Collections.singletonMap("key", Arrays.asList("value1", "value2"))))
				.isExactlyInstanceOf(UnsupportedOperationException.class);
		bundle.assertThatThrownBy(() -> asUnmodifiableMultiValueMap.remove("key1"))
				.isExactlyInstanceOf(UnsupportedOperationException.class);

		bundle.assertAll();
	}

	static Stream<MultiValueMap<String, String>> objectsUnderTest() {
		HashMap<String, List<String>> wrappedHashMap = new HashMap<String, List<String>>() {{
			put("existingkey", Arrays.asList("existingvalue1", "existingvalue2"));
		}};

		return Stream.concat(
				emptyObjectsUnderTest(),
				Stream.of(
						new LinkedMultiValueMap<>(wrappedHashMap),
						CollectionUtils.toMultiValueMap(wrappedHashMap)
				));
	}

	static Stream<MultiValueMap<String, String>> emptyObjectsUnderTest() {
		return Stream.of(
				new LinkedMultiValueMap<>(),
				new LinkedMultiValueMap<>(new HashMap<>()),
				new LinkedMultiValueMap<>(new LinkedHashMap<>()),
				CollectionUtils.toMultiValueMap(new HashMap<>()));
	}
}
