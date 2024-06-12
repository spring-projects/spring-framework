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

import org.junit.jupiter.api.Test;
import reactor.netty.http.client.HttpClient;

import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 6.1
 */
class ReactorNettyClientRequestFactoryTests extends AbstractHttpRequestFactoryTests {

	@Override
	protected ClientHttpRequestFactory createRequestFactory() {
		return new ReactorNettyClientRequestFactory();
	}

	@Override
	@Test
	void httpMethods() throws Exception {
		super.httpMethods();
		assertHttpMethod("patch", HttpMethod.PATCH);
	}

	@Test
	void restartWithDefaultConstructor() {
		ReactorNettyClientRequestFactory requestFactory = new ReactorNettyClientRequestFactory();
		assertThat(requestFactory.isRunning()).isTrue();
		requestFactory.start();
		assertThat(requestFactory.isRunning()).isTrue();
		requestFactory.stop();
		assertThat(requestFactory.isRunning()).isTrue();
		requestFactory.start();
		assertThat(requestFactory.isRunning()).isTrue();
	}

	@Test
	void restartWithHttpClient() {
		HttpClient httpClient = HttpClient.create();
		ReactorNettyClientRequestFactory requestFactory = new ReactorNettyClientRequestFactory(httpClient);
		assertThat(requestFactory.isRunning()).isTrue();
		requestFactory.start();
		assertThat(requestFactory.isRunning()).isTrue();
		requestFactory.stop();
		assertThat(requestFactory.isRunning()).isTrue();
		requestFactory.start();
		assertThat(requestFactory.isRunning()).isTrue();
	}

	@Test
	void restartWithExternalResourceFactory() {
		ReactorResourceFactory resourceFactory = new ReactorResourceFactory();
		resourceFactory.afterPropertiesSet();
		Function<HttpClient, HttpClient> mapper = Function.identity();
		ReactorNettyClientRequestFactory requestFactory = new ReactorNettyClientRequestFactory(resourceFactory, mapper);
		assertThat(requestFactory.isRunning()).isTrue();
		requestFactory.start();
		assertThat(requestFactory.isRunning()).isTrue();
		requestFactory.stop();
		assertThat(requestFactory.isRunning()).isFalse();
		requestFactory.start();
		assertThat(requestFactory.isRunning()).isTrue();
	}

	@Test
	void lateStartWithExternalResourceFactory() {
		ReactorResourceFactory resourceFactory = new ReactorResourceFactory();
		Function<HttpClient, HttpClient> mapper = Function.identity();
		ReactorNettyClientRequestFactory requestFactory = new ReactorNettyClientRequestFactory(resourceFactory, mapper);
		assertThat(requestFactory.isRunning()).isFalse();
		resourceFactory.start();
		requestFactory.start();
		assertThat(requestFactory.isRunning()).isTrue();
		requestFactory.stop();
		assertThat(requestFactory.isRunning()).isFalse();
		requestFactory.start();
		assertThat(requestFactory.isRunning()).isTrue();
	}

}
