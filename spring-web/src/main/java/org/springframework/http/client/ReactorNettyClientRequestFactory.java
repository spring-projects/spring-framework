/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.http.client;

import java.util.function.Function;

import reactor.netty.http.client.HttpClient;

/**
 * Reactor-Netty implementation of {@link ClientHttpRequestFactory}.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 6.1
 * @deprecated in favor of the renamed {@link ReactorClientHttpRequestFactory}
 */
@Deprecated(since = "6.2", forRemoval = true)
public class ReactorNettyClientRequestFactory extends ReactorClientHttpRequestFactory {

	/**
	 * Superseded by {@link ReactorClientHttpRequestFactory}.
	 * @see ReactorClientHttpRequestFactory#ReactorClientHttpRequestFactory()
	 */
	public ReactorNettyClientRequestFactory() {
		super();
	}

	/**
	 * Superseded by {@link ReactorClientHttpRequestFactory}.
	 * @see ReactorClientHttpRequestFactory#ReactorClientHttpRequestFactory(HttpClient)
	 */
	public ReactorNettyClientRequestFactory(HttpClient httpClient) {
		super(httpClient);
	}

	/**
	 * Superseded by {@link ReactorClientHttpRequestFactory}.
	 * @see ReactorClientHttpRequestFactory#ReactorClientHttpRequestFactory(ReactorResourceFactory, Function)
	 */
	public ReactorNettyClientRequestFactory(ReactorResourceFactory resourceFactory, Function<HttpClient, HttpClient> mapper) {
		super(resourceFactory, mapper);
	}

}
