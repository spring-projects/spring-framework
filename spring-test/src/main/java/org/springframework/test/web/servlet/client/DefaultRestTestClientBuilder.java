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

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.RouterFunctionMockMvcBuilder;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.util.UriBuilderFactory;

/**
 * Default implementation of {@link RestTestClient.Builder}.
 *
 * @author Rob Worsnop
 * @author Rossen Stoyanchev
 * @param <B> the type of the builder
 * @since 7.0
 */
class DefaultRestTestClientBuilder<B extends RestTestClient.Builder<B>> implements RestTestClient.Builder<B> {

	private final RestClient.Builder restClientBuilder;


	DefaultRestTestClientBuilder() {
		this.restClientBuilder = RestClient.builder();
	}

	DefaultRestTestClientBuilder(RestClient.Builder restClientBuilder) {
		this.restClientBuilder = restClientBuilder;
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
	public <T extends B> T apply(Consumer<RestTestClient.Builder<B>> builderConsumer) {
		builderConsumer.accept(this);
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
		return new DefaultRestTestClient(this.restClientBuilder);
	}


	static class AbstractMockMvcSetupBuilder<S extends RestTestClient.Builder<S>, M extends MockMvcBuilder>
			extends DefaultRestTestClientBuilder<S> implements RestTestClient.MockMvcSetupBuilder<S, M> {

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


	static class DefaultStandaloneSetupBuilder extends AbstractMockMvcSetupBuilder<RestTestClient.StandaloneSetupBuilder, StandaloneMockMvcBuilder>
			implements RestTestClient.StandaloneSetupBuilder {

		DefaultStandaloneSetupBuilder(Object... controllers) {
			super(MockMvcBuilders.standaloneSetup(controllers));
		}
	}


	static class DefaultRouterFunctionSetupBuilder extends AbstractMockMvcSetupBuilder<RestTestClient.RouterFunctionSetupBuilder, RouterFunctionMockMvcBuilder>
			implements RestTestClient.RouterFunctionSetupBuilder {

		DefaultRouterFunctionSetupBuilder(RouterFunction<?>... routerFunctions) {
			super(MockMvcBuilders.routerFunctions(routerFunctions));
		}

	}


	static class DefaultWebAppContextSetupBuilder extends AbstractMockMvcSetupBuilder<RestTestClient.WebAppContextSetupBuilder, DefaultMockMvcBuilder>
			implements RestTestClient.WebAppContextSetupBuilder {

		DefaultWebAppContextSetupBuilder(WebApplicationContext context) {
			super(MockMvcBuilders.webAppContextSetup(context));
		}
	}

}
