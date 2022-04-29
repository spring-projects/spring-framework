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

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
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
	void namedHeader() {
		this.service.executeNamed("test");
		assertRequestHeaders("id", "test");
	}

	@Test
	void listHeader() {
		this.service.execute(List.of("test1", Boolean.TRUE, "test3"));
		assertRequestHeaders("multiValueHeader", "test1", "true", "test3");
	}

	@Test
	void arrayHeader() {
		this.service.execute("test1", Boolean.FALSE, "test3");
		assertRequestHeaders("multiValueHeader", "test1", "false", "test3");
	}

	@Test
	void mapHeader() {
		this.service.executeMap(Maps.of("header1", "true", "header2", "false"));
		assertRequestHeaders("header1", "true");
		assertRequestHeaders("header2", "false");
	}

	@Test
	void mapHeaderNull() {
		this.service.executeMap(null);
		assertThat(getActualHeaders()).isEmpty();
	}

	@Test
	void mapWithOptional() {
		this.service.executeOptionalMapValue(Map.of("id", Optional.of("test")));
		assertRequestHeaders("id", "test");
	}

	@Test
	void nullHeaderRequired() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.service.executeString(null));
	}

	@Test
	void nullHeaderNotRequired() {
		this.service.executeNotRequired(null);
		assertThat(getActualHeaders().get("id")).isNull();
	}


	@Test
	void optional() {
		this.service.executeOptional(Optional.of("test"));
		assertRequestHeaders("id", "test");
	}

	@Test
	void optionalWithConversion() {
		this.service.executeOptional(Optional.of(Boolean.TRUE));
		assertRequestHeaders("id", "true");
	}

	@Test
	void optionalEmpty() {
		this.service.executeOptional(Optional.empty());
		assertThat(getActualHeaders().get("id")).isNull();
	}

	@Test
	void defaultValueWithNull() {
		this.service.executeWithDefaultValue(null);
		assertRequestHeaders("id", "default");
	}

	@Test
	void defaultValueWithOptional() {
		this.service.executeOptionalWithDefaultValue(Optional.empty());
		assertRequestHeaders("id", "default");
	}

	private void assertRequestHeaders(String key, String... values) {
		assertThat(getActualHeaders().get(key)).containsOnly(values);
	}

	private HttpHeaders getActualHeaders() {
		return this.clientAdapter.getRequestValues().getHeaders();
	}


	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	private interface Service {

		@GetExchange
		void executeString(@Nullable @RequestHeader String id);

		@GetExchange
		void execute(@RequestHeader Object id);

		@GetExchange
		void executeNamed(@RequestHeader(name = "id") String employeeId);

		@GetExchange
		void execute(@RequestHeader List<Object> multiValueHeader);

		@GetExchange
		void execute(@RequestHeader Object... multiValueHeader);

		@GetExchange
		void executeMap(@Nullable @RequestHeader Map<String, String> id);

		@GetExchange
		void executeOptionalMapValue(@RequestHeader Map<String, Optional<String>> headers);

		@GetExchange
		void executeNotRequired(@Nullable @RequestHeader(required = false) String id);

		@GetExchange
		void executeOptional(@RequestHeader Optional<Object> id);

		@GetExchange
		void executeWithDefaultValue(@Nullable @RequestHeader(defaultValue = "default") String id);

		@GetExchange
		void executeOptionalWithDefaultValue(@RequestHeader(defaultValue = "default") Optional<Object> id);

	}

}
