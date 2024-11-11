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

package org.springframework.validation;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;

import org.springframework.core.ResolvableType;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataBinder} with constructor binding.
 *
 * @author Rossen Stoyanchev
 */
class DataBinderConstructTests {

	@Test
	void dataClassBinding() {
		MapValueResolver valueResolver = new MapValueResolver(Map.of("param1", "value1", "param2", "true"));
		DataBinder binder = initDataBinder(DataClass.class);
		binder.construct(valueResolver);

		DataClass dataClass = getTarget(binder);
		assertThat(dataClass.param1()).isEqualTo("value1");
		assertThat(dataClass.param2()).isEqualTo(true);
		assertThat(dataClass.param3()).isEqualTo(0);
	}

	@Test
	void dataClassBindingWithOptionalParameter() {
		MapValueResolver valueResolver =
				new MapValueResolver(Map.of("param1", "value1", "param2", "true", "optionalParam", "8"));

		DataBinder binder = initDataBinder(DataClass.class);
		binder.construct(valueResolver);

		DataClass dataClass = getTarget(binder);
		assertThat(dataClass.param1()).isEqualTo("value1");
		assertThat(dataClass.param2()).isEqualTo(true);
		assertThat(dataClass.param3()).isEqualTo(8);
	}

	@Test
	void dataClassBindingWithMissingParameter() {
		MapValueResolver valueResolver = new MapValueResolver(Map.of("param1", "value1"));
		DataBinder binder = initDataBinder(DataClass.class);
		binder.construct(valueResolver);

		BindingResult bindingResult = binder.getBindingResult();
		assertThat(bindingResult.getAllErrors()).hasSize(1);
		assertThat(bindingResult.getFieldValue("param1")).isEqualTo("value1");
		assertThat(bindingResult.getFieldValue("param2")).isNull();
		assertThat(bindingResult.getFieldValue("param3")).isNull();
	}

	@Test  // gh-31821
	void dataClassBindingWithNestedOptionalParameterWithMissingParameter() {
		MapValueResolver valueResolver = new MapValueResolver(Map.of("param1", "value1"));
		DataBinder binder = initDataBinder(NestedDataClass.class);
		binder.construct(valueResolver);

		NestedDataClass dataClass = getTarget(binder);
		assertThat(dataClass.param1()).isEqualTo("value1");
		assertThat(dataClass.nestedParam2()).isNull();
	}

	@Test
	void dataClassBindingWithConversionError() {
		MapValueResolver valueResolver = new MapValueResolver(Map.of("param1", "value1", "param2", "x"));
		DataBinder binder = initDataBinder(DataClass.class);
		binder.construct(valueResolver);

		BindingResult bindingResult = binder.getBindingResult();
		assertThat(bindingResult.getAllErrors()).hasSize(1);
		assertThat(bindingResult.getFieldValue("param1")).isEqualTo("value1");
		assertThat(bindingResult.getFieldValue("param2")).isEqualTo("x");
		assertThat(bindingResult.getFieldValue("param3")).isNull();
	}

	@Test
	void listBinding() {
		MapValueResolver valueResolver = new MapValueResolver(Map.of(
				"dataClassList[0].param1", "value1", "dataClassList[0].param2", "true",
				"dataClassList[1].param1", "value2", "dataClassList[1].param2", "true",
				"dataClassList[2].param1", "value3", "dataClassList[2].param2", "true"));

		DataBinder binder = initDataBinder(ListDataClass.class);
		binder.construct(valueResolver);

		ListDataClass dataClass = getTarget(binder);
		List<DataClass> list = dataClass.dataClassList();

		assertThat(list).hasSize(3);
		assertThat(list.get(0).param1()).isEqualTo("value1");
		assertThat(list.get(1).param1()).isEqualTo("value2");
		assertThat(list.get(2).param1()).isEqualTo("value3");
	}

	@Test
	void mapBinding() {
		MapValueResolver valueResolver = new MapValueResolver(Map.of(
				"dataClassMap[a].param1", "value1", "dataClassMap[a].param2", "true",
				"dataClassMap[b].param1", "value2", "dataClassMap[b].param2", "true",
				"dataClassMap['c'].param1", "value3", "dataClassMap['c'].param2", "true"));

		DataBinder binder = initDataBinder(MapDataClass.class);
		binder.construct(valueResolver);

		MapDataClass dataClass = getTarget(binder);
		Map<String, DataClass> map = dataClass.dataClassMap();

		assertThat(map).hasSize(3);
		assertThat(map.get("a").param1()).isEqualTo("value1");
		assertThat(map.get("b").param1()).isEqualTo("value2");
		assertThat(map.get("c").param1()).isEqualTo("value3");
	}

	@Test
	void arrayBinding() {
		MapValueResolver valueResolver = new MapValueResolver(Map.of(
				"dataClassArray[0].param1", "value1", "dataClassArray[0].param2", "true",
				"dataClassArray[1].param1", "value2", "dataClassArray[1].param2", "true",
				"dataClassArray[2].param1", "value3", "dataClassArray[2].param2", "true"));

		DataBinder binder = initDataBinder(ArrayDataClass.class);
		binder.construct(valueResolver);

		ArrayDataClass dataClass = getTarget(binder);
		DataClass[] array = dataClass.dataClassArray();

		assertThat(array).hasSize(3);
		assertThat(array[0].param1()).isEqualTo("value1");
		assertThat(array[1].param1()).isEqualTo("value2");
		assertThat(array[2].param1()).isEqualTo("value3");
	}

	@SuppressWarnings("SameParameterValue")
	private static DataBinder initDataBinder(Class<?> targetType) {
		DataBinder binder = new DataBinder(null);
		binder.setTargetType(ResolvableType.forClass(targetType));
		binder.setConversionService(new DefaultFormattingConversionService());
		return binder;
	}

	@SuppressWarnings("unchecked")
	private static <T> T getTarget(DataBinder dataBinder) {
		assertThat(dataBinder.getBindingResult().getAllErrors()).isEmpty();
		Object target = dataBinder.getTarget();
		assertThat(target).isNotNull();
		return (T) target;
	}


	private static class DataClass {

		@NotNull
		private final String param1;

		private final boolean param2;

		private int param3;

		@ConstructorProperties({"param1", "param2", "optionalParam"})
		DataClass(String param1, boolean p2, Optional<Integer> optionalParam) {
			this.param1 = param1;
			this.param2 = p2;
			Assert.notNull(optionalParam, "Optional must not be null");
			optionalParam.ifPresent(integer -> this.param3 = integer);
		}

		public String param1() {
			return this.param1;
		}

		public boolean param2() {
			return this.param2;
		}

		public int param3() {
			return this.param3;
		}
	}


	private static class NestedDataClass {

		private final String param1;

		@Nullable
		private final DataClass nestedParam2;

		public NestedDataClass(String param1, @Nullable DataClass nestedParam2) {
			this.param1 = param1;
			this.nestedParam2 = nestedParam2;
		}

		public String param1() {
			return this.param1;
		}

		@Nullable
		public DataClass nestedParam2() {
			return this.nestedParam2;
		}
	}


	private record ListDataClass(List<DataClass> dataClassList) {
	}


	private record MapDataClass(Map<String, DataClass> dataClassMap) {
	}


	private record ArrayDataClass(DataClass[] dataClassArray) {
	}


	private record MapValueResolver(Map<String, Object> map) implements DataBinder.ValueResolver {

		@Override
		public Object resolveValue(String name, Class<?> type) {
			return map.get(name);
		}

		@Override
		public Set<String> getNames() {
			return this.map.keySet();
		}
	}

}
