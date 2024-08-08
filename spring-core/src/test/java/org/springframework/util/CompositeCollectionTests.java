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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Arjen Poutsma
 */
class CompositeCollectionTests {

	@Test
	void size() {
		List<String> first = List.of("foo", "bar", "baz");
		List<String> second = List.of("qux", "quux");
		CompositeCollection<String> composite = new CompositeCollection<>(first, second);

		assertThat(composite).hasSize(5);
	}

	@Test
	void isEmpty() {
		List<String> first = List.of("foo", "bar", "baz");
		List<String> second = List.of("qux", "quux");
		CompositeCollection<String> composite = new CompositeCollection<>(first, second);

		assertThat(composite).isNotEmpty();

		composite = new CompositeCollection<>(Collections.emptyList(), Collections.emptyList());
		assertThat(composite).isEmpty();
	}

	@Test
	void contains() {
		List<String> first = List.of("foo", "bar");
		List<String> second = List.of("baz", "qux");
		CompositeCollection<String> composite = new CompositeCollection<>(first, second);

		assertThat(composite.contains("foo")).isTrue();
		assertThat(composite.contains("bar")).isTrue();
		assertThat(composite.contains("baz")).isTrue();
		assertThat(composite.contains("qux")).isTrue();
		assertThat(composite.contains("quux")).isFalse();
	}

	@Test
	void iterator() {
		List<String> first = List.of("foo", "bar");
		List<String> second = List.of("baz", "qux");
		CompositeCollection<String> composite = new CompositeCollection<>(first, second);

		Iterator<String> iterator = composite.iterator();
		assertThat(iterator).hasNext();
		assertThat(iterator.next()).isEqualTo("foo");
		assertThat(iterator).hasNext();
		assertThat(iterator.next()).isEqualTo("bar");
		assertThat(iterator).hasNext();
		assertThat(iterator.next()).isEqualTo("baz");
		assertThat(iterator).hasNext();
		assertThat(iterator.next()).isEqualTo("qux");
		assertThat(iterator).isExhausted();
	}

	@Test
	void toArray() {
		List<String> first = List.of("foo", "bar");
		List<String> second = List.of("baz", "qux");
		CompositeCollection<String> composite = new CompositeCollection<>(first, second);

		Object[] array = composite.toArray();
		assertThat(array).containsExactly("foo", "bar", "baz", "qux");
	}

	@Test
	void toArrayArgs() {
		List<String> first = List.of("foo", "bar");
		List<String> second = List.of("baz", "qux");
		CompositeCollection<String> composite = new CompositeCollection<>(first, second);

		String[] array = new String[composite.size()];
		array = composite.toArray(array);
		assertThat(array).containsExactly("foo", "bar", "baz", "qux");
	}

	@Test
	void add() {
		List<String> first = List.of("foo", "bar");
		List<String> second = List.of("baz", "qux");
		CompositeCollection<String> composite = new CompositeCollection<>(first, second);

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> composite.add("quux"));
	}

	@Test
	void remove() {
		List<String> first = new ArrayList<>(List.of("foo", "bar"));
		List<String> second = new ArrayList<>(List.of("baz", "qux"));
		CompositeCollection<String> composite = new CompositeCollection<>(first, second);

		assertThat(composite.remove("foo")).isTrue();
		assertThat(composite.contains("foo")).isFalse();
		assertThat(first).containsExactly("bar");

		assertThat(composite.remove("quux")).isFalse();
	}

	@Test
	void containsAll() {
		List<String> first = List.of("foo", "bar");
		List<String> second = List.of("baz", "qux");
		CompositeCollection<String> composite = new CompositeCollection<>(first, second);

		List<String> all = new ArrayList<>(first);
		all.addAll(second);

		assertThat(composite.containsAll(all)).isTrue();

		all.add("quux");

		assertThat(composite.containsAll(all)).isFalse();
	}

	@Test
	void addAll() {
		List<String> first = List.of("foo", "bar");
		List<String> second = List.of("baz", "qux");
		CompositeCollection<String> composite = new CompositeCollection<>(first, second);

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> composite.addAll(List.of("quux", "corge")));
	}

	@Test
	void removeAll() {
		List<String> first = new ArrayList<>(List.of("foo", "bar"));
		List<String> second = new ArrayList<>(List.of("baz", "qux"));
		CompositeCollection<String> composite = new CompositeCollection<>(first, second);

		List<String> all = new ArrayList<>(first);
		all.addAll(second);

		assertThat(composite.removeAll(all)).isTrue();

		assertThat(composite).isEmpty();
		assertThat(first).isEmpty();
		assertThat(second).isEmpty();
	}

	@Test
	void retainAll() {
		List<String> first = new ArrayList<>(List.of("foo", "bar"));
		List<String> second = new ArrayList<>(List.of("baz", "qux"));
		CompositeCollection<String> composite = new CompositeCollection<>(first, second);

		assertThat(composite.retainAll(List.of("bar", "baz"))).isTrue();

		assertThat(composite).containsExactly("bar", "baz");
		assertThat(first).containsExactly("bar");
		assertThat(second).containsExactly("baz");
	}

	@Test
	void clear() {
		List<String> first = new ArrayList<>(List.of("foo", "bar"));
		List<String> second = new ArrayList<>(List.of("baz", "qux"));
		CompositeCollection<String> composite = new CompositeCollection<>(first, second);

		composite.clear();

		assertThat(composite).isEmpty();
		assertThat(first).isEmpty();
		assertThat(second).isEmpty();
	}
}
