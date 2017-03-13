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

import java.time.Duration;

import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilderFactory;

/**
 * Default implementation of {@link WebTestClient.Builder}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultWebTestClientBuilder implements WebTestClient.Builder {

	private final WebClient.Builder webClientBuilder = WebClient.builder();

	private final ClientHttpConnector connector;

	private final ExchangeMutatorWebFilter exchangeMutatorFilter;

	private Duration responseTimeout;


	DefaultWebTestClientBuilder() {
		this(new ReactorClientHttpConnector());
	}

	DefaultWebTestClientBuilder(ClientHttpConnector connector) {
		this.connector = connector;
		this.exchangeMutatorFilter = null;
	}

	DefaultWebTestClientBuilder(HttpHandler httpHandler, ExchangeMutatorWebFilter exchangeMutatorFilter) {
		this.connector = new HttpHandlerConnector(httpHandler);
		this.exchangeMutatorFilter = exchangeMutatorFilter;
	}


	@Override
	public WebTestClient.Builder baseUrl(String baseUrl) {
		this.webClientBuilder.baseUrl(baseUrl);
		return this;
	}

	@Override
	public WebTestClient.Builder uriBuilderFactory(UriBuilderFactory uriBuilderFactory) {
		this.webClientBuilder.uriBuilderFactory(uriBuilderFactory);
		return this;
	}

	@Override
	public WebTestClient.Builder defaultHeader(String headerName, String... headerValues) {
		this.webClientBuilder.defaultHeader(headerName, headerValues);
		return this;
	}

	@Override
	public WebTestClient.Builder defaultCookie(String cookieName, String... cookieValues) {
		this.webClientBuilder.defaultCookie(cookieName, cookieValues);
		return this;
	}

	@Override
	public WebTestClient.Builder exchangeStrategies(ExchangeStrategies strategies) {
		this.webClientBuilder.exchangeStrategies(strategies);
		return this;
	}

	@Override
	public WebTestClient.Builder responseTimeout(Duration timeout) {
		this.responseTimeout = timeout;
		return this;
	}

	@Override
	public WebTestClient build() {
		return new DefaultWebTestClient(this.webClientBuilder, this.connector,
				this.exchangeMutatorFilter, this.responseTimeout);
	}

}
