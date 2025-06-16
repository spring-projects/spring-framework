/*
 * Copyright 2002-2025 the original author or authors.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.HttpComponentsClientHttpConnector;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ApiVersionInserter;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeFunctions;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

/**
 * Default implementation of {@link WebTestClient.Builder}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultWebTestClientBuilder implements WebTestClient.Builder {

	private static final boolean reactorNettyClientPresent;

	private static final boolean jettyClientPresent;

	private static final boolean httpComponentsClientPresent;

	private static final boolean webFluxPresent;

	static {
		ClassLoader loader = DefaultWebTestClientBuilder.class.getClassLoader();
		reactorNettyClientPresent = ClassUtils.isPresent("reactor.netty.http.client.HttpClient", loader);
		jettyClientPresent = ClassUtils.isPresent("org.eclipse.jetty.client.HttpClient", loader);
		httpComponentsClientPresent =
				ClassUtils.isPresent("org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient", loader) &&
						ClassUtils.isPresent("org.apache.hc.core5.reactive.ReactiveDataConsumer", loader);
		webFluxPresent = ClassUtils.isPresent(
				"org.springframework.web.reactive.function.client.ExchangeFunction", loader);
	}


	private final @Nullable WebHttpHandlerBuilder httpHandlerBuilder;

	private @Nullable ClientHttpConnector connector;

	private @Nullable String baseUrl;

	private @Nullable UriBuilderFactory uriBuilderFactory;

	private @Nullable HttpHeaders defaultHeaders;

	private @Nullable MultiValueMap<String, String> defaultCookies;

	private @Nullable Object defaultApiVersion;

	private @Nullable ApiVersionInserter apiVersionInserter;

	private @Nullable List<ExchangeFilterFunction> filters;

	private Consumer<EntityExchangeResult<?>> entityResultConsumer = result -> {};

	private @Nullable ExchangeStrategies strategies;

	private @Nullable List<Consumer<ExchangeStrategies.Builder>> strategiesConfigurers;

	private @Nullable Duration responseTimeout;


	/** Determine connector via classpath detection. */
	DefaultWebTestClientBuilder() {
		this(null, null);
	}

	/** Use HttpHandlerConnector with mock server. */
	DefaultWebTestClientBuilder(WebHttpHandlerBuilder httpHandlerBuilder) {
		this(httpHandlerBuilder, null);
	}

	/** Use given connector. */
	DefaultWebTestClientBuilder(ClientHttpConnector connector) {
		this(null, connector);
	}

	DefaultWebTestClientBuilder(
			@Nullable WebHttpHandlerBuilder httpHandlerBuilder, @Nullable ClientHttpConnector connector) {

		Assert.isTrue(httpHandlerBuilder == null || connector == null,
				"Expected WebHttpHandlerBuilder or ClientHttpConnector but not both.");

		// Helpful message especially for MockMvcWebTestClient users
		Assert.state(webFluxPresent,
				"To use WebTestClient, please add spring-webflux to the test classpath.");

		this.connector = connector;
		this.httpHandlerBuilder = (httpHandlerBuilder != null ? httpHandlerBuilder.clone() : null);
	}

	/** Copy constructor. */
	DefaultWebTestClientBuilder(DefaultWebTestClientBuilder other) {
		this.httpHandlerBuilder = (other.httpHandlerBuilder != null ? other.httpHandlerBuilder.clone() : null);
		this.connector = other.connector;
		this.responseTimeout = other.responseTimeout;

		this.baseUrl = other.baseUrl;
		this.uriBuilderFactory = other.uriBuilderFactory;
		if (other.defaultHeaders != null) {
			this.defaultHeaders = new HttpHeaders();
			this.defaultHeaders.putAll(other.defaultHeaders);
		}
		else {
			this.defaultHeaders = null;
		}
		this.defaultCookies = (other.defaultCookies != null ?
				new LinkedMultiValueMap<>(other.defaultCookies) : null);
		this.defaultApiVersion = other.defaultApiVersion;
		this.apiVersionInserter = other.apiVersionInserter;
		this.filters = (other.filters != null ? new ArrayList<>(other.filters) : null);
		this.entityResultConsumer = other.entityResultConsumer;
		this.strategies = other.strategies;
		this.strategiesConfigurers = (other.strategiesConfigurers != null ?
				new ArrayList<>(other.strategiesConfigurers) : null);
	}


	@Override
	public WebTestClient.Builder baseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
		return this;
	}

	@Override
	public WebTestClient.Builder uriBuilderFactory(UriBuilderFactory uriBuilderFactory) {
		this.uriBuilderFactory = uriBuilderFactory;
		return this;
	}

	@Override
	public WebTestClient.Builder defaultHeader(String header, String... values) {
		initHeaders().put(header, Arrays.asList(values));
		return this;
	}

	@Override
	public WebTestClient.Builder defaultHeaders(Consumer<HttpHeaders> headersConsumer) {
		headersConsumer.accept(initHeaders());
		return this;
	}

	private HttpHeaders initHeaders() {
		if (this.defaultHeaders == null) {
			this.defaultHeaders = new HttpHeaders();
		}
		return this.defaultHeaders;
	}

	@Override
	public WebTestClient.Builder defaultCookie(String cookie, String... values) {
		initCookies().addAll(cookie, Arrays.asList(values));
		return this;
	}

	@Override
	public WebTestClient.Builder defaultCookies(Consumer<MultiValueMap<String, String>> cookiesConsumer) {
		cookiesConsumer.accept(initCookies());
		return this;
	}

	private MultiValueMap<String, String> initCookies() {
		if (this.defaultCookies == null) {
			this.defaultCookies = new LinkedMultiValueMap<>(3);
		}
		return this.defaultCookies;
	}

	@Override
	public WebTestClient.Builder defaultApiVersion(Object version) {
		this.defaultApiVersion = version;
		return this;
	}

	@Override
	public WebTestClient.Builder apiVersionInserter(ApiVersionInserter apiVersionInserter) {
		this.apiVersionInserter = apiVersionInserter;
		return this;
	}

	@Override
	public WebTestClient.Builder filter(ExchangeFilterFunction filter) {
		Assert.notNull(filter, "ExchangeFilterFunction is required");
		initFilters().add(filter);
		return this;
	}

	@Override
	public WebTestClient.Builder filters(Consumer<List<ExchangeFilterFunction>> filtersConsumer) {
		filtersConsumer.accept(initFilters());
		return this;
	}

	private List<ExchangeFilterFunction> initFilters() {
		if (this.filters == null) {
			this.filters = new ArrayList<>();
		}
		return this.filters;
	}

	@Override
	public WebTestClient.Builder entityExchangeResultConsumer(Consumer<EntityExchangeResult<?>> entityResultConsumer) {
		Assert.notNull(entityResultConsumer, "'entityResultConsumer' is required");
		this.entityResultConsumer = this.entityResultConsumer.andThen(entityResultConsumer);
		return this;
	}

	@Override
	public WebTestClient.Builder codecs(Consumer<ClientCodecConfigurer> configurer) {
		if (this.strategiesConfigurers == null) {
			this.strategiesConfigurers = new ArrayList<>(4);
		}
		this.strategiesConfigurers.add(builder -> builder.codecs(configurer));
		return this;
	}

	@Override
	public WebTestClient.Builder exchangeStrategies(ExchangeStrategies strategies) {
		this.strategies = strategies;
		return this;
	}

	@Override
	public WebTestClient.Builder apply(WebTestClientConfigurer configurer) {
		configurer.afterConfigurerAdded(this, this.httpHandlerBuilder, this.connector);
		return this;
	}

	@Override
	public WebTestClient.Builder responseTimeout(Duration timeout) {
		this.responseTimeout = timeout;
		return this;
	}

	@Override
	public WebTestClient.Builder clientConnector(ClientHttpConnector connector) {
		this.connector = connector;
		return this;
	}

	@Override
	public WebTestClient build() {
		ClientHttpConnector connectorToUse = this.connector;
		if (connectorToUse == null) {
			if (this.httpHandlerBuilder != null) {
				connectorToUse = new HttpHandlerConnector(this.httpHandlerBuilder.build());
			}
		}
		if (connectorToUse == null) {
			connectorToUse = initConnector();
		}
		ExchangeStrategies exchangeStrategies = initExchangeStrategies();
		Function<ClientHttpConnector, ExchangeFunction> exchangeFactory = connector -> {
			ExchangeFunction exchange = ExchangeFunctions.create(connector, exchangeStrategies);
			if (CollectionUtils.isEmpty(this.filters)) {
				return exchange;
			}
			return this.filters.stream()
					.reduce(ExchangeFilterFunction::andThen)
					.map(filter -> filter.apply(exchange))
					.orElse(exchange);

		};
		return new DefaultWebTestClient(
				connectorToUse, exchangeStrategies, exchangeFactory, initUriBuilderFactory(),
				(this.defaultHeaders != null ? HttpHeaders.readOnlyHttpHeaders(this.defaultHeaders) : null),
				(this.defaultCookies != null ? CollectionUtils.unmodifiableMultiValueMap(this.defaultCookies) : null),
				this.defaultApiVersion, this.apiVersionInserter, this.entityResultConsumer,
				this.responseTimeout, new DefaultWebTestClientBuilder(this));
	}

	private static ClientHttpConnector initConnector() {
		if (reactorNettyClientPresent) {
			return new ReactorClientHttpConnector();
		}
		else if (jettyClientPresent) {
			return new JettyClientHttpConnector();
		}
		else if (httpComponentsClientPresent) {
			return new HttpComponentsClientHttpConnector();
		}
		else {
			return new JdkClientHttpConnector();
		}
	}

	private ExchangeStrategies initExchangeStrategies() {
		if (CollectionUtils.isEmpty(this.strategiesConfigurers)) {
			return (this.strategies != null ? this.strategies : ExchangeStrategies.withDefaults());
		}
		ExchangeStrategies.Builder builder =
				(this.strategies != null ? this.strategies.mutate() : ExchangeStrategies.builder());
		this.strategiesConfigurers.forEach(configurer -> configurer.accept(builder));
		return builder.build();
	}

	private UriBuilderFactory initUriBuilderFactory() {
		if (this.uriBuilderFactory != null) {
			return this.uriBuilderFactory;
		}
		return (this.baseUrl != null ?
				new DefaultUriBuilderFactory(this.baseUrl) : new DefaultUriBuilderFactory());
	}
}
