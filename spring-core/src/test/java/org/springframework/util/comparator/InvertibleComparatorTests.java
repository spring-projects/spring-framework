/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.util.comparator;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Comparator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests for {@link InvertibleComparator}.
 * 
 * @author Keith Donald
 * @author Chris Beams
 * @author Phillip Webb
 */

public class InvertibleComparatorTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private Comparator<Integer> comparator = ComparableComparator.get();

	@Test
	public void shouldNeedComparator() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Comparator must not be null");
		new InvertibleComparator<Object>(null);
	}

	@Test
	public void shouldNeedComparatorWithAscending() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Comparator must not be null");
		new InvertibleComparator<Object>(null, true);
	}

	@Test
	public void shouldDefaultToAscending() throws Exception {
		InvertibleComparator<Integer> invertibleComparator = new InvertibleComparator<Integer>(
			comparator);
		assertThat(invertibleComparator.isAscending(), is(true));
		assertThat(invertibleComparator.compare(1, 2), is(-1));
	}

	@Test
	public void shouldInvert() throws Exception {
		InvertibleComparator<Integer> invertibleComparator = new InvertibleComparator<Integer>(
			comparator);
		assertThat(invertibleComparator.isAscending(), is(true));
		assertThat(invertibleComparator.compare(1, 2), is(-1));
		invertibleComparator.invertOrder();
		assertThat(invertibleComparator.isAscending(), is(false));
		assertThat(invertibleComparator.compare(1, 2), is(1));
	}

	@Test
	public void shouldCompareAscending() throws Exception {
		InvertibleComparator<Integer> invertibleComparator = InvertibleComparator.ascending(comparator);
		assertThat(invertibleComparator.compare(1, 2), is(-1));
	}

	@Test
	public void shouldCompareDescending() throws Exception {
		InvertibleComparator<Integer> invertibleComparator = InvertibleComparator.descending(comparator);
		assertThat(invertibleComparator.compare(1, 2), is(1));
	}
}
