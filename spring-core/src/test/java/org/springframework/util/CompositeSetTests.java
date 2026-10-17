/*
 * Copyright 2002-present the original author or authors.
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
	void remove() {
		Set<String> first = new HashSet<>(Set.of("foo", "bar"));
		Set<String> second = new HashSet<>(Set.of("bar", "baz"));
		CompositeSet<String> composite = new CompositeSet<>(first, second);
		// CompositeSet does not deduplicate elements, since CompositeMap guarantees
		// disjoint sets, and since "bar" is present in both sets, it is seen twice.
		assertThat(composite).containsExactlyInAnyOrder("foo", "bar", "bar", "baz");

		assertThat(composite.remove("foo")).isTrue();
		// "bar" is still present in both sets.
		assertThat(composite).containsExactlyInAnyOrder("bar", "bar", "baz");
		assertThat(first).containsExactly("bar");
		assertThat(second).containsExactlyInAnyOrder("bar", "baz");

		assertThat(composite.remove("bar")).isTrue();
		assertThat(composite).containsExactlyInAnyOrder("baz");
		assertThat(composite.contains("bar")).isFalse();
		assertThat(first).isEmpty();
		assertThat(second).containsExactly("baz");

		assertThat(composite.remove("baz")).isTrue();
		assertThat(composite).isEmpty();

		assertThat(composite.remove("qux")).isFalse();
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
