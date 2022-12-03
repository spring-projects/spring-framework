/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.CustomEnum;
import org.springframework.beans.testfixture.beans.GenericBean;
import org.springframework.core.convert.support.DefaultConversionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class BeanWrapperEnumTests {

	@Test
	public void testCustomEnum() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnum", "VALUE_1");
		assertThat(gb.getCustomEnum()).isEqualTo(CustomEnum.VALUE_1);
	}

	@Test
	public void testCustomEnumWithNull() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnum", null);
		assertThat(gb.getCustomEnum()).isNull();
	}

	@Test
	public void testCustomEnumWithEmptyString() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnum", "");
		assertThat(gb.getCustomEnum()).isNull();
	}

	@Test
	public void testCustomEnumArrayWithSingleValue() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumArray", "VALUE_1");
		assertThat(gb.getCustomEnumArray()).hasSize(1);
		assertThat(gb.getCustomEnumArray()[0]).isEqualTo(CustomEnum.VALUE_1);
	}

	@Test
	public void testCustomEnumArrayWithMultipleValues() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumArray", new String[] {"VALUE_1", "VALUE_2"});
		assertThat(gb.getCustomEnumArray()).hasSize(2);
		assertThat(gb.getCustomEnumArray()[0]).isEqualTo(CustomEnum.VALUE_1);
		assertThat(gb.getCustomEnumArray()[1]).isEqualTo(CustomEnum.VALUE_2);
	}

	@Test
	public void testCustomEnumArrayWithMultipleValuesAsCsv() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumArray", "VALUE_1,VALUE_2");
		assertThat(gb.getCustomEnumArray()).hasSize(2);
		assertThat(gb.getCustomEnumArray()[0]).isEqualTo(CustomEnum.VALUE_1);
		assertThat(gb.getCustomEnumArray()[1]).isEqualTo(CustomEnum.VALUE_2);
	}

	@Test
	public void testCustomEnumSetWithSingleValue() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumSet", "VALUE_1");
		assertThat(gb.getCustomEnumSet()).hasSize(1);
		assertThat(gb.getCustomEnumSet().contains(CustomEnum.VALUE_1)).isTrue();
	}

	@Test
	public void testCustomEnumSetWithMultipleValues() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumSet", new String[] {"VALUE_1", "VALUE_2"});
		assertThat(gb.getCustomEnumSet()).hasSize(2);
		assertThat(gb.getCustomEnumSet().contains(CustomEnum.VALUE_1)).isTrue();
		assertThat(gb.getCustomEnumSet().contains(CustomEnum.VALUE_2)).isTrue();
	}

	@Test
	public void testCustomEnumSetWithMultipleValuesAsCsv() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumSet", "VALUE_1,VALUE_2");
		assertThat(gb.getCustomEnumSet()).hasSize(2);
		assertThat(gb.getCustomEnumSet().contains(CustomEnum.VALUE_1)).isTrue();
		assertThat(gb.getCustomEnumSet().contains(CustomEnum.VALUE_2)).isTrue();
	}

	@Test
	public void testCustomEnumSetWithGetterSetterMismatch() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumSetMismatch", new String[] {"VALUE_1", "VALUE_2"});
		assertThat(gb.getCustomEnumSet()).hasSize(2);
		assertThat(gb.getCustomEnumSet().contains(CustomEnum.VALUE_1)).isTrue();
		assertThat(gb.getCustomEnumSet().contains(CustomEnum.VALUE_2)).isTrue();
	}

	@Test
	public void testStandardEnumSetWithMultipleValues() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setConversionService(new DefaultConversionService());
		assertThat(gb.getStandardEnumSet()).isNull();
		bw.setPropertyValue("standardEnumSet", new String[] {"VALUE_1", "VALUE_2"});
		assertThat(gb.getStandardEnumSet()).hasSize(2);
		assertThat(gb.getStandardEnumSet().contains(CustomEnum.VALUE_1)).isTrue();
		assertThat(gb.getStandardEnumSet().contains(CustomEnum.VALUE_2)).isTrue();
	}

	@Test
	public void testStandardEnumSetWithAutoGrowing() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setAutoGrowNestedPaths(true);
		assertThat(gb.getStandardEnumSet()).isNull();
		bw.getPropertyValue("standardEnumSet.class");
		assertThat(gb.getStandardEnumSet()).isEmpty();
	}

	@Test
	public void testStandardEnumMapWithMultipleValues() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setConversionService(new DefaultConversionService());
		assertThat(gb.getStandardEnumMap()).isNull();
		Map<String, Integer> map = new LinkedHashMap<>();
		map.put("VALUE_1", 1);
		map.put("VALUE_2", 2);
		bw.setPropertyValue("standardEnumMap", map);
		assertThat(gb.getStandardEnumMap()).hasSize(2);
		assertThat(gb.getStandardEnumMap().get(CustomEnum.VALUE_1)).isEqualTo(1);
		assertThat(gb.getStandardEnumMap().get(CustomEnum.VALUE_2)).isEqualTo(2);
	}

	@Test
	public void testStandardEnumMapWithAutoGrowing() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setAutoGrowNestedPaths(true);
		assertThat(gb.getStandardEnumMap()).isNull();
		bw.setPropertyValue("standardEnumMap[VALUE_1]", 1);
		assertThat(gb.getStandardEnumMap()).hasSize(1);
		assertThat(gb.getStandardEnumMap().get(CustomEnum.VALUE_1)).isEqualTo(1);
	}

	@Test
	public void testNonPublicEnum() {
		NonPublicEnumHolder holder = new NonPublicEnumHolder();
		BeanWrapper bw = new BeanWrapperImpl(holder);
		bw.setPropertyValue("nonPublicEnum", "VALUE_1");
		assertThat(holder.getNonPublicEnum()).isEqualTo(NonPublicEnum.VALUE_1);
	}


	enum NonPublicEnum {

		VALUE_1, VALUE_2;
	}


	static class NonPublicEnumHolder {

		private NonPublicEnum nonPublicEnum;

		public NonPublicEnum getNonPublicEnum() {
			return nonPublicEnum;
		}

		public void setNonPublicEnum(NonPublicEnum nonPublicEnum) {
			this.nonPublicEnum = nonPublicEnum;
		}
	}

}
