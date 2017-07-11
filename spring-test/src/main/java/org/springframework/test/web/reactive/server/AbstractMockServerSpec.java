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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.util.Assert;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

/**
 * Base class for implementations of {@link WebTestClient.MockServerSpec}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
abstract class AbstractMockServerSpec<B extends WebTestClient.MockServerSpec<B>>
		implements WebTestClient.MockServerSpec<B> {

	private final List<WebFilter> filters = new ArrayList<>(4);

	private WebSessionManager sessionManager = new DefaultWebSessionManager();

	private final List<MockServerConfigurer> configurers = new ArrayList<>(4);


	@Override
	public <T extends B> T webFilter(WebFilter... filter) {
		this.filters.addAll(Arrays.asList(filter));
		return self();
	}

	@Override
	public <T extends B> T webSessionManager(WebSessionManager sessionManager) {
		Assert.notNull(sessionManager, "WebSessionManager must not be null.");
		this.sessionManager = sessionManager;
		return self();
	}

	@Override
	public <T extends B> T apply(MockServerConfigurer configurer) {
		configurer.afterConfigureAdded(this);
		this.configurers.add(configurer);
		return self();
	}

	@SuppressWarnings("unchecked")
	private <T extends B> T self() {
		return (T) this;
	}

	@Override
	public WebTestClient.Builder configureClient() {
		WebHttpHandlerBuilder builder = initHttpHandlerBuilder();
		builder.filters(theFilters -> theFilters.addAll(0, this.filters));
		builder.sessionManager(this.sessionManager);
		this.configurers.forEach(configurer -> configurer.beforeServerCreated(builder));
		return new DefaultWebTestClientBuilder(builder);
	}

	/**
	 * Sub-classes must create an {@code WebHttpHandlerBuilder} that will then
	 * be used to create the HttpHandler for the mock server.
	 */
	protected abstract WebHttpHandlerBuilder initHttpHandlerBuilder();

	@Override
	public WebTestClient build() {
		return configureClient().build();
	}

}
