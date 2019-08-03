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

package org.springframework.web.reactive.function.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

/**
 * Default implementation of {@link WebClient.Builder}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
final class DefaultWebClientBuilder implements WebClient.Builder {

	@Nullable
	private String baseUrl;

	@Nullable
	private Map<String, ?> defaultUriVariables;

	@Nullable
	private UriBuilderFactory uriBuilderFactory;

	@Nullable
	private HttpHeaders defaultHeaders;

	@Nullable
	private MultiValueMap<String, String> defaultCookies;

	@Nullable
	private Consumer<WebClient.RequestHeadersSpec<?>> defaultRequest;

	@Nullable
	private List<ExchangeFilterFunction> filters;

	@Nullable
	private ClientHttpConnector connector;

	private ExchangeStrategies exchangeStrategies;

	@Nullable
	private ExchangeFunction exchangeFunction;


	public DefaultWebClientBuilder() {
		this.exchangeStrategies = ExchangeStrategies.withDefaults();
	}

	public DefaultWebClientBuilder(DefaultWebClientBuilder other) {
		Assert.notNull(other, "DefaultWebClientBuilder must not be null");

		this.baseUrl = other.baseUrl;
		this.defaultUriVariables = (other.defaultUriVariables != null ?
				new LinkedHashMap<>(other.defaultUriVariables) : null);
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
		this.defaultRequest = other.defaultRequest;
		this.filters = other.filters != null ? new ArrayList<>(other.filters) : null;
		this.connector = other.connector;
		this.exchangeStrategies = other.exchangeStrategies;
		this.exchangeFunction = other.exchangeFunction;
	}


	@Override
	public WebClient.Builder baseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
		return this;
	}

	@Override
	public WebClient.Builder defaultUriVariables(Map<String, ?> defaultUriVariables) {
		this.defaultUriVariables = defaultUriVariables;
		return this;
	}

	@Override
	public WebClient.Builder uriBuilderFactory(UriBuilderFactory uriBuilderFactory) {
		this.uriBuilderFactory = uriBuilderFactory;
		return this;
	}

	@Override
	public WebClient.Builder defaultHeader(String header, String... values) {
		initHeaders().put(header, Arrays.asList(values));
		return this;
	}

	@Override
	public WebClient.Builder defaultHeaders(Consumer<HttpHeaders> headersConsumer) {
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
	public WebClient.Builder defaultCookie(String cookie, String... values) {
		initCookies().addAll(cookie, Arrays.asList(values));
		return this;
	}

	@Override
	public WebClient.Builder defaultCookies(Consumer<MultiValueMap<String, String>> cookiesConsumer) {
		cookiesConsumer.accept(initCookies());
		return this;
	}

	private MultiValueMap<String, String> initCookies() {
		if (this.defaultCookies == null) {
			this.defaultCookies = new LinkedMultiValueMap<>(4);
		}
		return this.defaultCookies;
	}

	@Override
	public WebClient.Builder defaultRequest(Consumer<WebClient.RequestHeadersSpec<?>> defaultRequest) {
		this.defaultRequest = this.defaultRequest != null ?
				this.defaultRequest.andThen(defaultRequest) : defaultRequest;
		return this;
	}

	@Override
	public WebClient.Builder filter(ExchangeFilterFunction filter) {
		Assert.notNull(filter, "ExchangeFilterFunction must not be null");
		initFilters().add(filter);
		return this;
	}

	@Override
	public WebClient.Builder filters(Consumer<List<ExchangeFilterFunction>> filtersConsumer) {
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
	public WebClient.Builder clientConnector(ClientHttpConnector connector) {
		this.connector = connector;
		return this;
	}

	@Override
	public WebClient.Builder exchangeStrategies(ExchangeStrategies strategies) {
		Assert.notNull(strategies, "ExchangeStrategies must not be null");
		this.exchangeStrategies = strategies;
		return this;
	}

	@Override
	public WebClient.Builder exchangeFunction(ExchangeFunction exchangeFunction) {
		this.exchangeFunction = exchangeFunction;
		return this;
	}

	@Override
	public WebClient.Builder apply(Consumer<WebClient.Builder> builderConsumer) {
		builderConsumer.accept(this);
		return this;
	}

	@Override
	public WebClient.Builder clone() {
		return new DefaultWebClientBuilder(this);
	}

	@Override
	public WebClient build() {
		ExchangeFunction exchange = initExchangeFunction();
		ExchangeFunction filteredExchange = (this.filters != null ? this.filters.stream()
				.reduce(ExchangeFilterFunction::andThen)
				.map(filter -> filter.apply(exchange))
				.orElse(exchange) : exchange);
		return new DefaultWebClient(filteredExchange, initUriBuilderFactory(),
				this.defaultHeaders != null ? unmodifiableCopy(this.defaultHeaders) : null,
				this.defaultCookies != null ? unmodifiableCopy(this.defaultCookies) : null,
				this.defaultRequest, new DefaultWebClientBuilder(this));
	}

	private ExchangeFunction initExchangeFunction() {
		if (this.exchangeFunction != null) {
			return this.exchangeFunction;
		}
		else if (this.connector != null) {
			return ExchangeFunctions.create(this.connector, this.exchangeStrategies);
		}
		else {
			return ExchangeFunctions.create(new ReactorClientHttpConnector(), this.exchangeStrategies);
		}
	}

	private UriBuilderFactory initUriBuilderFactory() {
		if (this.uriBuilderFactory != null) {
			return this.uriBuilderFactory;
		}
		DefaultUriBuilderFactory factory = this.baseUrl != null ?
				new DefaultUriBuilderFactory(this.baseUrl) : new DefaultUriBuilderFactory();
		factory.setDefaultUriVariables(this.defaultUriVariables);
		return factory;
	}

	private static HttpHeaders unmodifiableCopy(HttpHeaders headers) {
		return HttpHeaders.readOnlyHttpHeaders(headers);
	}

	private static <K, V> MultiValueMap<K, V> unmodifiableCopy(MultiValueMap<K, V> map) {
		return CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<>(map));
	}

}
