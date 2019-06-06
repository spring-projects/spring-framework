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

package org.springframework.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 */
public class LinkedMultiValueMapTests {

	private final LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();


	@Test
	public void add() {
		map.add("key", "value1");
		map.add("key", "value2");
		assertThat(map.size()).isEqualTo(1);
		List<String> expected = new ArrayList<>(2);
		expected.add("value1");
		expected.add("value2");
		assertThat(map.get("key")).isEqualTo(expected);
	}

	@Test
	public void set() {
		map.set("key", "value1");
		map.set("key", "value2");
		assertThat(map.size()).isEqualTo(1);
		assertThat(map.get("key")).isEqualTo(Collections.singletonList("value2"));
	}

	@Test
	public void addAll() {
		map.add("key", "value1");
		map.addAll("key", Arrays.asList("value2", "value3"));
		assertThat(map.size()).isEqualTo(1);
		List<String> expected = new ArrayList<>(2);
		expected.add("value1");
		expected.add("value2");
		expected.add("value3");
		assertThat(map.get("key")).isEqualTo(expected);
	}

	@Test
	public void addAllWithEmptyList() {
		map.addAll("key", Collections.emptyList());
		assertThat(map.size()).isEqualTo(1);
		assertThat(map.get("key")).isEqualTo(Collections.emptyList());
		assertThat(map.getFirst("key")).isNull();
	}

	@Test
	public void getFirst() {
		List<String> values = new ArrayList<>(2);
		values.add("value1");
		values.add("value2");
		map.put("key", values);
		assertThat(map.getFirst("key")).isEqualTo("value1");
		assertThat(map.getFirst("other")).isNull();
	}

	@Test
	public void getFirstWithEmptyList() {
		map.put("key", Collections.emptyList());
		assertThat(map.getFirst("key")).isNull();
		assertThat(map.getFirst("other")).isNull();
	}

	@Test
	public void toSingleValueMap() {
		List<String> values = new ArrayList<>(2);
		values.add("value1");
		values.add("value2");
		map.put("key", values);
		Map<String, String> svm = map.toSingleValueMap();
		assertThat(svm.size()).isEqualTo(1);
		assertThat(svm.get("key")).isEqualTo("value1");
	}

	@Test
	public void toSingleValueMapWithEmptyList() {
		map.put("key", Collections.emptyList());
		Map<String, String> svm = map.toSingleValueMap();
		assertThat(svm.size()).isEqualTo(0);
		assertThat(svm.get("key")).isNull();
	}

	@Test
	public void equals() {
		map.set("key1", "value1");
		assertThat(map).isEqualTo(map);
		MultiValueMap<String, String> o1 = new LinkedMultiValueMap<>();
		o1.set("key1", "value1");
		assertThat(o1).isEqualTo(map);
		assertThat(map).isEqualTo(o1);
		Map<String, List<String>> o2 = new HashMap<>();
		o2.put("key1", Collections.singletonList("value1"));
		assertThat(o2).isEqualTo(map);
		assertThat(map).isEqualTo(o2);
	}

}
