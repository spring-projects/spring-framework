/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
class BeanWrapperEnumTests {

	@Test
	void customEnum() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnum", "VALUE_1");
		assertThat(gb.getCustomEnum()).isEqualTo(CustomEnum.VALUE_1);
	}

	@Test
	void customEnumWithNull() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnum", null);
		assertThat(gb.getCustomEnum()).isNull();
	}

	@Test
	void customEnumWithEmptyString() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnum", "");
		assertThat(gb.getCustomEnum()).isNull();
	}

	@Test
	void customEnumArrayWithSingleValue() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumArray", "VALUE_1");
		assertThat(gb.getCustomEnumArray()).hasSize(1);
		assertThat(gb.getCustomEnumArray()[0]).isEqualTo(CustomEnum.VALUE_1);
	}

	@Test
	void customEnumArrayWithMultipleValues() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumArray", new String[] {"VALUE_1", "VALUE_2"});
		assertThat(gb.getCustomEnumArray()).hasSize(2);
		assertThat(gb.getCustomEnumArray()[0]).isEqualTo(CustomEnum.VALUE_1);
		assertThat(gb.getCustomEnumArray()[1]).isEqualTo(CustomEnum.VALUE_2);
	}

	@Test
	void customEnumArrayWithMultipleValuesAsCsv() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumArray", "VALUE_1,VALUE_2");
		assertThat(gb.getCustomEnumArray()).hasSize(2);
		assertThat(gb.getCustomEnumArray()[0]).isEqualTo(CustomEnum.VALUE_1);
		assertThat(gb.getCustomEnumArray()[1]).isEqualTo(CustomEnum.VALUE_2);
	}

	@Test
	void customEnumSetWithSingleValue() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumSet", "VALUE_1");
		assertThat(gb.getCustomEnumSet()).hasSize(1);
		assertThat(gb.getCustomEnumSet()).contains(CustomEnum.VALUE_1);
	}

	@Test
	void customEnumSetWithMultipleValues() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumSet", new String[] {"VALUE_1", "VALUE_2"});
		assertThat(gb.getCustomEnumSet()).hasSize(2);
		assertThat(gb.getCustomEnumSet()).contains(CustomEnum.VALUE_1);
		assertThat(gb.getCustomEnumSet()).contains(CustomEnum.VALUE_2);
	}

	@Test
	void customEnumSetWithMultipleValuesAsCsv() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumSet", "VALUE_1,VALUE_2");
		assertThat(gb.getCustomEnumSet()).hasSize(2);
		assertThat(gb.getCustomEnumSet()).contains(CustomEnum.VALUE_1);
		assertThat(gb.getCustomEnumSet()).contains(CustomEnum.VALUE_2);
	}

	@Test
	void customEnumSetWithGetterSetterMismatch() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumSetMismatch", new String[] {"VALUE_1", "VALUE_2"});
		assertThat(gb.getCustomEnumSet()).hasSize(2);
		assertThat(gb.getCustomEnumSet()).contains(CustomEnum.VALUE_1);
		assertThat(gb.getCustomEnumSet()).contains(CustomEnum.VALUE_2);
	}

	@Test
	void standardEnumSetWithMultipleValues() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setConversionService(new DefaultConversionService());
		assertThat(gb.getStandardEnumSet()).isNull();
		bw.setPropertyValue("standardEnumSet", new String[] {"VALUE_1", "VALUE_2"});
		assertThat(gb.getStandardEnumSet()).hasSize(2);
		assertThat(gb.getStandardEnumSet()).contains(CustomEnum.VALUE_1);
		assertThat(gb.getStandardEnumSet()).contains(CustomEnum.VALUE_2);
	}

	@Test
	void standardEnumSetWithAutoGrowing() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setAutoGrowNestedPaths(true);
		assertThat(gb.getStandardEnumSet()).isNull();
		bw.getPropertyValue("standardEnumSet.class");
		assertThat(gb.getStandardEnumSet()).isEmpty();
	}

	@Test
	void standardEnumMapWithMultipleValues() {
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
	void standardEnumMapWithAutoGrowing() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setAutoGrowNestedPaths(true);
		assertThat(gb.getStandardEnumMap()).isNull();
		bw.setPropertyValue("standardEnumMap[VALUE_1]", 1);
		assertThat(gb.getStandardEnumMap()).hasSize(1);
		assertThat(gb.getStandardEnumMap().get(CustomEnum.VALUE_1)).isEqualTo(1);
	}

	@Test
	void nonPublicEnum() {
		NonPublicEnumHolder holder = new NonPublicEnumHolder();
		BeanWrapper bw = new BeanWrapperImpl(holder);
		bw.setPropertyValue("nonPublicEnum", "VALUE_1");
		assertThat(holder.getNonPublicEnum()).isEqualTo(NonPublicEnum.VALUE_1);
	}


	enum NonPublicEnum {

		VALUE_1, VALUE_2
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
