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
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.service.annotation.GetExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;


/**
 * Unit tests for {@link RequestHeaderArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
class CookieValueArgumentResolverTests {

	private final TestHttpClientAdapter clientAdapter = new TestHttpClientAdapter();

	private final Service service = this.clientAdapter.createService(Service.class);


	@Test
	void stringCookie() {
		this.service.executeString("test");
		assertCookie("cookie", "test");
	}

	@Test
	void objectCookie() {
		this.service.execute(Boolean.TRUE);
		assertCookie("cookie", "true");
	}

	@Test
	void listCookie() {
		this.service.executeList(List.of("test1", Boolean.TRUE, "test3"));
		assertCookie("multiValueCookie", "test1", "true", "test3");
	}

	@Test
	void arrayCookie() {
		this.service.executeArray("test1", Boolean.FALSE, "test3");
		assertCookie("multiValueCookie", "test1", "false", "test3");
	}

	@Test
	void namedCookie() {
		this.service.executeNamed("test");
		assertCookie("cookieRenamed", "test");
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	void nullCookieRequired() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.service.executeString(null));
	}

	@Test
	void nullCookieNotRequired() {
		this.service.executeNotRequired(null);
		assertCookie("cookie");
	}

	@Test
	void nullCookieWithDefaultValue() {
		this.service.executeWithDefaultValue(null);
		assertCookie("cookie", "default");
	}

	@Test
	void optionalStringCookie() {
		this.service.executeOptional(Optional.of("test"));
		assertCookie("cookie", "test");
	}

	@Test
	void optionalObjectCookie() {
		this.service.executeOptional(Optional.of(Boolean.TRUE));
		assertCookie("cookie", "true");
	}

	@Test
	void optionalEmpty() {
		this.service.executeOptional(Optional.empty());
		assertCookie("cookie");
	}

	@Test
	void optionalEmpthyWithDefaultValue() {
		this.service.executeOptionalWithDefaultValue(Optional.empty());
		assertCookie("cookie", "default");
	}

	@Test
	void mapOfCookies() {
		this.service.executeMap(Maps.of("cookie1", "true", "cookie2", "false"));
		assertCookie("cookie1", "true");
		assertCookie("cookie2", "false");
	}

	@Test
	void mapOfCookiesIsNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.service.executeMap(null));
	}

	@Test
	void mapOfCookiesHasOptionalValue() {
		this.service.executeMapWithOptionalValue(Map.of("cookie", Optional.of("test")));
		assertCookie("cookie", "test");
	}

	private void assertCookie(String key, String... values) {
		List<String> actualValues = this.clientAdapter.getRequestValues().getCookies().get(key);
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
		void executeString(@CookieValue String cookie);

		@GetExchange
		void execute(@CookieValue Object cookie);

		@GetExchange
		void executeList(@CookieValue List<Object> multiValueCookie);

		@GetExchange
		void executeArray(@CookieValue Object... multiValueCookie);

		@GetExchange
		void executeNamed(@CookieValue(name = "cookieRenamed") String cookie);

		@GetExchange
		void executeNotRequired(@Nullable @CookieValue(required = false) String cookie);

		@GetExchange
		void executeWithDefaultValue(@Nullable @CookieValue(defaultValue = "default") String cookie);

		@GetExchange
		void executeOptional(@CookieValue Optional<Object> cookie);

		@GetExchange
		void executeOptionalWithDefaultValue(@CookieValue(defaultValue = "default") Optional<Object> cookie);

		@GetExchange
		void executeMap(@Nullable @CookieValue Map<String, String> cookie);

		@GetExchange
		void executeMapWithOptionalValue(@CookieValue Map<String, Optional<String>> cookies);

	}

}
