/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.web.reactive.server;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.util.UriBuilderFactory;

/**
 * Default implementation of {@link WebTestClient.Builder}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultWebTestClientBuilder implements WebTestClient.Builder {

	private final WebClient.Builder webClientBuilder;

	@Nullable
	private final WebHttpHandlerBuilder httpHandlerBuilder;

	@Nullable
	private final ClientHttpConnector connector;

	@Nullable
	private Duration responseTimeout;


	/** Connect to server via Reactor Netty. */
	DefaultWebTestClientBuilder() {
		this(new ReactorClientHttpConnector());
	}

	/** Connect to server through the given connector. */
	DefaultWebTestClientBuilder(ClientHttpConnector connector) {
		this(null, null, connector, null);
	}

	/** Connect to given mock server with mock request and response. */
	DefaultWebTestClientBuilder(WebHttpHandlerBuilder httpHandlerBuilder) {
		this(null, httpHandlerBuilder, null, null);
	}

	/** Copy constructor. */
	DefaultWebTestClientBuilder(DefaultWebTestClientBuilder other) {
		this(other.webClientBuilder.clone(), other.httpHandlerBuilder, other.connector,
				other.responseTimeout);
	}

	private DefaultWebTestClientBuilder(@Nullable WebClient.Builder webClientBuilder,
			@Nullable WebHttpHandlerBuilder httpHandlerBuilder, @Nullable ClientHttpConnector connector,
			@Nullable Duration responseTimeout) {

		Assert.isTrue(httpHandlerBuilder != null || connector != null,
				"Either WebHttpHandlerBuilder or ClientHttpConnector must be provided");

		this.webClientBuilder = (webClientBuilder != null ? webClientBuilder : WebClient.builder());
		this.httpHandlerBuilder = (httpHandlerBuilder != null ? httpHandlerBuilder.clone() : null);
		this.connector = connector;
		this.responseTimeout = responseTimeout;
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
	public WebTestClient.Builder defaultHeaders(Consumer<HttpHeaders> headersConsumer) {
		this.webClientBuilder.defaultHeaders(headersConsumer);
		return this;
	}

	@Override
	public WebTestClient.Builder defaultCookie(String cookieName, String... cookieValues) {
		this.webClientBuilder.defaultCookie(cookieName, cookieValues);
		return this;
	}

	@Override
	public WebTestClient.Builder defaultCookies(
			Consumer<MultiValueMap<String, String>> cookiesConsumer) {
		this.webClientBuilder.defaultCookies(cookiesConsumer);
		return this;
	}

	@Override
	public WebTestClient.Builder filter(ExchangeFilterFunction filter) {
		this.webClientBuilder.filter(filter);
		return this;
	}

	@Override
	public WebTestClient.Builder filters(Consumer<List<ExchangeFilterFunction>> filtersConsumer) {
		this.webClientBuilder.filters(filtersConsumer);
		return this;
	}

	@Override
	public WebTestClient.Builder codecs(Consumer<ClientCodecConfigurer> configurer) {
		this.webClientBuilder.codecs(configurer);
		return this;
	}

	@Override
	public WebTestClient.Builder exchangeStrategies(ExchangeStrategies strategies) {
		this.webClientBuilder.exchangeStrategies(strategies);
		return this;
	}

	@SuppressWarnings("deprecation")
	@Override
	public WebTestClient.Builder exchangeStrategies(Consumer<ExchangeStrategies.Builder> configurer) {
		this.webClientBuilder.exchangeStrategies(configurer);
		return this;
	}

	@Override
	public WebTestClient.Builder responseTimeout(Duration timeout) {
		this.responseTimeout = timeout;
		return this;
	}

	@Override
	public WebTestClient.Builder apply(WebTestClientConfigurer configurer) {
		configurer.afterConfigurerAdded(this, this.httpHandlerBuilder, this.connector);
		return this;
	}


	@Override
	public WebTestClient build() {
		ClientHttpConnector connectorToUse = this.connector;
		if (connectorToUse == null) {
			Assert.state(this.httpHandlerBuilder != null, "No WebHttpHandlerBuilder available");
			connectorToUse = new HttpHandlerConnector(this.httpHandlerBuilder.build());
		}

		return new DefaultWebTestClient(this.webClientBuilder,
				connectorToUse, this.responseTimeout, new DefaultWebTestClientBuilder(this));
	}

}
