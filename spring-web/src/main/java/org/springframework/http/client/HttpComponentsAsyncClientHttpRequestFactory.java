/*
 * Copyright 2002-2017 the original author or authors.
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

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.protocol.HttpContext;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * Asynchronous extension of the {@link HttpComponentsClientHttpRequestFactory}. Uses
 * <a href="https://hc.apache.org/httpcomponents-asyncclient-dev/">Apache HttpComponents
 * HttpAsyncClient 4.0</a> to create requests.
 *
 * @author Arjen Poutsma
 * @author Stephane Nicoll
 * @since 4.0
 * @see HttpAsyncClient
 */
public class HttpComponentsAsyncClientHttpRequestFactory extends HttpComponentsClientHttpRequestFactory
		implements AsyncClientHttpRequestFactory, InitializingBean {

	private HttpAsyncClient asyncClient;


	/**
	 * Create a new instance of the {@code HttpComponentsAsyncClientHttpRequestFactory}
	 * with a default {@link HttpAsyncClient} and {@link HttpClient}.
	 */
	public HttpComponentsAsyncClientHttpRequestFactory() {
		super();
		this.asyncClient = HttpAsyncClients.createSystem();
	}

	/**
	 * Create a new instance of the {@code HttpComponentsAsyncClientHttpRequestFactory}
	 * with the given {@link HttpAsyncClient} instance and a default {@link HttpClient}.
	 * @param asyncClient the HttpAsyncClient instance to use for this request factory
	 * @since 4.3.10
	 */
	public HttpComponentsAsyncClientHttpRequestFactory(HttpAsyncClient asyncClient) {
		super();
		setAsyncClient(asyncClient);
	}

	/**
	 * Create a new instance of the {@code HttpComponentsAsyncClientHttpRequestFactory}
	 * with the given {@link CloseableHttpAsyncClient} instance and a default {@link HttpClient}.
	 * @param asyncClient the CloseableHttpAsyncClient instance to use for this request factory
	 */
	public HttpComponentsAsyncClientHttpRequestFactory(CloseableHttpAsyncClient asyncClient) {
		super();
		setAsyncClient(asyncClient);
	}

	/**
	 * Create a new instance of the {@code HttpComponentsAsyncClientHttpRequestFactory}
	 * with the given {@link HttpClient} and {@link HttpAsyncClient} instances.
	 * @param httpClient the HttpClient instance to use for this request factory
	 * @param asyncClient the HttpAsyncClient instance to use for this request factory
	 * @since 4.3.10
	 */
	public HttpComponentsAsyncClientHttpRequestFactory(HttpClient httpClient, HttpAsyncClient asyncClient) {
		super(httpClient);
		setAsyncClient(asyncClient);
	}

	/**
	 * Create a new instance of the {@code HttpComponentsAsyncClientHttpRequestFactory}
	 * with the given {@link CloseableHttpClient} and {@link CloseableHttpAsyncClient} instances.
	 * @param httpClient the CloseableHttpClient instance to use for this request factory
	 * @param asyncClient the CloseableHttpAsyncClient instance to use for this request factory
	 */
	public HttpComponentsAsyncClientHttpRequestFactory(
			CloseableHttpClient httpClient, CloseableHttpAsyncClient asyncClient) {

		super(httpClient);
		setAsyncClient(asyncClient);
	}


	/**
	 * Set the {@code HttpAsyncClient} used for
	 * {@linkplain #createAsyncRequest(URI, HttpMethod) synchronous execution}.
	 * @since 4.3.10
	 * @see #setHttpClient(HttpClient)
	 */
	public void setAsyncClient(HttpAsyncClient asyncClient) {
		Assert.notNull(asyncClient, "HttpAsyncClient must not be null");
		this.asyncClient = asyncClient;
	}

	/**
	 * Return the {@code HttpAsyncClient} used for
	 * {@linkplain #createAsyncRequest(URI, HttpMethod) synchronous execution}.
	 * @since 4.3.10
	 * @see #getHttpClient()
	 */
	public HttpAsyncClient getAsyncClient() {
		return this.asyncClient;
	}

	/**
	 * Set the {@code CloseableHttpAsyncClient} used for
	 * {@linkplain #createAsyncRequest(URI, HttpMethod) asynchronous execution}.
	 * @deprecated as of 4.3.10, in favor of {@link #setAsyncClient(HttpAsyncClient)}
	 */
	@Deprecated
	public void setHttpAsyncClient(CloseableHttpAsyncClient asyncClient) {
		this.asyncClient = asyncClient;
	}

	/**
	 * Return the {@code CloseableHttpAsyncClient} used for
	 * {@linkplain #createAsyncRequest(URI, HttpMethod) asynchronous execution}.
	 * @deprecated as of 4.3.10, in favor of {@link #getAsyncClient()}
	 */
	@Deprecated
	public CloseableHttpAsyncClient getHttpAsyncClient() {
		Assert.state(this.asyncClient == null || this.asyncClient instanceof CloseableHttpAsyncClient,
				"No CloseableHttpAsyncClient - use getAsyncClient() instead");
		return (CloseableHttpAsyncClient) this.asyncClient;
	}


	@Override
	public void afterPropertiesSet() {
		startAsyncClient();
	}

	private void startAsyncClient() {
        HttpAsyncClient asyncClient = getAsyncClient();
		if (asyncClient instanceof CloseableHttpAsyncClient) {
			CloseableHttpAsyncClient closeableAsyncClient = (CloseableHttpAsyncClient) asyncClient;
			if (!closeableAsyncClient.isRunning()) {
				closeableAsyncClient.start();
			}
		}
	}

	@Override
	public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) throws IOException {
		startAsyncClient();

		HttpUriRequest httpRequest = createHttpUriRequest(httpMethod, uri);
		postProcessHttpRequest(httpRequest);
        HttpContext context = createHttpContext(httpMethod, uri);
        if (context == null) {
            context = HttpClientContext.create();
        }

		// Request configuration not set in the context
		if (context.getAttribute(HttpClientContext.REQUEST_CONFIG) == null) {
			// Use request configuration given by the user, when available
			RequestConfig config = null;
			if (httpRequest instanceof Configurable) {
				config = ((Configurable) httpRequest).getConfig();
			}
			if (config == null) {
				config = createRequestConfig(getAsyncClient());
			}
			if (config != null) {
				context.setAttribute(HttpClientContext.REQUEST_CONFIG, config);
			}
		}

		return new HttpComponentsAsyncClientHttpRequest(getAsyncClient(), httpRequest, context);
	}

	@Override
	public void destroy() throws Exception {
		try {
			super.destroy();
		}
		finally {
			HttpAsyncClient asyncClient = getAsyncClient();
			if (asyncClient instanceof Closeable) {
				((Closeable) asyncClient).close();
			}
		}
	}

}
