/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.function.Function;

import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Mono;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientRequest;
import reactor.netty.http.client.HttpClientResponse;

import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * Reactor-Netty implementation of {@link ClientHttpConnector}.
 *
 * @author Brian Clozel
 * @since 5.0
 * @see reactor.netty.http.client.HttpClient
 */
public class ReactorClientHttpConnector implements ClientHttpConnector {

	private final HttpClient httpClient;


	/**
	 * Create a Reactor Netty {@link ClientHttpConnector}
	 * with default configuration and HTTP compression support enabled.
	 */
	public ReactorClientHttpConnector() {
		this.httpClient = HttpClient.create().compress();
	}

	/**
	 * Create a Reactor Netty {@link ClientHttpConnector} with a fully
	 * configured {@code HttpClient}.
	 * @param httpClient the client instance to use
	 * @since 5.1
	 */
	public ReactorClientHttpConnector(HttpClient httpClient) {
		Assert.notNull(httpClient, "HttpClient is required");
		this.httpClient = httpClient;
	}


	@Override
	public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri,
			Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

		if (!uri.isAbsolute()) {
			return Mono.error(new IllegalArgumentException("URI is not absolute: " + uri));
		}

		return this.httpClient
				.request(io.netty.handler.codec.http.HttpMethod.valueOf(method.name()))
				.uri(uri.toString())
				.send((request, outbound) -> requestCallback.apply(adaptRequest(method, uri, request, outbound)))
				.responseConnection((res, con) -> Mono.just(adaptResponse(res, con.inbound(), con.outbound().alloc())))
				.next();
	}

	private ReactorClientHttpRequest adaptRequest(HttpMethod method, URI uri, HttpClientRequest request,
			NettyOutbound nettyOutbound) {

		return new ReactorClientHttpRequest(method, uri, request, nettyOutbound);
	}

	private ClientHttpResponse adaptResponse(HttpClientResponse response, NettyInbound nettyInbound,
			ByteBufAllocator allocator) {

		return new ReactorClientHttpResponse(response, nettyInbound, allocator);
	}

}
