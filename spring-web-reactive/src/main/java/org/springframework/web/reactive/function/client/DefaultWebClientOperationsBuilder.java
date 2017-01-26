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

import org.springframework.util.Assert;
import org.springframework.web.util.UriBuilderFactory;

/**
 * Default implementation of {@link WebClientOperations.Builder}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultWebClientOperationsBuilder implements WebClientOperations.Builder {

	private final WebClient webClient;

	private UriBuilderFactory uriBuilderFactory;


	public DefaultWebClientOperationsBuilder(WebClient webClient) {
		Assert.notNull(webClient, "WebClient is required");
		this.webClient = webClient;
	}


	@Override
	public WebClientOperations.Builder uriBuilderFactory(UriBuilderFactory uriBuilderFactory) {
		this.uriBuilderFactory = uriBuilderFactory;
		return this;
	}

	@Override
	public WebClientOperations build() {
		return new DefaultWebClientOperations(this.webClient, this.uriBuilderFactory);
	}

}
