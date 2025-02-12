/*
 * Copyright 2002-2025 the original author or authors.
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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseCookie;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * {@link ClientHttpConnector} for the Java {@link HttpClient}.
 *
 * @author Julien Eyraud
 * @author Rossen Stoyanchev
 * @since 6.0
 * @see <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html">HttpClient</a>
 */
public class JdkClientHttpConnector implements ClientHttpConnector {

	private final HttpClient httpClient;

	private DataBufferFactory bufferFactory = DefaultDataBufferFactory.sharedInstance;

	private @Nullable Duration readTimeout;

	private ResponseCookie.Parser cookieParser = new JdkResponseCookieParser();


	/**
	 * Default constructor that uses {@link HttpClient#newHttpClient()}.
	 */
	public JdkClientHttpConnector() {
		this(HttpClient.newHttpClient());
	}

	/**
	 * Constructor with an initialized {@link HttpClient} and a {@link DataBufferFactory}.
	 */
	public JdkClientHttpConnector(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	/**
	 * Constructor with a {@link JdkHttpClientResourceFactory} that provides
	 * shared resources.
	 * @param clientBuilder a pre-initialized builder for the client that will
	 * be further initialized with the shared resources to use
	 * @param resourceFactory the {@link JdkHttpClientResourceFactory} to use
	 */
	public JdkClientHttpConnector(
			HttpClient.Builder clientBuilder, @Nullable JdkHttpClientResourceFactory resourceFactory) {

		if (resourceFactory != null) {
			Executor executor = resourceFactory.getExecutor();
			clientBuilder.executor(executor);
		}
		this.httpClient = clientBuilder.build();
	}


	/**
	 * Set the buffer factory to use.
	 * <p>By default, this is {@link DefaultDataBufferFactory#sharedInstance}.
	 */
	public void setBufferFactory(DataBufferFactory bufferFactory) {
		Assert.notNull(bufferFactory, "DataBufferFactory is required");
		this.bufferFactory = bufferFactory;
	}

	/**
	 * Set the underlying {@code HttpClient} read timeout as a {@code Duration}.
	 * <p>Default is the system's default timeout.
	 * @since 6.2
	 * @see java.net.http.HttpRequest.Builder#timeout
	 */
	public void setReadTimeout(Duration readTimeout) {
		Assert.notNull(readTimeout, "readTimeout is required");
		this.readTimeout = readTimeout;
	}

	/**
	 * Customize the parsing of response cookies.
	 * <p>By default, {@link java.net.HttpCookie#parse(String)} is used, and
	 * additionally the sameSite attribute is parsed and set.
	 * @param parser the parser to use
	 * @since 7.0
	 */
	public void setCookieParser(ResponseCookie.Parser parser) {
		Assert.notNull(parser, "ResponseCookie parser is required");
		this.cookieParser = parser;
	}


	@Override
	public Mono<ClientHttpResponse> connect(
			HttpMethod method, URI uri, Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

		JdkClientHttpRequest request =
				new JdkClientHttpRequest(method, uri, this.bufferFactory, this.readTimeout);

		return requestCallback.apply(request).then(Mono.defer(() -> {
			HttpRequest nativeRequest = request.getNativeRequest();

			CompletableFuture<HttpResponse<Flow.Publisher<List<ByteBuffer>>>> future =
					this.httpClient.sendAsync(nativeRequest, HttpResponse.BodyHandlers.ofPublisher());

			return Mono.fromCompletionStage(future).map(response ->
					new JdkClientHttpResponse(response, this.bufferFactory, parseCookies(response)));
		}));
	}

	private MultiValueMap<String, ResponseCookie> parseCookies(HttpResponse<?> response) {
		List<String> headers = response.headers().allValues(HttpHeaders.SET_COOKIE);
		return this.cookieParser.parse(headers);
	}

}
