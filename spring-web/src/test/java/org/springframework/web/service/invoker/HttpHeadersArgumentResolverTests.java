/*
 * Copyright 2002-2025 the original author or authors.
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

import org.springframework.http.HttpHeaders;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpHeadersArgumentResolver}.
 *
 * @author Yanming Zhou
 */
class HttpHeadersArgumentResolverTests {

	private final TestExchangeAdapter client = new TestExchangeAdapter();

	private final Service service =
			HttpServiceProxyFactory.builderFor(this.client).build().createClient(Service.class);

	@Test
	void headers() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("foo", "bar");
		headers.add("test", "testValue1");
		headers.add("test", "testValue2");
		this.service.execute(headers);

		HttpHeaders actualHeaders = this.client.getRequestValues().getHeaders();
		assertThat(actualHeaders.get("foo")).containsOnly("bar");
		assertThat(actualHeaders.get("test")).containsExactlyInAnyOrder("testValue1", "testValue2");
	}

	@Test
	void doesNotOverrideAnnotationHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("foo", "bar");
		this.service.executeWithAnnotationHeaders(headers);

		HttpHeaders actualHeaders = this.client.getRequestValues().getHeaders();
		assertThat(actualHeaders.get("foo")).containsExactlyInAnyOrder("foo", "bar");
		assertThat(actualHeaders.get("bar")).containsOnly("bar");
	}

	private interface Service {

		@GetExchange
		void execute(HttpHeaders headers);

		@HttpExchange(method = "GET", headers = {"foo=foo", "bar=bar"})
		void executeWithAnnotationHeaders(HttpHeaders headers);

	}

}
