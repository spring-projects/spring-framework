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

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.GetMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for custom {@link HttpRequestValuesResolver}.
 *
 * @author Freeman Lau
 */
class CustomHttpRequestValuesResolverTests {

	private final TestHttpClientAdapter client = new TestHttpClientAdapter();

	@Test
	void testCustomHttpRequestValuesResolver() {
		HttpServiceProxyFactory factory = HttpServiceProxyFactory.builder(client)
				.customRequestValuesResolver(new CustomHttpRequestValuesResolver())
				.build();
		Service service = factory.createClient(Service.class);
		service.execute();

		assertThat(client.getRequestValues().getHttpMethod()).isEqualTo(HttpMethod.GET);
		assertThat(client.getRequestValues().getUriTemplate()).isNotNull();
		assertThat(client.getRequestValues().getUriTemplate()).hasToString("/execute");
	}

	private static final class CustomHttpRequestValuesResolver
			implements HttpRequestValuesResolver {

		@Override
		public boolean supports(Method method) {
			return AnnotatedElementUtils.hasAnnotation(method, GetMapping.class);
		}

		@Override
		public HttpRequestValuesInitializer resolve(Method method, Class<?> serviceType) {
			GetMapping anno = AnnotatedElementUtils.findMergedAnnotation(method,
					GetMapping.class);

			String url = anno.value()[0];
			return new HttpRequestValuesInitializer(HttpMethod.GET, url, null, null);
		}

	}

	private interface Service {

		@GetMapping("/execute")
		void execute();

	}

}
