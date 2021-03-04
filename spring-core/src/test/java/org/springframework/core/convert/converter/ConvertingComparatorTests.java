/*
 * Copyright 2002-2020 the original author or authors.
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.comparator.ComparableComparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ConvertingComparator}.
 *
 * @author Phillip Webb
 */
class ConvertingComparatorTests {

	private final StringToInteger converter = new StringToInteger();

	private final ConversionService conversionService = new DefaultConversionService();

	private final TestComparator comparator = new TestComparator();

	@Test
	void shouldThrowOnNullComparator() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new ConvertingComparator<>(null, this.converter));
	}

	@Test
	void shouldThrowOnNullConverter() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new ConvertingComparator<String, Integer>(this.comparator, null));
	}

	@Test
	void shouldThrowOnNullConversionService() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new ConvertingComparator<String, Integer>(this.comparator, null, Integer.class));
	}

	@Test
	void shouldThrowOnNullType() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new ConvertingComparator<String, Integer>(this.comparator, this.conversionService, null));
	}

	@Test
	void shouldUseConverterOnCompare() throws Exception {
		ConvertingComparator<String, Integer> convertingComparator = new ConvertingComparator<>(
				this.comparator, this.converter);
		testConversion(convertingComparator);
	}

	@Test
	void shouldUseConversionServiceOnCompare() throws Exception {
		ConvertingComparator<String, Integer> convertingComparator = new ConvertingComparator<>(
				comparator, conversionService, Integer.class);
		testConversion(convertingComparator);
	}

	@Test
	void shouldGetForConverter() throws Exception {
		testConversion(new ConvertingComparator<>(comparator, converter));
	}

	private void testConversion(ConvertingComparator<String, Integer> convertingComparator) {
		assertThat(convertingComparator.compare("0", "0")).isEqualTo(0);
		assertThat(convertingComparator.compare("0", "1")).isEqualTo(-1);
		assertThat(convertingComparator.compare("1", "0")).isEqualTo(1);
		comparator.assertCalled();
	}

	@Test
	void shouldGetMapEntryKeys() throws Exception {
		ArrayList<Entry<String, Integer>> list = createReverseOrderMapEntryList();
		Comparator<Map.Entry<String, Integer>> comparator = ConvertingComparator.mapEntryKeys(new ComparableComparator<String>());
		list.sort(comparator);
		assertThat(list.get(0).getKey()).isEqualTo("a");
	}

	@Test
	void shouldGetMapEntryValues() throws Exception {
		ArrayList<Entry<String, Integer>> list = createReverseOrderMapEntryList();
		Comparator<Map.Entry<String, Integer>> comparator = ConvertingComparator.mapEntryValues(new ComparableComparator<Integer>());
		list.sort(comparator);
		assertThat(list.get(0).getValue()).isEqualTo(1);
	}

	private ArrayList<Entry<String, Integer>> createReverseOrderMapEntryList() {
		Map<String, Integer> map = new LinkedHashMap<>();
		map.put("b", 2);
		map.put("a", 1);
		ArrayList<Entry<String, Integer>> list = new ArrayList<>(
				map.entrySet());
		assertThat(list.get(0).getKey()).isEqualTo("b");
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
			assertThat(o1).isInstanceOf(Integer.class);
			assertThat(o2).isInstanceOf(Integer.class);
			this.called = true;
			return super.compare(o1, o2);
		};

		public void assertCalled() {
			assertThat(this.called).isTrue();
		}
	}

}
