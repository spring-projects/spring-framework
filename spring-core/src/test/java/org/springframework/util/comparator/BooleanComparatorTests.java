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

import java.util.Comparator;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Tests for {@link BooleanComparator}.
 *
 * @author Keith Donald
 * @author Chris Beams
 * @author Phillip Webb
 */
public class BooleanComparatorTests {

	@Test
	public void shouldCompareWithTrueLow() {
		Comparator<Boolean> c = new BooleanComparator(true);
		assertThat(c.compare(new Boolean(true), new Boolean(false)), is(-1));
		assertThat(c.compare(Boolean.TRUE, Boolean.TRUE), is(0));
	}

	@Test
	public void shouldCompareWithTrueHigh() {
		Comparator<Boolean> c = new BooleanComparator(false);
		assertThat(c.compare(new Boolean(true), new Boolean(false)), is(1));
		assertThat(c.compare(Boolean.TRUE, Boolean.TRUE), is(0));
	}

	@Test
	public void shouldCompareFromTrueLow() {
		Comparator<Boolean> c = BooleanComparator.TRUE_LOW;
		assertThat(c.compare(true, false), is(-1));
		assertThat(c.compare(Boolean.TRUE, Boolean.TRUE), is(0));
	}

	@Test
	public void shouldCompareFromTrueHigh() {
		Comparator<Boolean> c = BooleanComparator.TRUE_HIGH;
		assertThat(c.compare(true, false), is(1));
		assertThat(c.compare(Boolean.TRUE, Boolean.TRUE), is(0));
	}

}
