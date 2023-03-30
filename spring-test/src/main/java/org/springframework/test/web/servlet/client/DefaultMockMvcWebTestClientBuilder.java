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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.test.web.reactive.server.DefaultWebTestClientBuilder;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClientConfigurer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient.Builder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeFunctions;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.util.UriBuilderFactory;

/**
 * Default implementation of {@link MockMvcWebTestClient.Builder}.
 *
 * @author Justin Tay
 */
public class DefaultMockMvcWebTestClientBuilder extends DefaultWebTestClientBuilder implements MockMvcWebTestClient.Builder {

	private MockMvc mockMvc;
	private List<RequestPostProcessor> requestPostProcessors = new ArrayList<>();
	private List<Consumer<MockHttpServletRequestBuilder>> requestBuilderCustomizers = new ArrayList<>();

	DefaultMockMvcWebTestClientBuilder(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	/** Copy constructor. */
	DefaultMockMvcWebTestClientBuilder(DefaultMockMvcWebTestClientBuilder other) {
		super(other);
		this.mockMvc = other.mockMvc;
		this.requestPostProcessors = new ArrayList<>(other.requestPostProcessors);
		this.requestBuilderCustomizers = new ArrayList<>(other.requestBuilderCustomizers);
	}

	@Override
	public Builder mockMvc(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
		return this;
	}

	@Override
	public Builder requestPostProcessors(Consumer<List<RequestPostProcessor>> requestPostProcessorsConsumer) {
		requestPostProcessorsConsumer.accept(this.requestPostProcessors);
		return this;
	}

	@Override
	public Builder requestPostProcessor(RequestPostProcessor requestPostProcessor) {
		this.requestPostProcessors.add(requestPostProcessor);
		return this;
	}

	@Override
	public Builder requestBuilderCustomizer(Consumer<MockHttpServletRequestBuilder> requestBuilderCustomizer) {
		this.requestBuilderCustomizers.add(requestBuilderCustomizer);
		return this;
	}

	@Override
	public Builder requestBuilderCustomizers(Consumer<List<Consumer<MockHttpServletRequestBuilder>>> requestBuilderCustomizersConsumer) {
		requestBuilderCustomizersConsumer.accept(this.requestBuilderCustomizers);
		return this;
	}

	@Override
	public Builder apply(MockMvcWebTestClientConfigurer configurer) {
		configurer.afterConfigurerAdded(this, this.httpHandlerBuilder, this.connector);
		return this;
	}

	@Override
	public MockMvcWebTestClient build() {
		ClientHttpConnector connectorToUse = new MockMvcHttpConnector(this.mockMvc, customizer -> {
			this.requestPostProcessors.forEach(customizer::with);
			this.requestBuilderCustomizers.forEach(builderCustomizer -> builderCustomizer.accept(customizer));
		});
		Function<ClientHttpConnector, ExchangeFunction> exchangeFactory = connector -> {
			ExchangeFunction exchange = ExchangeFunctions.create(connector, initExchangeStrategies());
			if (CollectionUtils.isEmpty(this.filters)) {
				return exchange;
			}
			return this.filters.stream()
					.reduce(ExchangeFilterFunction::andThen)
					.map(filter -> filter.apply(exchange))
					.orElse(exchange);

		};
		return new DefaultMockMvcWebTestClient(connectorToUse, exchangeFactory, initUriBuilderFactory(),
				this.defaultHeaders != null ? HttpHeaders.readOnlyHttpHeaders(this.defaultHeaders) : null,
				this.defaultCookies != null ? CollectionUtils.unmodifiableMultiValueMap(this.defaultCookies) : null,
				this.entityResultConsumer, this.responseTimeout, new DefaultMockMvcWebTestClientBuilder(this));
	}

	/* Override methods to return covariant return type */
	@Override
	public Builder baseUrl(String baseUrl) {
		super.baseUrl(baseUrl);
		return this;
	}

	@Override
	public Builder uriBuilderFactory(
			UriBuilderFactory uriBuilderFactory) {
		super.uriBuilderFactory(uriBuilderFactory);
		return this;
	}

	@Override
	public Builder defaultHeader(String header,
			String... values) {
		super.defaultHeader(header, values);
		return this;
	}

	@Override
	public Builder defaultHeaders(
			Consumer<HttpHeaders> headersConsumer) {
		super.defaultHeaders(headersConsumer);
		return this;
	}

	@Override
	public Builder defaultCookie(String cookie,
			String... values) {
		super.defaultCookie(cookie, values);
		return this;
	}

	@Override
	public Builder defaultCookies(
			Consumer<MultiValueMap<String, String>> cookiesConsumer) {
		super.defaultCookies(cookiesConsumer);
		return this;
	}

	@Override
	public Builder filter(ExchangeFilterFunction filter) {
		super.filter(filter);
		return this;
	}

	@Override
	public Builder filters(
			Consumer<List<ExchangeFilterFunction>> filtersConsumer) {
		super.filters(filtersConsumer);
		return this;
	}

	@Override
	public Builder entityExchangeResultConsumer(
			Consumer<EntityExchangeResult<?>> entityResultConsumer) {
		super.entityExchangeResultConsumer(entityResultConsumer);
		return this;
	}

	@Override
	public Builder codecs(
			Consumer<ClientCodecConfigurer> configurer) {
		super.codecs(configurer);
		return this;
	}

	@Override
	public Builder exchangeStrategies(
			ExchangeStrategies strategies) {
		super.exchangeStrategies(strategies);
		return this;
	}

	@Override
	@SuppressWarnings("deprecation")
	public Builder exchangeStrategies(
			Consumer<ExchangeStrategies.Builder> configurer) {
		super.exchangeStrategies(configurer);
		return this;
	}

	@Override
	public Builder apply(
			WebTestClientConfigurer configurer) {
		super.apply(configurer);
		return this;
	}

	@Override
	public Builder responseTimeout(Duration timeout) {
		super.responseTimeout(timeout);
		return this;
	}
}
