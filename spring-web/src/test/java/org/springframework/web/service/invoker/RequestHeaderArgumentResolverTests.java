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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.groovy.util.Maps;
import org.junit.jupiter.api.Test;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;


/**
 * Unit tests for {@link RequestHeaderArgumentResolver}.
 *
 * @author Olga Maciaszek-Sharma
 */
class RequestHeaderArgumentResolverTests {

	private final TestHttpClientAdapter clientAdapter = new TestHttpClientAdapter();

	private final Service service = this.clientAdapter.createService(Service.class);


	@Test
	void stringHeader() {
		this.service.executeString("test");
		assertRequestHeaders("id", "test");
	}

	@Test
	void objectHeader() {
		this.service.execute(Boolean.TRUE);
		assertRequestHeaders("id", "true");
	}

	@Test
	void listHeader() {
		this.service.executeList(List.of("test1", Boolean.TRUE, "test3"));
		assertRequestHeaders("multiValueHeader", "test1", "true", "test3");
	}

	@Test
	void arrayHeader() {
		this.service.executeArray("test1", Boolean.FALSE, "test3");
		assertRequestHeaders("multiValueHeader", "test1", "false", "test3");
	}

	@Test
	void namedHeader() {
		this.service.executeNamed("test");
		assertRequestHeaders("id", "test");
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	void nullHeaderRequired() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.service.executeString(null));
	}

	@Test
	void nullHeaderNotRequired() {
		this.service.executeNotRequired(null);
		assertRequestHeaders("id");
	}

	@Test
	void nullHeaderWithDefaultValue() {
		this.service.executeWithDefaultValue(null);
		assertRequestHeaders("id", "default");
	}

	@Test
	void optionalStringHeader() {
		this.service.executeOptional(Optional.of("test"));
		assertRequestHeaders("id", "test");
	}

	@Test
	void optionalObjectHeader() {
		this.service.executeOptional(Optional.of(Boolean.TRUE));
		assertRequestHeaders("id", "true");
	}

	@Test
	void optionalEmpty() {
		this.service.executeOptional(Optional.empty());
		assertRequestHeaders("id");
	}

	@Test
	void optionalEmpthyWithDefaultValue() {
		this.service.executeOptionalWithDefaultValue(Optional.empty());
		assertRequestHeaders("id", "default");
	}

	@Test
	void mapOfHeaders() {
		this.service.executeMap(Maps.of("header1", "true", "header2", "false"));
		assertRequestHeaders("header1", "true");
		assertRequestHeaders("header2", "false");
	}

	@Test
	void mapOfHeadersIsNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.service.executeMap(null));
	}

	@Test
	void mapOfHeadersHasOptionalValue() {
		this.service.executeMapWithOptionalValue(Map.of("id", Optional.of("test")));
		assertRequestHeaders("id", "test");
	}

	private void assertRequestHeaders(String key, String... values) {
		List<String> actualValues = this.clientAdapter.getRequestValues().getHeaders().get(key);
		if (ObjectUtils.isEmpty(values)) {
			assertThat(actualValues).isNull();
		}
		else {
			assertThat(actualValues).containsOnly(values);
		}
	}


	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	private interface Service {

		@GetExchange
		void executeString(@RequestHeader String id);

		@GetExchange
		void execute(@RequestHeader Object id);

		@GetExchange
		void executeList(@RequestHeader List<Object> multiValueHeader);

		@GetExchange
		void executeArray(@RequestHeader Object... multiValueHeader);

		@GetExchange
		void executeNamed(@RequestHeader(name = "id") String employeeId);

		@GetExchange
		void executeNotRequired(@Nullable @RequestHeader(required = false) String id);

		@GetExchange
		void executeWithDefaultValue(@Nullable @RequestHeader(defaultValue = "default") String id);

		@GetExchange
		void executeOptional(@RequestHeader Optional<Object> id);

		@GetExchange
		void executeOptionalWithDefaultValue(@RequestHeader(defaultValue = "default") Optional<Object> id);

		@GetExchange
		void executeMap(@Nullable @RequestHeader Map<String, String> id);

		@GetExchange
		void executeMapWithOptionalValue(@RequestHeader Map<String, Optional<String>> headers);

	}

}
