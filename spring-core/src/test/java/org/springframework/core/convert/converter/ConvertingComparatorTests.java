/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core.convert.converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.comparator.ComparableComparator;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Tests for {@link ConvertingComparator}.
 *
 * @author Phillip Webb
 */
public class ConvertingComparatorTests {

	private final StringToInteger converter = new StringToInteger();

	private final ConversionService conversionService = new DefaultConversionService();

	private final TestComparator comparator = new TestComparator();

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowOnNullComparator() throws Exception {
		new ConvertingComparator<>(null, this.converter);
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowOnNullConverter() throws Exception {
		new ConvertingComparator<String, Integer>(this.comparator, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowOnNullConversionService() throws Exception {
		new ConvertingComparator<String, Integer>(this.comparator, null, Integer.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowOnNullType() throws Exception {
		new ConvertingComparator<String, Integer>(this.comparator,
			this.conversionService, null);
	}

	@Test
	public void shouldUseConverterOnCompare() throws Exception {
		ConvertingComparator<String, Integer> convertingComparator = new ConvertingComparator<>(
				this.comparator, this.converter);
		testConversion(convertingComparator);
	}

	@Test
	public void shouldUseConversionServiceOnCompare() throws Exception {
		ConvertingComparator<String, Integer> convertingComparator = new ConvertingComparator<>(
				comparator, conversionService, Integer.class);
		testConversion(convertingComparator);
	}

	@Test
	public void shouldGetForConverter() throws Exception {
		testConversion(new ConvertingComparator<>(comparator, converter));
	}

	private void testConversion(ConvertingComparator<String, Integer> convertingComparator) {
		assertThat(convertingComparator.compare("0", "0"), is(0));
		assertThat(convertingComparator.compare("0", "1"), is(-1));
		assertThat(convertingComparator.compare("1", "0"), is(1));
		comparator.assertCalled();
	}

	@Test
	public void shouldGetMapEntryKeys() throws Exception {
		ArrayList<Entry<String, Integer>> list = createReverseOrderMapEntryList();
		Comparator<Map.Entry<String, Integer>> comparator = ConvertingComparator.mapEntryKeys(new ComparableComparator<String>());
		Collections.sort(list, comparator);
		assertThat(list.get(0).getKey(), is("a"));
	}

	@Test
	public void shouldGetMapEntryValues() throws Exception {
		ArrayList<Entry<String, Integer>> list = createReverseOrderMapEntryList();
		Comparator<Map.Entry<String, Integer>> comparator = ConvertingComparator.mapEntryValues(new ComparableComparator<Integer>());
		Collections.sort(list, comparator);
		assertThat(list.get(0).getValue(), is(1));
	}

	private ArrayList<Entry<String, Integer>> createReverseOrderMapEntryList() {
		Map<String, Integer> map = new LinkedHashMap<>();
		map.put("b", 2);
		map.put("a", 1);
		ArrayList<Entry<String, Integer>> list = new ArrayList<>(
				map.entrySet());
		assertThat(list.get(0).getKey(), is("b"));
		return list;
	}

	private static class StringToInteger implements Converter<String, Integer> {

		@Override
		public Integer convert(String source) {
			return Integer.valueOf(source);
		}

	}


	private static class TestComparator extends ComparableComparator<Integer> {

		private boolean called;

		@Override
		public int compare(Integer o1, Integer o2) {
			assertThat(o1, instanceOf(Integer.class));
			assertThat(o2, instanceOf(Integer.class));
			this.called = true;
			return super.compare(o1, o2);
		};

		public void assertCalled() {
			assertThat(this.called, is(true));
		}
	}

}
