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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Test case for {@link CompositeIterator}.
 *
 * @author Erwin Vervaet
 * @author Juergen Hoeller
 */
class CompositeIteratorTests {

	@Test
	void noIterators() {
		CompositeIterator<String> it = new CompositeIterator<>();
		assertThat(it.hasNext()).isFalse();
		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(
				it::next);
	}

	@Test
	void singleIterator() {
		CompositeIterator<String> it = new CompositeIterator<>();
		it.add(Arrays.asList("0", "1").iterator());
		for (int i = 0; i < 2; i++) {
			assertThat(it.hasNext()).isTrue();
			assertThat(it.next()).isEqualTo(String.valueOf(i));
		}
		assertThat(it.hasNext()).isFalse();
		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(
				it::next);
	}

	@Test
	void multipleIterators() {
		CompositeIterator<String> it = new CompositeIterator<>();
		it.add(Arrays.asList("0", "1").iterator());
		it.add(List.of("2").iterator());
		it.add(Arrays.asList("3", "4").iterator());
		for (int i = 0; i < 5; i++) {
			assertThat(it.hasNext()).isTrue();
			assertThat(it.next()).isEqualTo(String.valueOf(i));
		}
		assertThat(it.hasNext()).isFalse();

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(
				it::next);
	}

	@Test
	void inUse() {
		List<String> list = Arrays.asList("0", "1");
		CompositeIterator<String> it = new CompositeIterator<>();
		it.add(list.iterator());
		it.hasNext();
		assertThatIllegalStateException().isThrownBy(() ->
				it.add(list.iterator()));
		CompositeIterator<String> it2 = new CompositeIterator<>();
		it2.add(list.iterator());
		it2.next();
		assertThatIllegalStateException().isThrownBy(() ->
				it2.add(list.iterator()));
	}

	@Test
	void duplicateIterators() {
		List<String> list = Arrays.asList("0", "1");
		Iterator<String> iterator = list.iterator();
		CompositeIterator<String> it = new CompositeIterator<>();
		it.add(iterator);
		it.add(list.iterator());
		assertThatIllegalArgumentException().isThrownBy(() ->
				it.add(iterator));
	}

}
