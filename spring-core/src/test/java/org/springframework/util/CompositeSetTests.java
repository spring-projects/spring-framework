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
class CompositeSetTests {

	@Test
	void equals() {
		Set<String> first = Set.of("foo", "bar");
		Set<String> second = Set.of("baz", "qux");
		CompositeSet<String> composite = new CompositeSet<>(first, second);

		Set<String> all = new HashSet<>(first);
		all.addAll(second);

		assertThat(composite).isEqualTo(all);
		assertThat(composite).isNotEqualTo(first);
		assertThat(composite).isNotEqualTo(second);
		assertThat(composite).isNotEqualTo(Collections.emptySet());
	}

	@Test
	void nullable() {
		Set<@Nullable String> first = new HashSet<>();
		first.add("foo");
		first.add(null);
		Set<@Nullable String> second = new HashSet<>();
		second.add("bar");
		first.add(null);
		CompositeSet<@Nullable String> composite = new CompositeSet<>(first, second);

		assertThat(composite).containsExactlyInAnyOrder("foo", null, "bar");
	}
}
