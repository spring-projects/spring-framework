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

package org.springframework.test.web.servlet.client;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.test.web.reactive.server.DefaultWebTestClient;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.util.UriBuilderFactory;

/**
 * Default implementation of {@link MockMvcWebTestClient}.
 *
 * @author Justin Tay
 */
public class DefaultMockMvcWebTestClient extends DefaultWebTestClient implements MockMvcWebTestClient {
	private final DefaultMockMvcWebTestClientBuilder builder;

	DefaultMockMvcWebTestClient(ClientHttpConnector connector,
			Function<ClientHttpConnector, ExchangeFunction> exchangeFactory, UriBuilderFactory uriBuilderFactory,
			HttpHeaders headers, MultiValueMap<String, String> cookies,
			Consumer<EntityExchangeResult<?>> entityResultConsumer, Duration responseTimeout,
			DefaultMockMvcWebTestClientBuilder clientBuilder) {
		super(connector, exchangeFactory, uriBuilderFactory, headers, cookies, entityResultConsumer, responseTimeout,
				clientBuilder);
		this.builder = clientBuilder;
	}

	@Override
	public MockMvcWebTestClient.Builder mutate() {
		return new DefaultMockMvcWebTestClientBuilder(this.builder);
	}

	@Override
	public MockMvcWebTestClient mutateWith(RequestPostProcessor requestPostProcessor) {
		return mutate().requestPostProcessor(requestPostProcessor).build();
	}

	@Override
	public MockMvcWebTestClient mutateWith(MockMvcWebTestClientConfigurer configurer) {
		return mutate().apply(configurer).build();
	}
}
