/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.http.client;

import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;

/**
 * {@link org.springframework.http.client.ClientHttpRequestFactory} implementation that
 * uses the Java {@link HttpClient}.
 */
public class JdkClientHttpRequestFactory implements ClientHttpRequestFactory {

	private HttpClient httpClient;

	private boolean expectContinue;

	@Nullable
	private Duration requestTimeout;

	private boolean bufferRequestBody = true;


	/**
	 * Create a new instance of the {@code JdkClientHttpRequestFactory}
	 * with a default {@link HttpClient}.
	 */
	public JdkClientHttpRequestFactory() {
		this.httpClient = HttpClient.newHttpClient();
	}

	/**
	 * Create a new instance of the {@code JdkClientHttpRequestFactory}
	 * with the given {@link HttpClient} instance.
	 * @param httpClient the HttpClient instance to use for this request factory
	 */
	public JdkClientHttpRequestFactory(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	/**
	 * Set the {@code HttpClient} used for
	 * {@linkplain #createRequest(URI, HttpMethod) synchronous execution}.
	 */
	public void setHttpClient(HttpClient httpClient) {
		Assert.notNull(httpClient, "HttpClient must not be null");
		this.httpClient = httpClient;
	}

	/**
	 * Return the {@code HttpClient} used for
	 * {@linkplain #createRequest(URI, HttpMethod) synchronous execution}.
	 */
	public HttpClient getHttpClient() {
		return this.httpClient;
	}

	/**
	 * If {@code true}, requests the server to acknowledge the request before sending the body.
	 * @param expectContinue {@code} if the server is requested to acknowledge the request
	 * @see HttpRequest#expectContinue()
	 */
	public void setExpectContinue(boolean expectContinue) {
		this.expectContinue = expectContinue;
	}

	/**
	 * Set the request timeout for a request. A {code null} of 0 specifies an infinite timeout.
	 * @param requestTimeout the timeout value or {@code null} to disable the timeout
	 * @see HttpRequest#timeout()
	 */
	public void setRequestTimeout(@Nullable Duration requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	/**
	 * Indicates whether this request factory should buffer the request body internally.
	 * <p>Default is {@code true}. When sending large amounts of data via POST or PUT, it is
	 * recommended to change this property to {@code false}, so as not to run out of memory.
	 */
	public void setBufferRequestBody(boolean bufferRequestBody) {
		this.bufferRequestBody = bufferRequestBody;
	}

	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		HttpClient client = getHttpClient();

		if (this.bufferRequestBody) {
			return new JdkClientHttpRequest(client, httpMethod, uri, expectContinue, requestTimeout);
		}
		else {
			return new JdkClientStreamingHttpRequest(client, httpMethod, uri, expectContinue, requestTimeout);
		}
	}
}
