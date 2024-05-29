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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;

/**
 * @author Arjen Poutsma
 */
class SingleToMultiValueMapAdapterTests {


	private Map<String, String> delegate;

	private MultiValueMap<String, String> adapter;

	@BeforeEach
	void setUp() {
		this.delegate = new LinkedHashMap<>();
		this.delegate.put("foo", "bar");
		this.delegate.put("qux", "quux");

		this.adapter = new SingleToMultiValueMapAdapter<>(this.delegate);
	}

	@Test
	void getFirst() {
		assertThat(this.adapter.getFirst("foo")).isEqualTo("bar");
		assertThat(this.adapter.getFirst("qux")).isEqualTo("quux");
		assertThat(this.adapter.getFirst("corge")).isNull();
	}

	@Test
	void add() {
		this.adapter.add("corge", "grault");
		assertThat(this.adapter.getFirst("corge")).isEqualTo("grault");
		assertThat(this.delegate.get("corge")).isEqualTo("grault");

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				this.adapter.add("foo", "garply"));
	}

	@Test
	void addAll() {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("corge", "grault");
		this.adapter.addAll(map);

		assertThat(this.adapter.getFirst("corge")).isEqualTo("grault");
		assertThat(this.delegate.get("corge")).isEqualTo("grault");

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				this.adapter.addAll(map));
	}

	@Test
	void set() {
		this.adapter.set("foo", "baz");
		assertThat(this.delegate.get("foo")).isEqualTo("baz");
	}

	@Test
	void setAll() {
		this.adapter.setAll(Map.of("foo", "baz"));
		assertThat(this.delegate.get("foo")).isEqualTo("baz");
	}

	@Test
	void size() {
		assertThat(this.adapter.size()).isEqualTo(this.delegate.size()).isEqualTo(2);
	}

	@Test
	void isEmpty() {
		assertThat(this.adapter.isEmpty()).isFalse();

		this.adapter = new SingleToMultiValueMapAdapter<>(Collections.emptyMap());
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
		assertThat(this.adapter.containsValue(List.of("bar"))).isTrue();
		assertThat(this.adapter.containsValue(List.of("quux"))).isTrue();
		assertThat(this.adapter.containsValue(List.of("corge"))).isFalse();
	}

	@Test
	void get() {
		assertThat(this.adapter.get("foo")).isEqualTo(List.of("bar"));
		assertThat(this.adapter.get("qux")).isEqualTo(List.of("quux"));
		assertThat(this.adapter.get("corge")).isNull();
	}

	@Test
	void put() {
		assertThat(this.adapter.put("foo", List.of("baz"))).containsExactly("bar");
		assertThat(this.adapter.put("qux", Collections.emptyList())).containsExactly("quux");
		assertThat(this.adapter.put("grault", List.of("garply"))).isNull();

		assertThat(this.delegate).containsExactly(entry("foo", "baz"), entry("qux", null), entry("grault", "garply"));

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				this.adapter.put("foo", List.of("bar", "baz")));
	}

	@Test
	void remove() {
		assertThat(this.adapter.remove("foo")).isEqualTo(List.of("bar"));
		assertThat(this.adapter.containsKey("foo")).isFalse();
		assertThat(this.delegate.containsKey("foo")).isFalse();
	}

	@Test
	void putAll() {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("foo", "baz");
		map.add("qux", null);
		map.add("grault", "garply");
		this.adapter.putAll(map);

		assertThat(this.delegate).containsExactly(entry("foo", "baz"), entry("qux", null), entry("grault", "garply"));
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
		assertThat(this.adapter.values()).containsExactly(List.of("bar"), List.of("quux"));
	}

	@Test
	void entrySet() {
		assertThat(this.adapter.entrySet()).containsExactly(entry("foo", List.of("bar")), entry("qux", List.of("quux")));
	}

	@Test
	void forEach() {
		MultiValueMap<String, String> seen = new LinkedMultiValueMap<>();
		this.adapter.forEach(seen::put);
		assertThat(seen).containsExactly(entry("foo", List.of("bar")), entry("qux", List.of("quux")));
	}
}
