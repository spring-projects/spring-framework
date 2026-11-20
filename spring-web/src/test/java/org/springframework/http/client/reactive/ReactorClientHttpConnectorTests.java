/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client.reactive;

import java.net.URI;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ReactorResourceFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 6.1
 */
class ReactorClientHttpConnectorTests {

	@Test
	void restartWithDefaultConstructor() {
		ReactorClientHttpConnector connector = new ReactorClientHttpConnector();
		assertThat(connector.isRunning()).isTrue();
		connector.start();
		assertThat(connector.isRunning()).isTrue();
		connector.stop();
		assertThat(connector.isRunning()).isTrue();
		connector.start();
		assertThat(connector.isRunning()).isTrue();
		connector.stop();
		assertThat(connector.isRunning()).isTrue();
	}

	@Test
	void restartWithHttpClient() {
		HttpClient httpClient = HttpClient.create();
		ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
		assertThat(connector.isRunning()).isTrue();
		connector.start();
		assertThat(connector.isRunning()).isTrue();
		connector.stop();
		assertThat(connector.isRunning()).isTrue();
		connector.start();
		assertThat(connector.isRunning()).isTrue();
		connector.stop();
		assertThat(connector.isRunning()).isTrue();
	}

	@Test
	void restartWithExternalResourceFactory() {
		ReactorResourceFactory resourceFactory = new ReactorResourceFactory();
		resourceFactory.afterPropertiesSet();
		Function<HttpClient, HttpClient> mapper = Function.identity();
		ReactorClientHttpConnector connector = new ReactorClientHttpConnector(resourceFactory, mapper);
		assertThat(connector.isRunning()).isTrue();
		connector.start();
		assertThat(connector.isRunning()).isTrue();
		connector.stop();
		assertThat(connector.isRunning()).isFalse();
		connector.start();
		assertThat(connector.isRunning()).isTrue();
		connector.stop();
		assertThat(connector.isRunning()).isFalse();
	}

	@Test
	void lateStartWithExternalResourceFactory() {
		ReactorResourceFactory resourceFactory = new ReactorResourceFactory();
		Function<HttpClient, HttpClient> mapper = Function.identity();
		ReactorClientHttpConnector connector = new ReactorClientHttpConnector(resourceFactory, mapper);
		assertThat(connector.isRunning()).isFalse();
		resourceFactory.start();
		connector.start();
		assertThat(connector.isRunning()).isTrue();
		connector.stop();
		assertThat(connector.isRunning()).isFalse();
		connector.start();
		assertThat(connector.isRunning()).isTrue();
		connector.stop();
		assertThat(connector.isRunning()).isFalse();
	}

	@Test
	void lazyStartWithExternalResourceFactory() throws Exception {
		ReactorResourceFactory resourceFactory = new ReactorResourceFactory();
		Function<HttpClient, HttpClient> mapper = Function.identity();
		ReactorClientHttpConnector connector = new ReactorClientHttpConnector(resourceFactory, mapper);
		assertThat(connector.isRunning()).isFalse();
		resourceFactory.start();
		connector.connect(HttpMethod.GET, new URI(""), request -> Mono.empty());
		assertThat(connector.isRunning()).isTrue();
		connector.stop();
		assertThat(connector.isRunning()).isFalse();
		connector.connect(HttpMethod.GET, new URI(""), request -> Mono.empty());
		assertThat(connector.isRunning()).isFalse();
		connector.start();
		assertThat(connector.isRunning()).isTrue();
		connector.stop();
		assertThat(connector.isRunning()).isFalse();
	}

}
