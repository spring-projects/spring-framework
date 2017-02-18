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

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;

/**
 * Decorates a {@link ClientHttpConnector} in order to capture executed requests
 * and responses and notify one or more registered listeners. This is helpful
 * for access to the actual {@link ClientHttpRequest} sent and the
 * {@link ClientHttpResponse} returned by the server.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class WiretapConnector implements ClientHttpConnector {

	private final ClientHttpConnector delegate;

	private final List<Consumer<Info>> listeners;


	public WiretapConnector(ClientHttpConnector delegate) {
		this.delegate = delegate;
		this.listeners = new CopyOnWriteArrayList<>();
	}


	/**
	 * Register a listener to consume exchanged requests and responses.
	 */
	public void addListener(Consumer<Info> consumer) {
		this.listeners.add(consumer);
	}


	@Override
	public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri,
			Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

		AtomicReference<ClientHttpRequest> requestRef = new AtomicReference<>();

		return this.delegate
				.connect(method, uri, request -> {
					requestRef.set(request);
					return requestCallback.apply(request);
				})
				.doOnNext(response -> {
					Info info = new Info(requestRef.get(), response);
					this.listeners.forEach(consumer -> consumer.accept(info));
				});
	}


	public static class Info {

		private final HttpMethod method;

		private final URI url;

		private final HttpHeaders requestHeaders;

		private final ClientHttpResponse response;


		public Info(ClientHttpRequest request, ClientHttpResponse response) {
			this.method = request.getMethod();
			this.url = request.getURI();
			this.requestHeaders = request.getHeaders();
			this.response = response;
		}


		public HttpMethod getMethod() {
			return this.method;
		}

		public URI getUrl() {
			return this.url;
		}

		public HttpHeaders getRequestHeaders() {
			return this.requestHeaders;
		}

		public ClientHttpResponse getResponse() {
			return this.response;
		}
	}

}
