/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.test.web.reactive.server;

import java.util.function.UnaryOperator;

import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * Base class for implementations of {@link WebTestClient.MockServerSpec}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
abstract class AbstractMockServerSpec<B extends WebTestClient.MockServerSpec<B>>
		implements WebTestClient.MockServerSpec<B> {

	private final ExchangeMutatorWebFilter exchangeMutatorFilter = new ExchangeMutatorWebFilter();


	@Override
	public <T extends B> T exchangeMutator(UnaryOperator<ServerWebExchange> mutator) {
		this.exchangeMutatorFilter.register(mutator);
		return self();
	}

	@SuppressWarnings("unchecked")
	private <T extends B> T self() {
		return (T) this;
	}


	@Override
	public WebTestClient.Builder configureClient() {
		HttpHandler handler = initHttpHandlerBuilder().prependFilter(this.exchangeMutatorFilter).build();
		return new DefaultWebTestClientBuilder(handler, this.exchangeMutatorFilter);
	}

	protected abstract WebHttpHandlerBuilder initHttpHandlerBuilder();

	@Override
	public WebTestClient build() {
		return configureClient().build();
	}

}
