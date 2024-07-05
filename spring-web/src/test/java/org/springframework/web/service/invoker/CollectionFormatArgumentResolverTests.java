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

import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CollectionFormat;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.service.invoker.CollectionFormat.CSV;

/**
 * Tests for {@link RequestParamArgumentResolver}.
 *
 * <p>Additional tests for this resolver:
 * <ul>
 * <li>Base class functionality in {@link NamedValueArgumentResolverTests}
 * <li>Form data vs query params in {@link HttpRequestValuesTests}
 * </ul>
 *
 * @author Seokjae Lee
 */
class CollectionFormatArgumentResolverTests {

	private final TestExchangeAdapter client = new TestExchangeAdapter();

	private final Service service =
			HttpServiceProxyFactory.builderFor(this.client).build().createClient(Service.class);


	@Test
	@SuppressWarnings("unchecked")
	void requestParamWithCollectionFormat() {
		String param1 = "value 1";
		List<String> param2 = List.of("1", "2", "3");
		this.service.postForm(param1, param2);

		Object body = this.client.getRequestValues().getBodyValue();
		assertThat(body).isInstanceOf(MultiValueMap.class);
		assertThat((MultiValueMap<String, String>) body).hasSize(2)
				.containsEntry("param1", List.of(param1))
				.containsEntry("param2", List.of(String.join(",", param2)));
	}


	private interface Service {

		@PostExchange(contentType = "application/x-www-form-urlencoded")
		void postForm(@RequestParam String param1,
					  @RequestParam @CollectionFormat(CSV) List<String> param2);

	}

}
