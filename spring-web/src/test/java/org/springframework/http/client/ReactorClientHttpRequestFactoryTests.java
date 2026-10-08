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

package org.springframework.http.client;

import java.time.Duration;
import java.util.function.Function;

import io.netty.channel.ChannelOption;
import org.junit.jupiter.api.Test;
import reactor.netty.http.client.HttpClient;

import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

/**
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 6.1
 */
class ReactorClientHttpRequestFactoryTests extends AbstractHttpRequestFactoryTests {

	@Override
	protected ClientHttpRequestFactory createRequestFactory() {
		return new ReactorClientHttpRequestFactory();
	}

	@Override
	@Test
	void httpMethods() throws Exception {
		super.httpMethods();
		assertHttpMethod("patch", HttpMethod.PATCH);
	}

	@Test
	void connectTimeoutAsDurationExceedingIntegerRangeIsCapped() {
		ReactorClientHttpRequestFactory requestFactory = new ReactorClientHttpRequestFactory(HttpClient.create());
		requestFactory.setConnectTimeout(Duration.ofDays(50));

		assertThat(requestFactory).extracting("httpClient", as(type(HttpClient.class)))
				.extracting(client -> client.configuration().options().get(ChannelOption.CONNECT_TIMEOUT_MILLIS))
				.isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	void restartWithDefaultConstructor() {
		ReactorClientHttpRequestFactory requestFactory = new ReactorClientHttpRequestFactory();
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
		ReactorClientHttpRequestFactory requestFactory = new ReactorClientHttpRequestFactory(httpClient);
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
		ReactorClientHttpRequestFactory requestFactory = new ReactorClientHttpRequestFactory(resourceFactory, mapper);
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
		ReactorClientHttpRequestFactory requestFactory = new ReactorClientHttpRequestFactory(resourceFactory, mapper);
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
