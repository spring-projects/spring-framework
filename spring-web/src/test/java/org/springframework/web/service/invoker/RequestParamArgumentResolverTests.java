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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.PostExchange;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RequestParamArgumentResolver}.
 *
 * <p>Additional tests for this resolver:
 * <ul>
 * <li>Base class functionality in {@link NamedValueArgumentResolverTests}
 * <li>Form data vs query params in {@link HttpRequestValuesTests}
 * </ul>
 *
 * @author Rossen Stoyanchev
 */
public class RequestParamArgumentResolverTests {

	private final TestHttpClientAdapter client = new TestHttpClientAdapter();

	private Service service;


	@BeforeEach
	void setUp() throws Exception {
		HttpServiceProxyFactory proxyFactory = new HttpServiceProxyFactory(this.client);
		proxyFactory.afterPropertiesSet();
		this.service = proxyFactory.createClient(Service.class);
	}


	// Base class functionality should be tested in NamedValueArgumentResolverTests.
	// Form data vs query params tested in HttpRequestValuesTests.

	@Test
	void requestParam() {
		this.service.postForm("value 1", "value 2");

		Object body = this.client.getRequestValues().getBodyValue();
		assertThat(body).isNotNull().isInstanceOf(byte[].class);
		assertThat(new String((byte[]) body, UTF_8)).isEqualTo("param1=value+1&param2=value+2");
	}


	private interface Service {

		@PostExchange(contentType = "application/x-www-form-urlencoded")
		void postForm(@RequestParam String param1, @RequestParam String param2);

	}

}
