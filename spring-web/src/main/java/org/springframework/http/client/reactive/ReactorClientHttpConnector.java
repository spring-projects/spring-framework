/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.http.client.reactive;

import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Function;

import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.client.HttpClientOptions;
import reactor.ipc.netty.options.ClientOptions;
import reactor.ipc.netty.http.client.HttpClient;
import reactor.ipc.netty.http.client.HttpClientException;

import org.springframework.http.HttpMethod;

/**
 * Reactor-Netty implementation of {@link ClientHttpConnector}
 *
 * @author Brian Clozel
 * @see HttpClient
 * @since 5.0
 */
public class ReactorClientHttpConnector implements ClientHttpConnector {

	private final HttpClient httpClient;


	/**
	 * Create a Reactor Netty {@link ClientHttpConnector} with default {@link ClientOptions}
	 * and SSL support enabled.
	 */
	public ReactorClientHttpConnector() {
		this.httpClient = HttpClient.create();
	}

	/**
	 * Create a Reactor Netty {@link ClientHttpConnector} with the given {@link ClientOptions}
	 */
	public ReactorClientHttpConnector(Consumer<? super HttpClientOptions> clientOptions) {
		this.httpClient = HttpClient.create(clientOptions);
	}


	@Override
	public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri,
			Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

		return httpClient
				.request(io.netty.handler.codec.http.HttpMethod.valueOf(method.name()),
						uri.toString(),
						httpClientRequest -> requestCallback
								.apply(new ReactorClientHttpRequest(method, uri, httpClientRequest)))
				.otherwise(HttpClientException.class, exc -> Mono.just(exc.getResponse()))
				.map(ReactorClientHttpResponse::new);
	}

}
