/*
 * Copyright 2002-2023 the original author or authors.
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
import java.util.Map;
import java.util.Optional;

import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;

import org.springframework.core.ResolvableType;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DataBinder} with constructor binding.
 *
 * @author Rossen Stoyanchev
 */
public class DataBinderConstructTests {


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

	@SuppressWarnings("SameParameterValue")
	private static DataBinder initDataBinder(Class<DataClass> targetType) {
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


	private static class MapValueResolver implements DataBinder.ValueResolver {

		private final Map<String, Object> values;

		private MapValueResolver(Map<String, Object> values) {
			this.values = values;
		}

		@Override
		public Object resolveValue(String name, Class<?> type) {
			return values.get(name);
		}
	}

}
