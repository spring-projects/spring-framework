/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Yanming Zhou
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

	@Test
	void nullable() {
		List<@Nullable String> list = Arrays.asList("foo", "bar", null);
		FilteredCollection<@Nullable String> filtered = new FilteredCollection<>(list, s -> !"bar".equals(s));

		assertThat(filtered).containsExactlyInAnyOrder("foo", null);
	}
}
