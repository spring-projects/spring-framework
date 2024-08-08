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
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class CompositeSetTests {

	@Test
	void testEquals() {
		Set<String> first = Set.of("foo", "bar");
		Set<String> second = Set.of("baz", "qux");
		CompositeSet<String> composite = new CompositeSet<>(first, second);

		Set<String> all = new HashSet<>(first);
		all.addAll(second);

		assertThat(composite.equals(all)).isTrue();
		assertThat(composite.equals(first)).isFalse();
		assertThat(composite.equals(second)).isFalse();
		assertThat(composite.equals(Collections.emptySet())).isFalse();
	}

}
