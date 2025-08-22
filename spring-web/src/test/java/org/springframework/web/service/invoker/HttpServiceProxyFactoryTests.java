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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.service.annotation.GetExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link HttpServiceProxyFactory}.
 *
 * @author Rossen Stoyanchev
 */
public class HttpServiceProxyFactoryTests {

	@Test
	void httpExchangeAdapterDecorator() {

		HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(mock(HttpExchangeAdapter.class))
				.exchangeAdapterDecorator(TestDecorator::new)
				.build();

		Service service = factory.createClient(Service.class);
		assertThat(service.execute()).isEqualTo("decorated");
	}


	private interface Service {

		@GetExchange
		String execute();
	}


	private static class TestDecorator extends HttpExchangeAdapterDecorator {

		public TestDecorator(HttpExchangeAdapter delegate) {
			super(delegate);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> @Nullable T exchangeForBody(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
			return (T) "decorated";
		}
	}

}
