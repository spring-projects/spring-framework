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

import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link HttpMethodArgumentResolver}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 */
class HttpMethodArgumentResolverTests {

	private final TestExchangeAdapter client = new TestExchangeAdapter();

	private final Service service =
			HttpServiceProxyFactory.builderFor(this.client).build().createClient(Service.class);


	@Test
	void httpMethodFromArgument() {
		this.service.execute(HttpMethod.POST);
		assertThat(getActualMethod()).isEqualTo(HttpMethod.POST);
	}

	@Test
	void httpMethodFromAnnotation() {
		this.service.executeHttpHead();
		assertThat(getActualMethod()).isEqualTo(HttpMethod.HEAD);
	}

	@Test
	void notHttpMethod() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.service.executeNotHttpMethod("test"))
				.withMessage("Could not resolve parameter [0] in " +
						"public abstract void org.springframework.web.service.invoker." +
						"HttpMethodArgumentResolverTests$Service.executeNotHttpMethod(java.lang.String): " +
						"No suitable resolver");
	}

	@Test
	void nullHttpMethod() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.service.execute(null));
	}

	@Test
	void nullHttpMethodWithNullable() {
		this.service.executeNullableHttpMethod(null);
		assertThat(getActualMethod()).isEqualTo(HttpMethod.GET);
	}

	@Test
	void nullHttpMethodWithOptional() {
		this.service.executeOptionalHttpMethod(null);
		assertThat(getActualMethod()).isEqualTo(HttpMethod.GET);
	}

	@Test
	void emptyOptionalHttpMethod() {
		this.service.executeOptionalHttpMethod(Optional.empty());
		assertThat(getActualMethod()).isEqualTo(HttpMethod.GET);
	}

	@Test
	void optionalHttpMethod() {
		this.service.executeOptionalHttpMethod(Optional.of(HttpMethod.POST));
		assertThat(getActualMethod()).isEqualTo(HttpMethod.POST);
	}


	@Nullable
	private HttpMethod getActualMethod() {
		return this.client.getRequestValues().getHttpMethod();
	}


	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	private interface Service {

		@HttpExchange
		void execute(HttpMethod method);

		@HttpExchange(method = "HEAD")
		void executeHttpHead();

		@GetExchange
		void executeNotHttpMethod(String test);

		@GetExchange
		void executeNullableHttpMethod(@Nullable HttpMethod method);

		@GetExchange
		void executeOptionalHttpMethod(Optional<HttpMethod> method);

	}

}
