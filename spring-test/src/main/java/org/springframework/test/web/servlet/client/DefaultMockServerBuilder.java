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

package org.springframework.test.web.servlet.client;

import java.util.function.Consumer;

import org.springframework.test.web.servlet.MockMvcBuilder;

/**
 * Default implementation of {@link RestTestClient.MockServerBuilder}.
 * @author Rob Worsnop
 * @param <M> the type of the {@link MockMvcBuilder} to use for building the mock server
 */
class DefaultMockServerBuilder<M extends MockMvcBuilder>
		extends DefaultRestTestClientBuilder<RestTestClient.MockServerBuilder<M>>
		implements RestTestClient.MockServerBuilder<M> {

	private final M builder;

	public DefaultMockServerBuilder(M builder) {
		this.builder = builder;
	}

	@Override
	public RestTestClient.MockServerBuilder<M> configureServer(Consumer<M> consumer) {
		consumer.accept(this.builder);
		return this;
	}

	@Override
	public RestTestClient build() {
		this.restClientBuilder.requestFactory(new MockMvcClientHttpRequestFactory(this.builder.build()));
		return super.build();
	}
}
