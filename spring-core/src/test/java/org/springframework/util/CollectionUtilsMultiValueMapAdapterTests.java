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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mihai Dumitrescu
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 */
class CollectionUtilsMultiValueMapAdapterTests {

	private HashMap<String, List<String>> wrapped = new HashMap();
	private MultiValueMap<String, String> objectUnderTest = CollectionUtils.toMultiValueMap(wrapped);;

	@AfterEach
	private void clearMap() {
		objectUnderTest.clear();
		wrapped.clear();
	}

	@Test
	void add() {
		objectUnderTest.add("key", "value1");
		objectUnderTest.add("key", "value2");
		assertThat(objectUnderTest).hasSize(1);
		assertThat(objectUnderTest.get("key")).containsExactly("value1", "value2");
	}

	@Test
	void addIfAbsentWhenAbsent() {
		objectUnderTest.addIfAbsent("key", "value1");
		assertThat(objectUnderTest.get("key")).containsExactly("value1");
	}

	@Test
	void addIfAbsentWhenPresent() {
		objectUnderTest.add("key", "value1");
		objectUnderTest.addIfAbsent("key", "value2");
		assertThat(objectUnderTest.get("key")).containsExactly("value1");
	}

	@Test
	void set() {
		objectUnderTest.set("key", "value1");
		objectUnderTest.set("key", "value2");
		assertThat(objectUnderTest.get("key")).containsExactly("value2");
	}

	@Test
	void addAll() {
		objectUnderTest.add("key", "value1");
		objectUnderTest.addAll("key", Arrays.asList("value2", "value3"));
		assertThat(objectUnderTest).hasSize(1);
		assertThat(objectUnderTest.get("key")).containsExactly("value1","value2","value3");
	}

	@Test
	@Disabled("to be fixed in gh-25140")
	void addAllWithEmptyList() {
		objectUnderTest.addAll("key", Collections.emptyList());
		assertThat(objectUnderTest).hasSize(1);
		assertThat(objectUnderTest.get("key")).isEmpty();
		assertThat(objectUnderTest.getFirst("key")).isNull();
	}

	@Test
	void getFirst() {
		List<String> values = new ArrayList<>(2);
		values.add("value1");
		values.add("value2");
		objectUnderTest.put("key", values);
		assertThat(objectUnderTest.getFirst("key")).isEqualTo("value1");
		assertThat(objectUnderTest.getFirst("other")).isNull();
	}


	@Test
	void toSingleValueMap() {
		List<String> values = new ArrayList<>(2);
		values.add("value1");
		values.add("value2");
		objectUnderTest.put("key", values);
		Map<String, String> singleValueMap = objectUnderTest.toSingleValueMap();
		assertThat(singleValueMap).hasSize(1);
		assertThat(singleValueMap.get("key")).isEqualTo("value1");
	}

	@Test
	@Disabled("to be fixed in gh-25140")
	void toSingleValueMapWithEmptyList() {
		objectUnderTest.put("key", Collections.emptyList());
		Map<String, String> singleValueMap = objectUnderTest.toSingleValueMap();
		assertThat(singleValueMap).isEmpty();
		assertThat(singleValueMap.get("key")).isNull();
	}

	@Test
	void equals() {
		objectUnderTest.set("key1", "value1");
		assertThat(objectUnderTest).isEqualTo(objectUnderTest);
		MultiValueMap<String, String> o1 = new LinkedMultiValueMap<>();
		o1.set("key1", "value1");
		assertThat(o1).isEqualTo(objectUnderTest);
		assertThat(objectUnderTest).isEqualTo(o1);
		Map<String, List<String>> o2 = new HashMap<>();
		o2.put("key1", Collections.singletonList("value1"));
		assertThat(o2).isEqualTo(objectUnderTest);
		assertThat(objectUnderTest).isEqualTo(o2);
	}


	@Test
	void conversionOfEmptyMap() {
		MultiValueMap<String, String> asMultiValueMap = CollectionUtils.toMultiValueMap(wrapped);
		assertThat(asMultiValueMap.isEmpty()).isTrue();
		assertThat(asMultiValueMap).isEmpty();
	}

	@Test
	void conversionOfNonEmptyMap() {
		wrapped.put("key", Arrays.asList("first", "second"));
		MultiValueMap<String, String> asMultiValueMap = CollectionUtils.toMultiValueMap(wrapped);
		assertThat(asMultiValueMap).containsAllEntriesOf(wrapped);
	}

	@Test
	void changeByReference() {
		MultiValueMap<String, String> asMultiValueMap = CollectionUtils.toMultiValueMap(wrapped);
		assertThat(asMultiValueMap).doesNotContainKeys("secondKey");
		wrapped.put("secondKey", new ArrayList<>());

		assertThat(asMultiValueMap).containsKey("secondKey");
	}

	@Test
	void canNotChangeAnUnmodifiableMultiValueMap() {
		MultiValueMap<String, String> asUnmodifiableMultiValueMap = CollectionUtils.unmodifiableMultiValueMap(CollectionUtils.toMultiValueMap(wrapped));
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
						() -> asUnmodifiableMultiValueMap.putAll(exampleMultiValueMap()))
		);

	}

	@NotNull
	private List<String> exampleListOfValues() {
		return Arrays.asList("value1", "value2");
	}

	@NotNull
	private HashMap<String, String> exampleHashMap() {
		return new HashMap<String, String>(){{
			put("key2", "key2.value1");
		}};
	}

	private MultiValueMap<String, String> exampleMultiValueMap() {
		return new LinkedMultiValueMap<String, String>(){{
			put("key1", Arrays.asList("key1.value1", "key1.value2"));
		}};
	}

}
