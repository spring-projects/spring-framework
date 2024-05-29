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
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class FilteredCollectionTests {

	@Test
	void size() {
		List<String> list = List.of("foo", "bar", "baz");
		FilteredCollection<String> filtered = new FilteredCollection<>(list, s -> !s.equals("bar"));

		assertThat(filtered).hasSize(2);
	}

	@Test
	void iterator() {
		List<String> list = List.of("foo", "bar", "baz");
		FilteredCollection<String> filtered = new FilteredCollection<>(list, s -> !s.equals("bar"));

		assertThat(filtered.iterator()).toIterable().containsExactly("foo", "baz");
	}

	@Test
	void add() {
		List<String> list = new ArrayList<>(List.of("foo"));
		FilteredCollection<String> filtered = new FilteredCollection<>(list, s -> !s.equals("bar"));
		boolean added = filtered.add("bar");
		assertThat(added).isFalse();
		assertThat(filtered).containsExactly("foo");
		assertThat(list).containsExactly("foo", "bar");
	}

	@Test
	void remove() {
		List<String> list = new ArrayList<>(List.of("foo", "bar"));
		FilteredCollection<String> filtered = new FilteredCollection<>(list, s -> !s.equals("bar"));
		assertThat(list).contains("bar");
		assertThat(filtered).doesNotContain("bar");
		boolean removed = filtered.remove("bar");
		assertThat(removed).isFalse();
		assertThat(filtered).doesNotContain("bar");
		assertThat(list).doesNotContain("bar");
	}

	@Test
	void contains() {
		List<String> list = List.of("foo", "bar", "baz");
		FilteredCollection<String> filtered = new FilteredCollection<>(list, s -> !s.equals("bar"));
		boolean contained = filtered.contains("bar");
		assertThat(contained).isFalse();
	}

}
