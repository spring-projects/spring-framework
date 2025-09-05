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

import java.util.List;
import java.util.function.Consumer;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.client.RestTestClient.MockMvcSetupBuilder;
import org.springframework.test.web.servlet.client.RestTestClient.RouterFunctionSetupBuilder;
import org.springframework.test.web.servlet.client.RestTestClient.StandaloneSetupBuilder;
import org.springframework.test.web.servlet.client.RestTestClient.WebAppContextSetupBuilder;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.RouterFunctionMockMvcBuilder;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ApiVersionInserter;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.util.UriBuilderFactory;

/**
 * Default implementation of {@link RestTestClient.Builder}.
 *
 * @author Rob Worsnop
 * @author Rossen Stoyanchev
 * @since 7.0
 * @param <B> the type of the builder
 */
class DefaultRestTestClientBuilder<B extends RestTestClient.Builder<B>> implements RestTestClient.Builder<B> {

	private final RestClient.Builder restClientBuilder;

	private Consumer<EntityExchangeResult<?>> entityResultConsumer = result -> {};


	DefaultRestTestClientBuilder() {
		this(RestClient.builder());
	}

	DefaultRestTestClientBuilder(RestClient.Builder restClientBuilder) {
		this.restClientBuilder = restClientBuilder;
	}

	DefaultRestTestClientBuilder(DefaultRestTestClientBuilder<B> other) {
		this.restClientBuilder = other.restClientBuilder.clone();
		this.entityResultConsumer = other.entityResultConsumer;
	}


	@Override
	public <T extends B> T baseUrl(String baseUrl) {
		this.restClientBuilder.baseUrl(baseUrl);
		return self();
	}

	@Override
	public <T extends B> T uriBuilderFactory(UriBuilderFactory uriFactory) {
		this.restClientBuilder.uriBuilderFactory(uriFactory);
		return self();
	}

	@Override
	public <T extends B> T defaultHeader(String headerName, String... headerValues) {
		this.restClientBuilder.defaultHeader(headerName, headerValues);
		return self();
	}

	@Override
	public <T extends B> T defaultHeaders(Consumer<HttpHeaders> headersConsumer) {
		this.restClientBuilder.defaultHeaders(headersConsumer);
		return self();
	}

	@Override
	public <T extends B> T defaultCookie(String cookieName, String... cookieValues) {
		this.restClientBuilder.defaultCookie(cookieName, cookieValues);
		return self();
	}

	@Override
	public <T extends B> T defaultCookies(Consumer<MultiValueMap<String, String>> cookiesConsumer) {
		this.restClientBuilder.defaultCookies(cookiesConsumer);
		return self();
	}

	@Override
	public <T extends B> T defaultApiVersion(Object version) {
		this.restClientBuilder.defaultApiVersion(version);
		return self();
	}

	@Override
	public <T extends B> T apiVersionInserter(ApiVersionInserter apiVersionInserter) {
		this.restClientBuilder.apiVersionInserter(apiVersionInserter);
		return self();
	}

	@Override
	public <T extends B> T requestInterceptor(ClientHttpRequestInterceptor interceptor) {
		this.restClientBuilder.requestInterceptor(interceptor);
		return self();
	}

	@Override
	public <T extends B> T requestInterceptors(Consumer<List<ClientHttpRequestInterceptor>> interceptorsConsumer) {
		this.restClientBuilder.requestInterceptors(interceptorsConsumer);
		return self();
	}

	@Override
	public <T extends B> T configureMessageConverters(Consumer<HttpMessageConverters.ClientBuilder> configurer) {
		this.restClientBuilder.configureMessageConverters(configurer);
		return self();
	}

	@Override
	public <T extends B> T entityExchangeResultConsumer(Consumer<EntityExchangeResult<?>> entityResultConsumer) {
		Assert.notNull(entityResultConsumer, "'entityResultConsumer' is required");
		this.entityResultConsumer = this.entityResultConsumer.andThen(entityResultConsumer);
		return self();
	}

	@SuppressWarnings("unchecked")
	protected <T extends B> T self() {
		return (T) this;
	}

	protected void setClientHttpRequestFactory(ClientHttpRequestFactory requestFactory) {
		this.restClientBuilder.requestFactory(requestFactory);
	}

	@Override
	public RestTestClient build() {
		return new DefaultRestTestClient(
				this.restClientBuilder, this.entityResultConsumer, new DefaultRestTestClientBuilder<>(this));
	}


	/**
	 * Base class for implementations for {@link MockMvcSetupBuilder}.
	 * @param <S> the "self" type of the builder
	 * @param <M> the type of {@link MockMvc} builder
	 */
	static class AbstractMockMvcSetupBuilder<S extends RestTestClient.Builder<S>, M extends MockMvcBuilder>
			extends DefaultRestTestClientBuilder<S> implements MockMvcSetupBuilder<S, M> {

		private final M mockMvcBuilder;

		public AbstractMockMvcSetupBuilder(M mockMvcBuilder) {
			this.mockMvcBuilder = mockMvcBuilder;
		}

		public <T extends S> T configureServer(Consumer<M> consumer) {
			consumer.accept(this.mockMvcBuilder);
			return self();
		}

		@Override
		public RestTestClient build() {
			MockMvc mockMvc = this.mockMvcBuilder.build();
			setClientHttpRequestFactory(new MockMvcClientHttpRequestFactory(mockMvc));
			return super.build();
		}
	}


	/**
	 * Default implementation of {@link StandaloneSetupBuilder}.
	 */
	static class DefaultStandaloneSetupBuilder
			extends AbstractMockMvcSetupBuilder<StandaloneSetupBuilder, StandaloneMockMvcBuilder>
			implements StandaloneSetupBuilder {

		DefaultStandaloneSetupBuilder(Object... controllers) {
			super(MockMvcBuilders.standaloneSetup(controllers));
		}
	}


	/**
	 * Default implementation of {@link RouterFunctionSetupBuilder}.
	 */
	static class DefaultRouterFunctionSetupBuilder
			extends AbstractMockMvcSetupBuilder<RouterFunctionSetupBuilder, RouterFunctionMockMvcBuilder>
			implements RouterFunctionSetupBuilder {

		DefaultRouterFunctionSetupBuilder(RouterFunction<?>... routerFunctions) {
			super(MockMvcBuilders.routerFunctions(routerFunctions));
		}

	}


	/**
	 * Default implementation of {@link WebAppContextSetupBuilder}.
	 */
	static class DefaultWebAppContextSetupBuilder
			extends AbstractMockMvcSetupBuilder<WebAppContextSetupBuilder, DefaultMockMvcBuilder>
			implements WebAppContextSetupBuilder {

		DefaultWebAppContextSetupBuilder(WebApplicationContext context) {
			super(MockMvcBuilders.webAppContextSetup(context));
		}
	}

}
