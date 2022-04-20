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

package org.springframework.web.reactive.function;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.URI;

/**
 * {@link ClientHttpRequestFactory} implementation that
 * uses Spring's {@link WebClient}.
 */
public class WebClientHttpRequestFactory implements ClientHttpRequestFactory {

	private WebClient webClient;

	private boolean bufferRequestBody = true;


	/**
	 * Create a new instance of the {@code WebClientHttpRequestFactory}
	 * with a default {@link WebClient} based on system properties.
	 */
	public WebClientHttpRequestFactory() {
		this.webClient = WebClient.create();
	}

	/**
	 * Create a new instance of the {@code WebClientHttpRequestFactory}
	 * with the given {@link WebClient} instance.
	 * @param webClient the HttpClient instance to use for this request factory
	 */
	public WebClientHttpRequestFactory(WebClient webClient) {
		this.webClient = webClient;
	}

	/**
	 * Set the {@code HttpClient} used for
	 * {@linkplain #createRequest(URI, HttpMethod) synchronous execution}.
	 */
	public void setHttpClient(WebClient webClient) {
		Assert.notNull(webClient, "WebClient must not be null");
		this.webClient = webClient;
	}

	/**
	 * Return the {@code HttpClient} used for
	 * {@linkplain #createRequest(URI, HttpMethod) synchronous execution}.
	 */
	public WebClient getHttpClient() {
		return this.webClient;
	}

	/**
	 * Indicates whether this request factory should buffer the request body internally.
	 * <p>Default is {@code true}. When sending large amounts of data via POST or PUT, it is
	 * recommended to change this property to {@code false}, so as not to run out of memory.
	 * @since 4.0
	 */
	public void setBufferRequestBody(boolean bufferRequestBody) {
		this.bufferRequestBody = bufferRequestBody;
	}

	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		WebClient client = getHttpClient();

		if (this.bufferRequestBody) {
			return new WebClientHttpRequest(client, httpMethod, uri);
		}
		else {
			return new WebClientStreamingHttpRequest(client, httpMethod, uri);
		}
	}
}
