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

import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilderFactory;

/**
 * Default implementation of {@link RestTestClient.Builder}.
 *
 * @author Rob Worsnop
 */
class DefaultRestTestClientBuilder implements RestTestClient.Builder {

	private final RestClient.Builder restClientBuilder;

	DefaultRestTestClientBuilder() {
		this.restClientBuilder = RestClient.builder();
	}

	DefaultRestTestClientBuilder(RestClient.Builder restClientBuilder) {
		this.restClientBuilder = restClientBuilder;
	}

	@Override
	public RestTestClient.Builder apply(Consumer<RestTestClient.Builder> builderConsumer) {
		builderConsumer.accept(this);
		return this;
	}

	@Override
	public RestTestClient.Builder baseUrl(String baseUrl) {
		this.restClientBuilder.baseUrl(baseUrl);
		return this;
	}

	@Override
	public RestTestClient.Builder defaultCookie(String cookieName, String... cookieValues) {
		this.restClientBuilder.defaultCookie(cookieName, cookieValues);
		return this;
	}

	@Override
	public RestTestClient.Builder defaultCookies(Consumer<MultiValueMap<String, String>> cookiesConsumer) {
		this.restClientBuilder.defaultCookies(cookiesConsumer);
		return this;
	}

	@Override
	public RestTestClient.Builder defaultHeader(String headerName, String... headerValues) {
		this.restClientBuilder.defaultHeader(headerName, headerValues);
		return this;
	}

	@Override
	public RestTestClient.Builder defaultHeaders(Consumer<HttpHeaders> headersConsumer) {
		this.restClientBuilder.defaultHeaders(headersConsumer);
		return this;
	}

	@Override
	public RestTestClient.Builder uriBuilderFactory(UriBuilderFactory uriFactory) {
		this.restClientBuilder.uriBuilderFactory(uriFactory);
		return this;
	}

	@Override
	public RestTestClient build() {
		return new DefaultRestTestClient(this.restClientBuilder);
	}
}
