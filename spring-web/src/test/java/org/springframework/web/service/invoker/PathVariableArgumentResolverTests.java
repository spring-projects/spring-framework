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

package org.springframework.web.service.invoker;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;


/**
 * Tests for {@link PathVariableArgumentResolver}.
 *
 * @author Olga Maciaszek-Sharma
 */
class PathVariableArgumentResolverTests {

	private final TestHttpClientAdapter clientAdapter = new TestHttpClientAdapter();

	private final Service service = this.clientAdapter.createService(Service.class);


	@Test
	void stringVariable() {
		this.service.execute("test");
		assertPathVariable("id", "test");
	}

	@Test
	void objectVariable() {
		this.service.execute(Boolean.TRUE);
		assertPathVariable("id", "true");
	}

	@Test
	void namedVariable() {
		this.service.executeNamed("test");
		assertPathVariable("id", "test");
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	void nullVariableRequired() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.service.execute(null));
	}

	@Test
	void nullVariableNotRequired() {
		this.service.executeNotRequired(null);
		assertPathVariable("id", null);
	}

	@Test
	void optionalStringVariable() {
		this.service.execute(Optional.of("test"));
		assertPathVariable("id", "test");
	}

	@Test
	void optionalObjectVariable() {
		this.service.executeOptional(Optional.of(Boolean.TRUE));
		assertPathVariable("id", "true");
	}

	@Test
	void optionalEmpty() {
		this.service.executeOptional(Optional.empty());
		assertPathVariable("id", null);
	}

	@Test
	void optionalEmptyOnObjectArgument() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.service.execute(Optional.empty()));
	}

	@Test
	void mapOfVariables() {
		this.service.executeMap(Map.of("id", "test"));
		assertPathVariable("id", "test");
	}

	@Test
	void mapOfVariablesIsNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.service.executeMap(null));
	}

	@Test
	void mapOfVariablesHasOptionalValue() {
		this.service.executeMapWithOptionalValue(Map.of("id", Optional.of("test")));
		assertPathVariable("id", "test");
	}


	@Test
	void mapOfVariablesHasOptionalEmpty() {
		this.service.executeMapWithOptionalValue(Map.of("id", Optional.empty()));
		assertPathVariable("id", null);
	}

	@SuppressWarnings("SameParameterValue")
	private void assertPathVariable(String name, @Nullable String expectedValue) {
		assertThat(this.clientAdapter.getRequestValues().getUriVariables().get(name))
				.isEqualTo(expectedValue);
	}


	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	private interface Service {

		@GetExchange
		void execute(@PathVariable String id);

		@GetExchange
		void execute(@PathVariable Object id);

		@GetExchange
		void executeNamed(@PathVariable(name = "id") String employeeId);

		@GetExchange
		void executeNotRequired(@Nullable @PathVariable(required = false) String id);

		@GetExchange
		void executeOptional(@PathVariable Optional<Boolean> id);

		@GetExchange
		void executeMap(@Nullable @PathVariable Map<String, String> map);

		@GetExchange
		void executeMapWithOptionalValue(@PathVariable Map<String, Optional<String>> map);
	}

}
