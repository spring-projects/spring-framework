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
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.HttpHeadResponseDecorator;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * Connector that handles requests by invoking an {@link HttpHandler} rather
 * than making actual requests to a network socket.
 *
 * <p>Internally the connector uses and adapts<br>
 * {@link MockClientHttpRequest} and {@link MockClientHttpResponse} to<br>
 * {@link MockServerHttpRequest} and {@link MockServerHttpResponse}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class HttpHandlerConnector implements ClientHttpConnector {

	private static Log logger = LogFactory.getLog(HttpHandlerConnector.class);


	private final HttpHandler handler;


	/**
	 * Constructor with the {@link HttpHandler} to handle requests with.
	 */
	public HttpHandlerConnector(HttpHandler handler) {
		Assert.notNull(handler, "HttpHandler is required");
		this.handler = handler;
	}


	@Override
	public Mono<ClientHttpResponse> connect(HttpMethod httpMethod, URI uri,
			Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

		MonoProcessor<ClientHttpResponse> result = MonoProcessor.create();

		MockClientHttpRequest mockClientRequest = new MockClientHttpRequest(httpMethod, uri);
		MockServerHttpResponse mockServerResponse = new MockServerHttpResponse();

		mockClientRequest.setWriteHandler(requestBody -> {
			log("Invoking HttpHandler for ", httpMethod, uri);
			ServerHttpRequest mockServerRequest = adaptRequest(mockClientRequest, requestBody);
			ServerHttpResponse responseToUse = prepareResponse(mockServerResponse, mockServerRequest);
			this.handler.handle(mockServerRequest, responseToUse).subscribe(aVoid -> {}, result::onError);
			return Mono.empty();
		});

		mockServerResponse.setWriteHandler(responseBody -> {
			log("Creating client response for ", httpMethod, uri);
			result.onNext(adaptResponse(mockServerResponse, responseBody));
			return Mono.empty();
		});

		log("Writing client request for ", httpMethod, uri);
		requestCallback.apply(mockClientRequest).subscribe(aVoid -> {}, result::onError);

		return result;
	}

	private void log(String message, HttpMethod httpMethod, URI uri) {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("%s %s \"%s\"", message, httpMethod, uri));
		}
	}

	private ServerHttpRequest adaptRequest(MockClientHttpRequest request, Publisher<DataBuffer> body) {
		HttpMethod method = request.getMethod();
		URI uri = request.getURI();
		HttpHeaders headers = request.getHeaders();
		MultiValueMap<String, HttpCookie> cookies = request.getCookies();
		return MockServerHttpRequest.method(method, uri).headers(headers).cookies(cookies).body(body);
	}

	private ServerHttpResponse prepareResponse(ServerHttpResponse response, ServerHttpRequest request) {
		return HttpMethod.HEAD.equals(request.getMethod()) ?
				new HttpHeadResponseDecorator(response) : response;
	}

	private ClientHttpResponse adaptResponse(MockServerHttpResponse response, Flux<DataBuffer> body) {
		HttpStatus status = Optional.ofNullable(response.getStatusCode()).orElse(HttpStatus.OK);
		MockClientHttpResponse clientResponse = new MockClientHttpResponse(status);
		clientResponse.getHeaders().putAll(response.getHeaders());
		clientResponse.getCookies().putAll(response.getCookies());
		clientResponse.setBody(body);
		return clientResponse;
	}

}
