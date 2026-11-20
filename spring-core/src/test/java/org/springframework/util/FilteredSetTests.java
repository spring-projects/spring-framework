/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Yanming Zhou
 */
class FilteredSetTests {

	@Test
	void equals() {
		Set<String> set = Set.of("foo", "bar", "baz");
		FilteredSet<String> filtered = new FilteredSet<>(set, s -> !s.equals("bar"));

		Set<String> expected = Set.of("foo", "baz");

		assertThat(filtered).isEqualTo(expected);
		assertThat(filtered).isNotEqualTo(set);
		assertThat(filtered).isNotEqualTo(Collections.emptySet());
	}

	@Test
	void nullable() {
		Set<@Nullable String> set = new HashSet<>();
		set.add("foo");
		set.add("bar");
		set.add(null);
		FilteredSet<@Nullable String> filtered = new FilteredSet<>(set, s -> !"bar".equals(s));

		assertThat(filtered).containsExactlyInAnyOrder("foo", null);
	}
}
