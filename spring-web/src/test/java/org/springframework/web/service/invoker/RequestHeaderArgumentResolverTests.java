/*
 * Copyright 2002-2023 the original author or authors.
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

import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RequestHeaderArgumentResolver}.
 * <p>For base class functionality, see {@link NamedValueArgumentResolverTests}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 */
class RequestHeaderArgumentResolverTests {

	private final TestExchangeAdapter client = new TestExchangeAdapter();

	private final Service service =
			HttpServiceProxyFactory.builderFor(this.client).build().createClient(Service.class);


	// Base class functionality should be tested in NamedValueArgumentResolverTests.

	@Test
	void header() {
		this.service.execute("test");
		assertRequestHeaders("id", "test");
	}

	private void assertRequestHeaders(String key, String... values) {
		List<String> actualValues = this.client.getRequestValues().getHeaders().get(key);
		if (ObjectUtils.isEmpty(values)) {
			assertThat(actualValues).isNull();
		}
		else {
			assertThat(actualValues).containsOnly(values);
		}
	}


	private interface Service {

		@GetExchange
		void execute(@RequestHeader String id);

	}

}
