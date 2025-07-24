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

import jakarta.servlet.Filter;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.servlet.DispatcherServletCustomizer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;

/**
 * Base class for implementations of {@link RestTestClient.MockServerSpec}
 * that simply delegates to a {@link org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder} supplied by
 * the concrete subclasses.
 *
 * @author Rob Worsnop
 * @param <B> the type of the concrete subclass spec
 */
abstract class AbstractMockServerSpec<B extends RestTestClient.MockServerSpec<B>>
		implements RestTestClient.MockServerSpec<B> {

	@Override
	public <T extends B> T filters(Filter... filters) {
		getMockMvcBuilder().addFilters(filters);
		return self();
	}

	@Override
	public final <T extends B> T filter(Filter filter, String... urlPatterns) {
		getMockMvcBuilder().addFilter(filter, urlPatterns);
		return self();
	}

	@Override
	public <T extends B> T defaultRequest(RequestBuilder requestBuilder) {
		getMockMvcBuilder().defaultRequest(requestBuilder);
		return self();
	}

	@Override
	public <T extends B> T alwaysExpect(ResultMatcher resultMatcher) {
		getMockMvcBuilder().alwaysExpect(resultMatcher);
		return self();
	}

	@Override
	public <T extends B> T dispatchOptions(boolean dispatchOptions) {
		getMockMvcBuilder().dispatchOptions(dispatchOptions);
		return self();
	}

	@Override
	public <T extends B> T dispatcherServletCustomizer(DispatcherServletCustomizer customizer) {
		getMockMvcBuilder().addDispatcherServletCustomizer(customizer);
		return self();
	}

	@Override
	public <T extends B> T apply(MockMvcConfigurer configurer) {
		getMockMvcBuilder().apply(configurer);
		return self();
	}

	@SuppressWarnings("unchecked")
	private <T extends B> T self() {
		return (T) this;
	}

	/**
	 * Return the concrete {@link ConfigurableMockMvcBuilder} to delegate
	 * configuration methods and to use to create the {@link MockMvc}.
	 */
	protected abstract ConfigurableMockMvcBuilder<?> getMockMvcBuilder();

	@Override
	public RestTestClient.Builder configureClient() {
		MockMvc mockMvc = getMockMvcBuilder().build();
		ClientHttpRequestFactory requestFactory = new MockMvcClientHttpRequestFactory(mockMvc);
		return RestTestClient.bindToServer(requestFactory);
	}

	@Override
	public RestTestClient build() {
		return configureClient().build();
	}
}
