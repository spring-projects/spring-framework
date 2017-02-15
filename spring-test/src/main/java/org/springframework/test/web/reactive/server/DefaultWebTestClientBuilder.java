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

import java.time.Duration;

import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Default implementation of {@link WebTestClient.Builder}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultWebTestClientBuilder implements WebTestClient.Builder {

	private final WebClient.Builder webClientBuilder;

	private final ClientHttpConnector connector;

	private Duration responseTimeout;


	public DefaultWebTestClientBuilder(WebClient.Builder builder, ClientHttpConnector connector) {
		this.webClientBuilder = builder;
		this.connector = connector;
	}


	@Override
	public WebTestClient.Builder responseTimeout(Duration timeout) {
		this.responseTimeout = timeout;
		return this;
	}

	@Override
	public WebTestClient build() {
		return new DefaultWebTestClient(this.webClientBuilder, this.connector, this.responseTimeout);
	}

}
