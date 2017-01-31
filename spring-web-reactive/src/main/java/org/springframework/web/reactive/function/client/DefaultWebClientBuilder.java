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
 * Default implementation of {@link WebClient.Builder}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultWebClientBuilder implements WebClient.Builder {

	private final ExchangeFunction exchangeFunction;

	private UriBuilderFactory uriBuilderFactory;


	public DefaultWebClientBuilder(ExchangeFunction exchangeFunction) {
		Assert.notNull(exchangeFunction, "'exchangeFunction' must not be null");
		this.exchangeFunction = exchangeFunction;
	}


	@Override
	public WebClient.Builder uriBuilderFactory(UriBuilderFactory uriBuilderFactory) {
		this.uriBuilderFactory = uriBuilderFactory;
		return this;
	}

	@Override
	public WebClient build() {
		return new DefaultWebClient(this.exchangeFunction, this.uriBuilderFactory);
	}

}
