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

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests for {@link HttpMethodArgumentResolver}.
 *
 * @author Olga Maciaszek-Sharma
 */
public class HttpMethodArgumentResolverTests {

	private final TestHttpClientAdapter clientAdapter = new TestHttpClientAdapter();

	private final Service service = this.clientAdapter.createService(Service.class, new HttpMethodArgumentResolver());


	@Test
	void shouldResolveRequestMethodFromArgument() {
		this.service.execute(HttpMethod.GET);
		assertThat(getActualMethod()).isEqualTo(HttpMethod.GET);
	}

	@Test
	void shouldIgnoreArgumentsNotMatchingType() {
		this.service.execute("test");
		assertThat(getActualMethod()).isNull();
	}

	@Test
	void shouldOverrideMethodAnnotation() {
		this.service.executeGet(HttpMethod.POST);
		assertThat(getActualMethod()).isEqualTo(HttpMethod.POST);
	}

	@Test
	void shouldIgnoreNullValue() {
		this.service.executeForNull(null);
		assertThat(getActualMethod()).isNull();
	}

	@Nullable
	private HttpMethod getActualMethod() {
		return this.clientAdapter.getRequestSpec().getHttpMethod();
	}


	private interface Service {

		@HttpExchange
		void execute(HttpMethod method);

		@GetExchange
		void executeGet(HttpMethod method);

		@HttpExchange
		void execute(String test);

		@HttpExchange
		void execute(HttpMethod firstMethod, HttpMethod secondMethod);

		@HttpExchange
		void executeForNull(@Nullable HttpMethod method);
	}

}
