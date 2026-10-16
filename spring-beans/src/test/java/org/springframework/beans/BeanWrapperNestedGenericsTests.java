/*
 * Copyright 2026-present the original author or authors.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.core.convert.support.DefaultConversionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for generic type resolution through nested bean properties.
 *
 * @author Hyun Lee
 */
class BeanWrapperNestedGenericsTests {

	@ParameterizedTest
	@ValueSource(strings = {"filter.value", "enumFilter.value", "filters[0].value",
			"filterMap[key].value", "optionalFilter.value", "nestedFilter.value.value"})
	void nestedGenericEnum(String path) {
		BeanWrapper wrapper = new BeanWrapperImpl(new FilterHolder());
		wrapper.setAutoGrowNestedPaths(true);

		wrapper.setPropertyValue(path, "FIRST");

		assertThat(wrapper.getPropertyValue(path)).isSameAs(State.FIRST);
		assertThat(wrapper.getPropertyType(path)).isEqualTo(State.class);
		assertThat(wrapper.getPropertyTypeDescriptor(path).getType()).isEqualTo(State.class);
	}

	@Test
	void nestedGenericArrayAndCollection() {
		BeanWrapper wrapper = new BeanWrapperImpl(new FilterHolder());
		wrapper.setAutoGrowNestedPaths(true);

		wrapper.setPropertyValue("filter.values", new String[] {"FIRST", "SECOND"});
		wrapper.setPropertyValue("filter.list[0]", "SECOND");
		wrapper.setPropertyValue("filter.map[FIRST]", "SECOND");

		assertThat(wrapper.getPropertyValue("filter.values")).isEqualTo(new State[] {State.FIRST, State.SECOND});
		assertThat(wrapper.getPropertyValue("filter.list[0]")).isSameAs(State.SECOND);
		assertThat(wrapper.getPropertyValue("filter.map[FIRST]")).isSameAs(State.SECOND);
	}

	@Test
	void independentGenericDeclarations() {
		BeanWrapper wrapper = new BeanWrapperImpl(new FilterHolder());
		wrapper.setAutoGrowNestedPaths(true);

		wrapper.setPropertyValue("filter.value", "FIRST");
		wrapper.setPropertyValue("otherFilter.value", "FIRST");

		assertThat(wrapper.getPropertyValue("filter.value")).isSameAs(State.FIRST);
		assertThat(wrapper.getPropertyValue("otherFilter.value")).isSameAs(OtherState.FIRST);
	}

	@Test
	void concreteRuntimeSubclass() {
		FilterHolder holder = new FilterHolder();
		holder.setFilter(new StateFilter());
		BeanWrapper wrapper = new BeanWrapperImpl(holder);

		wrapper.setPropertyValue("filter.value", "FIRST");

		assertThat(holder.getFilter().getValue()).isSameAs(State.FIRST);
	}

	@Test
	void concreteRuntimeSubclassWithWildcardDeclaration() {
		FilterHolder holder = new FilterHolder();
		BeanWrapper wrapper = new BeanWrapperImpl(holder);

		wrapper.setPropertyValue("wildcardFilter.value", "FIRST");

		assertThat(holder.getWildcardFilter().getValue()).isSameAs(State.FIRST);
	}

	@Test
	void genericRuntimeSubclass() {
		FilterHolder holder = new FilterHolder();
		holder.setFilter(new GenericFilter<>());
		BeanWrapper wrapper = new BeanWrapperImpl(holder);

		wrapper.setPropertyValue("filter.value", "FIRST");

		assertThat(holder.getFilter().getValue()).isSameAs(State.FIRST);
	}

	@Test
	void nestedGenericEnumWithConversionService() {
		FilterHolder holder = new FilterHolder();
		BeanWrapper wrapper = new BeanWrapperImpl(holder);
		wrapper.setConversionService(new DefaultConversionService());

		wrapper.setPropertyValue("enumFilter.value", "FIRST");

		assertThat(holder.getEnumFilter().getValue()).isSameAs(State.FIRST);
	}


	enum State { FIRST, SECOND }

	enum OtherState { FIRST }

	static class Filter<T> {

		private T value;

		private T[] values;

		private final List<T> list = new ArrayList<>();

		private final Map<T, T> map = new HashMap<>();

		public T getValue() {
			return this.value;
		}

		public void setValue(T value) {
			this.value = value;
		}

		public T[] getValues() {
			return this.values;
		}

		public void setValues(T[] values) {
			this.values = values;
		}

		public Map<T, T> getMap() {
			return this.map;
		}

		public List<T> getList() {
			return this.list;
		}
	}

	static class EnumFilter<T extends Enum<T>> extends Filter<T> {
	}

	static class GenericFilter<V> extends Filter<V> {
	}

	static class StateFilter extends Filter<State> {
	}

	static class FilterHolder {

		private Filter<State> filter;

		private final EnumFilter<State> enumFilter = new EnumFilter<>();

		private final Filter<? extends Enum<?>> wildcardFilter = new StateFilter();

		private final Filter<OtherState> otherFilter = new Filter<>();

		private final List<Filter<State>> filters = new ArrayList<>();

		private final Map<String, Filter<State>> filterMap = new HashMap<>();

		private final Optional<Filter<State>> optionalFilter = Optional.of(new Filter<>());

		private final Filter<Filter<State>> nestedFilter = new Filter<>();

		public Filter<State> getFilter() {
			return this.filter;
		}

		public void setFilter(Filter<State> filter) {
			this.filter = filter;
		}

		public Filter<? extends Enum<?>> getWildcardFilter() {
			return this.wildcardFilter;
		}

		public EnumFilter<State> getEnumFilter() {
			return this.enumFilter;
		}

		public Filter<OtherState> getOtherFilter() {
			return this.otherFilter;
		}

		public List<Filter<State>> getFilters() {
			return this.filters;
		}

		public Map<String, Filter<State>> getFilterMap() {
			return this.filterMap;
		}

		public Optional<Filter<State>> getOptionalFilter() {
			return this.optionalFilter;
		}

		public Filter<Filter<State>> getNestedFilter() {
			return this.nestedFilter;
		}
	}

}
