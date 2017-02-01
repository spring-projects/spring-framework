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

package org.springframework.web.reactive.function.client;

import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.Assert;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

/**
 * Default implementation of {@link WebClient.Builder}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultWebClientBuilder implements WebClient.Builder {

	private UriBuilderFactory uriBuilderFactory;

	private ClientHttpConnector connector;

	private ExchangeStrategies exchangeStrategies = ExchangeStrategies.withDefaults();


	public DefaultWebClientBuilder(String baseUrl) {
		this(new DefaultUriBuilderFactory(baseUrl));
	}

	public DefaultWebClientBuilder(UriBuilderFactory uriBuilderFactory) {
		Assert.notNull(uriBuilderFactory, "UriBuilderFactory is required.");
		this.uriBuilderFactory = uriBuilderFactory;
	}


	@Override
	public WebClient.Builder clientConnector(ClientHttpConnector connector) {
		this.connector = connector;
		return this;
	}

	@Override
	public WebClient.Builder exchangeStrategies(ExchangeStrategies strategies) {
		Assert.notNull(strategies, "ExchangeStrategies is required.");
		this.exchangeStrategies = strategies;
		return this;
	}

	@Override
	public WebClient build() {
		ClientHttpConnector connector = this.connector != null ? this.connector : new ReactorClientHttpConnector();
		ExchangeFunction exchangeFunction = ExchangeFunctions.create(connector, this.exchangeStrategies);
		return new DefaultWebClient(exchangeFunction, this.uriBuilderFactory);
	}

}
