/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.util.comparator;

import java.util.Comparator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ComparableComparator}.
 *
 * @author Keith Donald
 * @author Chris Beams
 * @author Phillip Webb
 */
class ComparableComparatorTests {

	@Test
	void comparableComparator() {
		Comparator<String> c = new ComparableComparator<>();
		String s1 = "abc";
		String s2 = "cde";
		assertThat(c.compare(s1, s2)).isLessThan(0);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	void shouldNeedComparable() {
		Comparator c = new ComparableComparator();
		Object o1 = new Object();
		Object o2 = new Object();
		assertThatExceptionOfType(ClassCastException.class).isThrownBy(() ->
				c.compare(o1, o2));
	}

}
