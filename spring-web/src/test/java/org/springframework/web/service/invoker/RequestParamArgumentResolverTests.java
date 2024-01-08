/*
 * Copyright 2002-2024 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.PostExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RequestParamArgumentResolver}.
 *
 * <p>Additional tests for this resolver:
 * <ul>
 * <li>Base class functionality in {@link NamedValueArgumentResolverTests}
 * <li>Form data vs query params in {@link HttpRequestValuesTests}
 * </ul>
 *
 * @author Rossen Stoyanchev
 */
class RequestParamArgumentResolverTests {

	private final TestExchangeAdapter client = new TestExchangeAdapter();

	private final Service service =
			HttpServiceProxyFactory.builderFor(this.client).build().createClient(Service.class);


	@Test
	@SuppressWarnings("unchecked")
	void requestParam() {
		this.service.postForm("value 1", "value 2");

		Object body = this.client.getRequestValues().getBodyValue();
		assertThat(body).isInstanceOf(MultiValueMap.class);
		assertThat((MultiValueMap<String, String>) body).hasSize(2)
				.containsEntry("param1", List.of("value 1"))
				.containsEntry("param2", List.of("value 2"));
	}


	private interface Service {

		@PostExchange(contentType = "application/x-www-form-urlencoded")
		void postForm(@RequestParam String param1, @RequestParam String param2);

	}

}
