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

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link UriBuilderFactoryArgumentResolver}.
 *
 * @author Olga Maciaszek-Sharma
 */
@SuppressWarnings({"DataFlowIssue", "OptionalAssignedToNull"})
class UriBuilderFactoryArgumentResolverTests {

	private final TestExchangeAdapter client = new TestExchangeAdapter();

	private final Service service =
			HttpServiceProxyFactory.builderFor(this.client).build().createClient(Service.class);


	@Test
	void uriBuilderFactory(){
		UriBuilderFactory factory = new DefaultUriBuilderFactory("https://example.com");
		this.service.execute(factory);

		assertThat(getRequestValues().getUriBuilderFactory()).isEqualTo(factory);
		assertThat(getRequestValues().getUriTemplate()).isEqualTo("/path");
		assertThat(getRequestValues().getUri()).isNull();
	}

	@Test
	void nullUriBuilderFactory() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.service.execute(null))
				.withMessage("UriBuilderFactory is required");
	}

	@Test
	void nullUriBuilderFactoryWithNullable(){
		this.service.executeNullable(null);

		assertThat(getRequestValues().getUriBuilderFactory()).isEqualTo(null);
		assertThat(getRequestValues().getUriTemplate()).isEqualTo("/path");
		assertThat(getRequestValues().getUri()).isNull();
	}

	@Test
	void nullUriBuilderFactoryWithOptional(){
		this.service.executeOptional(null);

		assertThat(getRequestValues().getUriBuilderFactory()).isEqualTo(null);
		assertThat(getRequestValues().getUriTemplate()).isEqualTo("/path");
		assertThat(getRequestValues().getUri()).isNull();
	}

	@Test
	void emptyOptionalUriBuilderFactory(){
		this.service.executeOptional(Optional.empty());

		assertThat(getRequestValues().getUriBuilderFactory()).isEqualTo(null);
		assertThat(getRequestValues().getUriTemplate()).isEqualTo("/path");
		assertThat(getRequestValues().getUri()).isNull();
	}

	@Test
	void optionalUriBuilderFactory(){
		UriBuilderFactory factory = new DefaultUriBuilderFactory("https://example.com");
		this.service.executeOptional(Optional.of(factory));

		assertThat(getRequestValues().getUriBuilderFactory()).isEqualTo(factory);
		assertThat(getRequestValues().getUriTemplate()).isEqualTo("/path");
		assertThat(getRequestValues().getUri()).isNull();
	}

	private HttpRequestValues getRequestValues() {
		return this.client.getRequestValues();
	}


	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	private interface Service {

		@GetExchange("/path")
		void execute(UriBuilderFactory uri);

		@GetExchange("/path")
		void executeNullable(@Nullable UriBuilderFactory uri);

		@GetExchange("/path")
		void executeOptional(Optional<UriBuilderFactory> uri);

	}
}
