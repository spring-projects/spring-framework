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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.util.Assert;

/**
 * Decorate any other {@link ClientHttpConnector} with the purpose of
 * intercepting, capturing, and exposing {@code ClientHttpRequest}s reflecting
 * the exact and complete details sent to the server.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see HttpHandlerConnector
 */
class WebTestClientConnector implements ClientHttpConnector {

	public static final String REQUEST_ID_HEADER_NAME = "request-id";


	private final ClientHttpConnector delegate;

	private final Map<String, ClientHttpRequest> capturedRequests = new ConcurrentHashMap<>();


	public WebTestClientConnector(ClientHttpConnector delegate) {
		this.delegate = delegate;
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
				.doOnNext(response ->  {
					ClientHttpRequest request = requestRef.get();
					String id = request.getHeaders().getFirst(REQUEST_ID_HEADER_NAME);
					if (id != null) {
						this.capturedRequests.put(id, request);
					}
				});
	}

	/**
	 * Retrieve the request with the given "request-id" header.
	 */
	public ClientHttpRequest claimRequest(String requestId) {
		ClientHttpRequest request = this.capturedRequests.get(requestId);
		Assert.notNull(request, "No matching request [" + requestId + "]. Did connect return a response yet?");
		return request;
	}

}
