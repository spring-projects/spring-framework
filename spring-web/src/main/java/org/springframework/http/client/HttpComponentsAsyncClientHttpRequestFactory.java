/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.nio.reactor.IOReactorStatus;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * Asynchronous extension of the {@link HttpComponentsClientHttpRequestFactory}. Uses
  * <a href="http://hc.apache.org/httpcomponents-asyncclient-dev/">Apache HttpComponents
 * HttpAsyncClient</a> to create requests.
 *
 * @author Arjen Poutsma
 * @since 4.0
 * @see HttpAsyncClient
 */
public class HttpComponentsAsyncClientHttpRequestFactory
		extends HttpComponentsClientHttpRequestFactory
		implements AsyncClientHttpRequestFactory, InitializingBean {

	private HttpAsyncClient httpAsyncClient;


	/**
	 * Create a new instance of the {@code HttpComponentsAsyncClientHttpRequestFactory}
	 * with a default {@link HttpAsyncClient} and {@link HttpClient}.
	 */
	public HttpComponentsAsyncClientHttpRequestFactory() {
		this(HttpAsyncClients.createDefault());
	}

	/**
	 * Create a new instance of the {@code HttpComponentsAsyncClientHttpRequestFactory}
	 * with the given {@link HttpAsyncClient} instance and a default {@link HttpClient}.
	 * @param httpAsyncClient the HttpAsyncClient instance to use for this request factory
	 */
	public HttpComponentsAsyncClientHttpRequestFactory(HttpAsyncClient httpAsyncClient) {
		super();
		Assert.notNull(httpAsyncClient, "'httpAsyncClient' must not be null");
		this.httpAsyncClient = httpAsyncClient;
	}

	/**
	 * Create a new instance of the {@code HttpComponentsAsyncClientHttpRequestFactory}
	 * with the given {@link HttpClient} and {@link HttpAsyncClient} instances.
	 * @param httpClient the HttpClient instance to use for this request factory
	 * @param httpAsyncClient the HttpAsyncClient instance to use for this request factory
	 */
	public HttpComponentsAsyncClientHttpRequestFactory(HttpClient httpClient,
			HttpAsyncClient httpAsyncClient) {
		super(httpClient);
		Assert.notNull(httpAsyncClient, "'httpAsyncClient' must not be null");
		this.httpAsyncClient = httpAsyncClient;
	}

	/**
	 * Set the {@code HttpClient} used for
	 * {@linkplain #createAsyncRequest(java.net.URI, org.springframework.http.HttpMethod) asynchronous execution}.
	 */
	public void setHttpAsyncClient(HttpAsyncClient httpAsyncClient) {
		this.httpAsyncClient = httpAsyncClient;
	}

	/**
	 * Return the {@code HttpClient} used for
	 * {@linkplain #createAsyncRequest(URI, HttpMethod) asynchronous execution}.
	 */
	public HttpAsyncClient getHttpAsyncClient() {
		return httpAsyncClient;
	}

	@Override
	public void afterPropertiesSet() {
		startAsyncClient();
	}

	private void startAsyncClient() {
		HttpAsyncClient asyncClient = getHttpAsyncClient();
		if (asyncClient.getStatus() != IOReactorStatus.ACTIVE) {
			asyncClient.start();
		}
	}

	@Override
	public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod)
			throws IOException {
		HttpAsyncClient asyncClient = getHttpAsyncClient();
		startAsyncClient();
		HttpUriRequest httpRequest = createHttpUriRequest(httpMethod, uri);
		postProcessHttpRequest(httpRequest);
		return new HttpComponentsAsyncClientHttpRequest(asyncClient, httpRequest,
				createHttpContext(httpMethod, uri));
	}

	@Override
	public void destroy() throws Exception {
		super.destroy();
		getHttpAsyncClient().shutdown();
	}
}
