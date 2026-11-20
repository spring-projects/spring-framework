/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
