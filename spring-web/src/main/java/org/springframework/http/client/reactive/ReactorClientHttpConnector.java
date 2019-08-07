/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.http.client.reactive;

import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Function;

import io.netty.buffer.UnpooledByteBufAllocator;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.client.HttpClient;
import reactor.ipc.netty.http.client.HttpClientOptions;
import reactor.ipc.netty.http.client.HttpClientRequest;
import reactor.ipc.netty.http.client.HttpClientResponse;
import reactor.ipc.netty.options.ClientOptions;

import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpMethod;

/**
 * Reactor-Netty implementation of {@link ClientHttpConnector}.
 *
 * @author Brian Clozel
 * @since 5.0
 * @see reactor.ipc.netty.http.client.HttpClient
 */
public class ReactorClientHttpConnector implements ClientHttpConnector {

	// 5.0.x only: no buffer pooling
	static final NettyDataBufferFactory BUFFER_FACTORY =
			new NettyDataBufferFactory(new UnpooledByteBufAllocator(false));


	private final HttpClient httpClient;


	/**
	 * Create a Reactor Netty {@link ClientHttpConnector}
	 * with default {@link ClientOptions} and HTTP compression support enabled.
	 */
	public ReactorClientHttpConnector() {
		this.httpClient = HttpClient.builder()
				.options(options -> options.compression(true))
				.build();
	}

	/**
	 * Create a Reactor Netty {@link ClientHttpConnector} with the given
	 * {@code HttpClientOptions.Builder}
	 */
	public ReactorClientHttpConnector(Consumer<? super HttpClientOptions.Builder> clientOptions) {
		this.httpClient = HttpClient.create(clientOptions);
	}


	@Override
	public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri,
			Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

		if (!uri.isAbsolute()) {
			return Mono.error(new IllegalArgumentException("URI is not absolute: " + uri));
		}

		return this.httpClient
				.request(adaptHttpMethod(method),
						uri.toString(),
						request -> requestCallback.apply(adaptRequest(method, uri, request)))
				.map(this::adaptResponse);
	}

	private io.netty.handler.codec.http.HttpMethod adaptHttpMethod(HttpMethod method) {
		return io.netty.handler.codec.http.HttpMethod.valueOf(method.name());
	}

	private ReactorClientHttpRequest adaptRequest(HttpMethod method, URI uri, HttpClientRequest request) {
		return new ReactorClientHttpRequest(method, uri, request);
	}

	private ClientHttpResponse adaptResponse(HttpClientResponse response) {
		return new ReactorClientHttpResponse(response);
	}

}
