/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.orm.hibernate4.support;

import java.util.Arrays;
import java.util.stream.Stream;

import org.hibernate.ScrollableResults;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.orm.hibernate4.support.ScrollableResultsSpliterator.*;


/**
 * @author Marko Topolnik
 * @since 4.2
 */
public class ScrollableResultsSpliteratorTests {

	@Test
	public void testUnaryTuple() {
		final ScrollableResults sr = mockScrollableResults(new Object[][] {
			{42},
			{24},
		});
		assertEquals("Sum of two unary result rows", 66,
				resultStream(Integer.class, sr).mapToInt(i->i).sum());
	}

	@Test
	public void testBinaryTuple() {
		final ScrollableResults sr = mockScrollableResults(new Object[][] {
				{42, 17},
				{24, 37},
		});
		assertEquals("Sum of second result column", 54,
				resultStream(Object[].class, sr).mapToInt(row->(int)row[1]).sum());
	}

	@Test
	public void testParallel() {
		final Object[][] data = new Object[10_000][];
		Arrays.setAll(data, i -> new Object[] {i});
		final ScrollableResults sr = mockScrollableResults(data);
		assertEquals("Sum 0..9999",
				Stream.of(data).mapToInt(row -> (int)row[0]).sum(),
				resultStream(Integer.class, sr).parallel().mapToInt(this::aLotOfWork).sum());
	}

	int aLotOfWork(Integer input) {
		double d = 1.001;
		for (int i = 0; i < 1000; i++) {
			d = Math.pow(d, (d < 1e18)? 1.001 : 0.999);
		}
		return d < 0? -input : input;
	}

	static ScrollableResults mockScrollableResults(Object[][] rows) {
		final ScrollableResults sr = mock(ScrollableResults.class);
		final int[] cursor = {-1};
		given(sr.next()).willAnswer(x -> (++cursor[0] < rows.length));
		given(sr.get()).willAnswer(x -> rows[cursor[0]]);
		given(sr.get(0)).willAnswer(x -> rows[cursor[0]][0]);
		return sr;
	}
}
