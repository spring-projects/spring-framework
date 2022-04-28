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
 * Tests for {@link RequestHeaderArgumentResolver}.
 *
 * @author Olga Maciaszek-Sharma
 */
class RequestHeaderArgumentResolverTests {

	private final TestHttpClientAdapter clientAdapter = new TestHttpClientAdapter();

	private final Service service = this.clientAdapter.createService(Service.class);

	@Test
	void shouldResolveSingleValueRequestHeader() {
		this.service.executeString("test");
		assertRequestHeaders("id", "test");
	}

	@Test
	void shouldResolveRequestHeaderWithNameFromAnnotationName() {
		this.service.executeNamed("test");
		assertRequestHeaders("id", "test");
	}

	@Test
	void shouldResolveRequestHeaderNameFromValue() {
		this.service.executeNamedWithValue("test");
		assertRequestHeaders("test", "test");
	}

	@Test
	void shouldResolveObjectValueRequestHeader() {
		this.service.execute(Boolean.TRUE);
		assertRequestHeaders("id", "true");
	}

	@Test
	void shouldResolveListRequestHeader() {
		this.service.execute(List.of("test1", Boolean.TRUE, "test3"));
		assertRequestHeaders("id", "test1", "true", "test3");
	}

	@Test
	void shouldResolveArrayRequestHeader() {
		this.service.execute("test1", Boolean.FALSE, "test3");
		assertRequestHeaders("id", "test1", "false", "test3");
	}

	@Test
	void shouldResolveRequestHeadersFromMap() {
		this.service.executeMap(Maps.of(Boolean.TRUE, "true", Boolean.FALSE, "false"));
		assertRequestHeaders("true", "true");
		assertRequestHeaders("false", "false");
	}

	@Test
	void shouldThrowExceptionWhenRequiredHeaderNull() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.service.executeString(null));
	}

	@Test
	void shouldIgnoreNullWhenHeaderNotRequired() {
		this.service.executeNotRequired(null);
		assertThat(getActualHeaders().get("id")).isNull();
	}

	@Test
	void shouldIgnoreNullMapValue() {
		this.service.executeMap(null);
		assertThat(getActualHeaders()).isEmpty();
	}

	@Test
	void shouldResolveRequestHeaderFromOptionalArgumentWithConversion() {
		this.service.executeOptional(Optional.of(Boolean.TRUE));
		assertRequestHeaders("id", "true");
	}

	@Test
	void shouldResolveRequestHeaderFromOptionalArgument() {
		this.service.executeOptional(Optional.of("test"));
		assertRequestHeaders("id", "test");
	}

	@Test
	void shouldThrowExceptionForEmptyOptional() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.service.execute(Optional.empty()));
	}

	@Test
	void shouldIgnoreEmptyOptionalWhenNotRequired() {
		this.service.executeOptionalNotRequired(Optional.empty());
		assertThat(getActualHeaders().get("id")).isNull();
	}

	@Test
	void shouldResolveRequestHeaderFromOptionalMapValue() {
		this.service.executeOptionalMapValue(Map.of("id", Optional.of("test")));
		assertRequestHeaders("id", "test");
	}

	@Test
	void shouldReplaceNullValueWithDefaultWhenAvailable() {
		this.service.executeWithDefaultValue(null);
		assertRequestHeaders("id", "default");
	}

	@Test
	void shouldReplaceEmptyOptionalValueWithDefaultWhenAvailable() {
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
		void executeNotRequired(@Nullable @RequestHeader(required = false) String id);

		@GetExchange
		void execute(@RequestHeader Object id);

		@GetExchange
		void execute(@RequestHeader List<Object> id);

		@GetExchange
		void execute(@RequestHeader Object... id);

		@GetExchange
		void executeMap(@Nullable @RequestHeader Map<Object, String> id);

		@GetExchange
		void executeOptionalMapValue(@RequestHeader Map<Object, Optional<String>> id);

		@GetExchange
		void executeOptional(@RequestHeader Optional<Object> id);

		@GetExchange
		void executeOptionalNotRequired(@RequestHeader(required = false) Optional<String> id);

		@GetExchange
		void executeNamedWithValue(@Nullable @RequestHeader(name = "id", value = "test") String employeeId);

		@GetExchange
		void executeNamed(@RequestHeader(name = "id") String employeeId);

		@GetExchange
		void executeWithDefaultValue(@Nullable @RequestHeader(defaultValue = "default") String id);

		@GetExchange
		void executeOptionalWithDefaultValue(@Nullable @RequestHeader(defaultValue = "default") Optional<Object> id);
	}

}
