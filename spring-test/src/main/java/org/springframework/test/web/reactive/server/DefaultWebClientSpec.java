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

import java.util.Map;

import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilderFactory;

/**
 * Default implementation of {@link WebTestClient.WebClientSpec}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultWebClientSpec implements WebTestClient.WebClientSpec {

	private final WebClient.Builder builder = WebClient.builder();

	private final ClientHttpConnector connector;


	public DefaultWebClientSpec() {
		this(new ReactorClientHttpConnector());
	}

	public DefaultWebClientSpec(HttpHandler httpHandler) {
		this(new HttpHandlerConnector(httpHandler));
	}

	public DefaultWebClientSpec(ClientHttpConnector connector) {
		this.connector = connector;
	}


	@Override
	public WebTestClient.WebClientSpec baseUrl(String baseUrl) {
		this.builder.baseUrl(baseUrl);
		return this;
	}

	@Override
	public WebTestClient.WebClientSpec defaultUriVariables(Map<String, ?> defaultUriVariables) {
		this.builder.defaultUriVariables(defaultUriVariables);
		return this;
	}

	@Override
	public WebTestClient.WebClientSpec uriBuilderFactory(UriBuilderFactory uriBuilderFactory) {
		this.builder.uriBuilderFactory(uriBuilderFactory);
		return this;
	}

	@Override
	public WebTestClient.WebClientSpec defaultHeader(String headerName, String... headerValues) {
		this.builder.defaultHeader(headerName, headerValues);
		return this;
	}

	@Override
	public WebTestClient.WebClientSpec defaultCookie(String cookieName, String... cookieValues) {
		this.builder.defaultCookie(cookieName, cookieValues);
		return this;
	}

	@Override
	public WebTestClient.WebClientSpec exchangeStrategies(ExchangeStrategies strategies) {
		this.builder.exchangeStrategies(strategies);
		return this;
	}

	@Override
	public WebTestClient.Builder builder() {
		return new DefaultWebTestClientBuilder(this.builder, this.connector);
	}

	@Override
	public WebTestClient build() {
		return builder().build();
	}

}
