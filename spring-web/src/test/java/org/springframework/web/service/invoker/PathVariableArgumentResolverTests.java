/*
 * Copyright 2002-present the original author or authors.
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

import java.net.URI;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PathVariableArgumentResolver}.
 *
 * <p>For base class functionality, see {@link NamedValueArgumentResolverTests}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 */
class PathVariableArgumentResolverTests {

	private final TestExchangeAdapter client = new TestExchangeAdapter();

	private final Service service =
			HttpServiceProxyFactory.builderFor(this.client).build().createClient(Service.class);


	@Test
	void pathVariable() {
		this.service.execute("test");
		assertPathVariable("id", "test");
	}

	@Test // gh-34499
	void pathVariableAndRequestParamWithSameName() {
		this.service.executeWithPathVarAndRequestParam("{transfer-id}", "aValue");

		assertPathVariable("transfer-id", "{transfer-id}");

		HttpRequestValues values = this.client.getRequestValues();
		URI uri = (new DefaultUriBuilderFactory()).expand(values.getUriTemplate(), values.getUriVariables());
		assertThat(uri.toString()).isEqualTo("/transfers/%7Btransfer-id%7D?transfer-id=aValue");
	}

	@SuppressWarnings("SameParameterValue")
	private void assertPathVariable(String name, @Nullable String expectedValue) {
		assertThat(this.client.getRequestValues().getUriVariables().get(name)).isEqualTo(expectedValue);
	}


	private interface Service {

		@GetExchange
		void execute(@PathVariable String id);

		@GetExchange("/transfers/{transfer-id}")
		void executeWithPathVarAndRequestParam(
				@PathVariable("transfer-id") String transferId,
				@RequestParam("transfer-id") String transferIdParam);

	}

}
