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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * @author Arjen Poutsma
 */
class MultiToSingleValueMapAdapterTests {

	private LinkedMultiValueMap<String, String> delegate;

	private Map<String, String> adapter;


	@BeforeEach
	void setUp() {
		this.delegate = new LinkedMultiValueMap<>();
		this.delegate.add("foo", "bar");
		this.delegate.add("foo", "baz");
		this.delegate.add("qux", "quux");

		this.adapter = new MultiToSingleValueMapAdapter<>(this.delegate);
	}

	@Test
	void size() {
		assertThat(this.adapter.size()).isEqualTo(this.delegate.size()).isEqualTo(2);
	}

	@Test
	void isEmpty() {
		assertThat(this.adapter.isEmpty()).isFalse();

		this.adapter = new MultiToSingleValueMapAdapter<>(new LinkedMultiValueMap<>());
		assertThat(this.adapter.isEmpty()).isTrue();
	}

	@Test
	void containsKey() {
		assertThat(this.adapter.containsKey("foo")).isTrue();
		assertThat(this.adapter.containsKey("qux")).isTrue();
		assertThat(this.adapter.containsKey("corge")).isFalse();
	}

	@Test
	void containsValue() {
		assertThat(this.adapter.containsValue("bar")).isTrue();
		assertThat(this.adapter.containsValue("quux")).isTrue();
		assertThat(this.adapter.containsValue("corge")).isFalse();
	}

	@Test
	void get() {
		assertThat(this.adapter.get("foo")).isEqualTo("bar");
		assertThat(this.adapter.get("qux")).isEqualTo("quux");
		assertThat(this.adapter.get("corge")).isNull();
	}

	@Test
	void put() {
		String result = this.adapter.put("foo", "bar");
		assertThat(result).isEqualTo("bar");
		assertThat(this.delegate.get("foo")).containsExactly("bar");
	}

	@Test
	void remove() {
		this.adapter.remove("foo");
		assertThat(this.adapter.containsKey("foo")).isFalse();
		assertThat(this.delegate.containsKey("foo")).isFalse();
	}

	@Test
	void putAll() {
		LinkedHashMap<String, String> map = new LinkedHashMap<>();
		map.put("foo", "bar");
		map.put("qux", null);
		this.adapter.putAll(map);
		assertThat(this.adapter.get("foo")).isEqualTo("bar");
		assertThat(this.adapter.get("qux")).isNull();

		assertThat(this.delegate.get("foo")).isEqualTo(List.of("bar"));
		assertThat(this.adapter.get("qux")).isNull();
	}

	@Test
	void clear() {
		this.adapter.clear();
		assertThat(this.adapter).isEmpty();
		assertThat(this.delegate).isEmpty();
	}

	@Test
	void keySet() {
		assertThat(this.adapter.keySet()).containsExactly("foo", "qux");
	}

	@Test
	void values() {
		assertThat(this.adapter.values()).containsExactly("bar", "quux");
	}

	@Test
	void entrySet() {
		assertThat(this.adapter.entrySet()).containsExactly(entry("foo", "bar"), entry("qux", "quux"));
	}
}
