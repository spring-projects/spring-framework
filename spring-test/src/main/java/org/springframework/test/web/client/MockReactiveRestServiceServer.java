/*
 * Copyright 2002-2021 the original author or authors.
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
package org.springframework.test.web.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseCookie.ResponseCookieBuilder;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpResponse;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * A mock REST service server that can be used to verify requests from {@link WebClient} objects, and
 * stub mock responses to provide to the client.
 * <p>
 * This acts as a drop-in replacement for {@link MockRestServiceServer} that provides support for reactive
 * {@link WebClient} objects. While supporting a reactive client API underneath, the majority of this implementation
 * works with the same components that {@link MockRestServiceServer} does.
 * <p>
 * This is relatively lightweight API to use, so can be initialized per test case if required. This does not open
 * any network sockets.
 * <p>
 * Instances of this server can be created by calling either
 * {@link #createServer()}, {@link #createServer(boolean)}, or {@link #createServer(RequestExpectationManager)}.
 * {@link WebClient} instances that will communicate with this server can then be created using the
 * {@link #createWebClient()} or {@link #createWebClientBuilder()} methods, respectively.
 *
 * @author Ashley Scopes
 * @see StepVerifier
 * @see RequestMatcher
 * @see ResponseCreator
 * @see MockRestRequestMatchers
 * @see MockRestResponseCreators
 */
public final class MockReactiveRestServiceServer {
	private final RequestExpectationManager expectationManager;

	/**
	 * Initialize the mock server with the desired expectation manager to use.
	 *
	 * @param expectationManager the expectation manager to use for requests.
	 */
	private MockReactiveRestServiceServer(@NonNull RequestExpectationManager expectationManager) {
		this.expectationManager = expectationManager;
	}

	/**
	 * Register an expectation for a request to occur exactly once. This is an alias for calling
	 * {@link #expect(ExpectedCount, RequestMatcher)} with {@link ExpectedCount#once()} as the first
	 * parameter.
	 *
	 * @param requestMatcher the request matcher to use.
	 * @return the response actions builder to use to define how to respond to such requests.
	 */
	@NonNull
	public ResponseActions expect(@NonNull RequestMatcher requestMatcher) {
		return expect(ExpectedCount.once(), requestMatcher);
	}

	/**
	 * Register an expectation for a request to occur a given number of times.
	 *
	 * @param count   the number of times the request should be made.
	 * @param matcher the matcher for the request.
	 * @return the response actions builder to use to define how to respond to such requests.
	 */
	@NonNull
	public ResponseActions expect(@NonNull ExpectedCount count, @NonNull RequestMatcher matcher) {
		return this.expectationManager.expectRequest(count, matcher);
	}

	/**
	 * Verify that the expected requests have been performed. This is equivalent to calling
	 * {@link #verify(Duration)} with {@link Duration#ZERO}.
	 *
	 * @throws AssertionError if the requests have not been performed as expected.
	 */
	public void verify() {
		this.expectationManager.verify();
	}

	/**
	 * Verify that the expected results get performed within the given timeout.
	 *
	 * @param timeout the maximum time to wait before failing.
	 * @throws AssertionError if the request times out before the expected requests occur.
	 */
	@NonNull
	public void verify(@NonNull Duration timeout) {
		this.expectationManager.verify(timeout);
	}

	/**
	 * Remove all expectations from this mock server.
	 */
	public void reset() {
		this.expectationManager.reset();
	}

	/**
	 * Create a new {@link WebClient} builder that is bound to this mock server.
	 *
	 * @return a {@code WebClient} builder that will pipe requests through this mock server instance.
	 */
	@NonNull
	public WebClient.Builder createWebClientBuilder() {
		return WebClient
				.builder()
				.clientConnector(new MockClientHttpConnector());
	}

	/**
	 * Create a new {@link WebClient} that is bound to this mock server.
	 *
	 * @return a {@code WebClient} with default settings that pipes requests through this mock server instance.
	 */
	@NonNull
	public WebClient createWebClient() {
		return createWebClientBuilder().build();
	}

	/**
	 * Create a new mock reactive REST service server with the given expectation manager.
	 *
	 * @param manager the expectation manager to use.
	 * @return the new mock reactive REST service server.
	 */
	@NonNull
	public static MockReactiveRestServiceServer createServer(@NonNull RequestExpectationManager manager) {
		return new MockReactiveRestServiceServer(manager);
	}

	/**
	 * Create a new mock reactive REST service server.
	 *
	 * @param ignoreRequestOrder true to ignore the order of requests, or false to require requests to
	 *                           be performed in the order that they are registered.
	 * @return the new mock reactive REST service server.
	 */
	@NonNull
	public static MockReactiveRestServiceServer createServer(boolean ignoreRequestOrder) {
		RequestExpectationManager manager = ignoreRequestOrder
				? new UnorderedRequestExpectationManager()
				: new SimpleRequestExpectationManager();

		return createServer(manager);
	}

	/**
	 * Create a new mock reactive REST service server that requires requests to be performed in the order that they
	 * are registered.
	 *
	 * @return the new mock reactive REST service server to use.
	 */
	@NonNull
	public static MockReactiveRestServiceServer createServer() {
		return createServer(false);
	}

	/**
	 * Internal mock connector for {@link WebClient} instances. This deals with converting reactive
	 * {@link org.springframework.mock.http.client.reactive.MockClientHttpRequest} objects to non-reactive
	 * {@link org.springframework.mock.http.client.MockClientHttpRequest} objects that the
	 * {@link MockRestServiceServer} internals are compatible with. This also will handle converting non-reactive
	 * {@link org.springframework.mock.http.client.MockClientHttpResponse} objects back to reactive
	 * {@link org.springframework.mock.http.client.reactive.MockClientHttpResponse} objects for the response object
	 * to use internally.
	 *
	 * @author Ashley Scopes
	 */
	private final class MockClientHttpConnector implements ClientHttpConnector {
		@NonNull
		public Mono<ClientHttpResponse> connect(
				@NonNull HttpMethod method, @NonNull URI uri,
				@NonNull Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

			MockClientHttpRequest request = new MockClientHttpRequest(method, uri);

			return Mono
					.defer(() -> requestCallback.apply(request))
					.thenReturn(request)
					.flatMap(this::createRequest)
					.map(this::performRequest)
					.map(this::convertResponse)
					.checkpoint("Request to " + method + " " + uri + " [MockReactiveRestServiceServer]")
					.onErrorMap(ex -> ex.getCause() instanceof AssertionError
							? Exceptions.bubble(ex.getCause())
							: ex);
		}

		@NonNull
		private Mono<org.springframework.http.client.ClientHttpRequest> createRequest(
				@NonNull MockClientHttpRequest reactiveRequest) {

			org.springframework.mock.http.client.MockClientHttpRequest proceduralRequest =
					new org.springframework.mock.http.client.MockClientHttpRequest(
							reactiveRequest.getMethod(), reactiveRequest.getURI());

			proceduralRequest.getHeaders().putAll(reactiveRequest.getHeaders());

			return DataBufferUtils
					.join(reactiveRequest.getBody())
					.doOnNext(dataBuffer -> {
						byte[] arr = new byte[1024];
						int c;

						try {
							InputStream input = dataBuffer.asInputStream();
							OutputStream output = proceduralRequest.getBody();

							while ((c = input.read(arr)) != -1) {
								output.write(arr, 0, c);
							}
						}
						catch (IOException ex) {
							throw new RuntimeException(ex);
						}
					})
					.thenReturn(proceduralRequest);
		}

		@NonNull
		private org.springframework.http.client.ClientHttpResponse performRequest(
				@NonNull org.springframework.http.client.ClientHttpRequest proceduralRequest) {

			try {
				return MockReactiveRestServiceServer
						.this
						.expectationManager
						.validateRequest(proceduralRequest);
			}
			catch (IOException ex) {
				throw new WebClientRequestException(
						ex,
						Objects.requireNonNull(proceduralRequest.getMethod()),
						proceduralRequest.getURI(),
						proceduralRequest.getHeaders()
				);
			}
		}

		@NonNull
		private ClientHttpResponse convertResponse(
				@NonNull org.springframework.http.client.ClientHttpResponse proceduralResponse) {

			int status = -1;
			String statusMessage = "Could not read response status";
			HttpHeaders headers = null;

			try {
				status = proceduralResponse.getRawStatusCode();
				statusMessage = proceduralResponse.getStatusText();
				headers = proceduralResponse.getHeaders();
				DataBuffer responseBody = DefaultDataBufferFactory
						.sharedInstance
						.wrap(IOUtils.toByteArray(proceduralResponse.getBody()));

				MockClientHttpResponse reactiveResponse = new MockClientHttpResponse(status);
				reactiveResponse.getHeaders().putAll(headers);
				MultiValueMap<String, ResponseCookie> cookies = reactiveResponse.getCookies();

				headers.getOrEmpty(HttpHeaders.SET_COOKIE)
						.stream()
						.filter(cookie -> cookie.contains("="))
						.map(cookie -> ResponseCookie.fromClientResponse(
								cookie.substring(0, cookie.indexOf('=')),
								cookie.substring(cookie.indexOf('=') + 1)
						))
						.map(ResponseCookieBuilder::build)
						.forEach(cookie -> cookies.add(cookie.getName(), cookie));

				reactiveResponse.setBody(Flux.just(responseBody));

				return reactiveResponse;
			}
			catch (IOException ex) {
				throw new WebClientResponseException(status, statusMessage, headers, null, null);
			}
		}
	}
}
