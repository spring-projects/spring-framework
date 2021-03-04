/*
 * Copyright 2002-2019 the original author or authors.
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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;


/**
 * Tests for {@link InvertibleComparator}.
 *
 * @author Keith Donald
 * @author Chris Beams
 * @author Phillip Webb
 */
@Deprecated
class InvertibleComparatorTests {

	private final Comparator<Integer> comparator = new ComparableComparator<>();


	@Test
	void shouldNeedComparator() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new InvertibleComparator<>(null));
	}

	@Test
	void shouldNeedComparatorWithAscending() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new InvertibleComparator<>(null, true));
	}

	@Test
	void shouldDefaultToAscending() throws Exception {
		InvertibleComparator<Integer> invertibleComparator = new InvertibleComparator<>(comparator);
		assertThat(invertibleComparator.isAscending()).isTrue();
		assertThat(invertibleComparator.compare(1, 2)).isEqualTo(-1);
	}

	@Test
	void shouldInvert() throws Exception {
		InvertibleComparator<Integer> invertibleComparator = new InvertibleComparator<>(comparator);
		assertThat(invertibleComparator.isAscending()).isTrue();
		assertThat(invertibleComparator.compare(1, 2)).isEqualTo(-1);
		invertibleComparator.invertOrder();
		assertThat(invertibleComparator.isAscending()).isFalse();
		assertThat(invertibleComparator.compare(1, 2)).isEqualTo(1);
	}

	@Test
	void shouldCompareAscending() throws Exception {
		InvertibleComparator<Integer> invertibleComparator = new InvertibleComparator<>(comparator, true);
		assertThat(invertibleComparator.compare(1, 2)).isEqualTo(-1);
	}

	@Test
	void shouldCompareDescending() throws Exception {
		InvertibleComparator<Integer> invertibleComparator = new InvertibleComparator<>(comparator, false);
		assertThat(invertibleComparator.compare(1, 2)).isEqualTo(1);
	}

}
