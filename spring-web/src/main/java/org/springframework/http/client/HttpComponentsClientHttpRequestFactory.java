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
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.HttpContext;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.http.client.ClientHttpRequestFactory} implementation that
 * uses <a href="http://hc.apache.org/httpcomponents-client-ga/">Apache HttpComponents
 * HttpClient</a> to create requests.
 *
 * <p>Allows to use a pre-configured {@link HttpClient} instance -
 * potentially with authentication, HTTP connection pooling, etc.
 *
 * @author Oleg Kalnichevski
 * @author Arjen Poutsma
 * @since 3.1
 */
public class HttpComponentsClientHttpRequestFactory
		implements ClientHttpRequestFactory, DisposableBean {

	private HttpClient httpClient;

	private boolean bufferRequestBody = true;


	/**
	 * Create a new instance of the {@code HttpComponentsClientHttpRequestFactory} with
	 * a default {@link HttpClient}.
	 */
	public HttpComponentsClientHttpRequestFactory() {
		this(HttpClients.createDefault());
	}

	/**
	 * Create a new instance of the {@code HttpComponentsClientHttpRequestFactory}
	 * with the given {@link HttpClient} instance.
	 * @param httpClient the HttpClient instance to use for this request factory
	 */
	public HttpComponentsClientHttpRequestFactory(HttpClient httpClient) {
		Assert.notNull(httpClient, "'httpClient' must not be null");
		this.httpClient = httpClient;
	}

	/**
	 * Set the {@code HttpClient} used for
	 * {@linkplain #createRequest(URI, HttpMethod) synchronous execution}.
	 */
	public void setHttpClient(HttpClient httpClient) {
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
	 * Set the connection timeout for the underlying HttpClient.
	 * A timeout value of 0 specifies an infinite timeout.
	 * @param timeout the timeout value in milliseconds
	 * @deprecated With no direct replacement
	 */
	@Deprecated
	public void setConnectTimeout(int timeout) {
		Assert.isTrue(timeout >= 0, "Timeout must be a non-negative value");
		getHttpClient().getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);
	}

	/**
	 * Set the socket read timeout for the underlying HttpClient.
	 * A timeout value of 0 specifies an infinite timeout.
	 * @param timeout the timeout value in milliseconds
	 * @deprecated With no direct replacement
	 */
	@Deprecated
	public void setReadTimeout(int timeout) {
		Assert.isTrue(timeout >= 0, "Timeout must be a non-negative value");
		getHttpClient().getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT, timeout);
	}

	/**
	 * Indicates whether this request factory should buffer the request body internally.
	 *
	 * <p>Default is {@code true}. When sending large amounts of data via POST or PUT, it is
	 * recommended to change this property to {@code false}, so as not to run out of memory.
	 */
	public void setBufferRequestBody(boolean bufferRequestBody) {
		this.bufferRequestBody = bufferRequestBody;
	}

	/**
	 * Indicates whether this request factory should buffer the request body internally.
	 */
	public boolean isBufferRequestBody() {
		return bufferRequestBody;
	}



	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		HttpClient client = getHttpClient();
		Assert.state(client != null,
				"Synchronous execution requires an HttpClient to be set");
		HttpUriRequest httpRequest = createHttpUriRequest(httpMethod, uri);
		postProcessHttpRequest(httpRequest);
		if (bufferRequestBody) {
			return new HttpComponentsClientHttpRequest(client, httpRequest,
					createHttpContext(httpMethod, uri));
		}
		else {
			return new HttpComponentsStreamingClientHttpRequest(client,
					httpRequest, createHttpContext(httpMethod, uri));
		}
	}

	/**
	 * Create a Commons HttpMethodBase object for the given HTTP method and URI specification.
	 * @param httpMethod the HTTP method
	 * @param uri the URI
	 * @return the Commons HttpMethodBase object
	 */
	protected HttpUriRequest createHttpUriRequest(HttpMethod httpMethod, URI uri) {
		switch (httpMethod) {
			case GET:
				return new HttpGet(uri);
			case DELETE:
				return new HttpDelete(uri);
			case HEAD:
				return new HttpHead(uri);
			case OPTIONS:
				return new HttpOptions(uri);
			case POST:
				return new HttpPost(uri);
			case PUT:
				return new HttpPut(uri);
			case TRACE:
				return new HttpTrace(uri);
			case PATCH:
				return new HttpPatch(uri);
			default:
				throw new IllegalArgumentException("Invalid HTTP method: " + httpMethod);
		}
	}

	/**
	 * Template method that allows for manipulating the {@link HttpUriRequest} before it is
	 * returned as part of a {@link HttpComponentsClientHttpRequest}.
	 * <p>The default implementation is empty.
	 * @param request the request to process
	 */
	protected void postProcessHttpRequest(HttpUriRequest request) {
	}

	/**
	 * Template methods that creates a {@link HttpContext} for the given HTTP method and URI.
	 * <p>The default implementation returns {@code null}.
	 * @param httpMethod the HTTP method
	 * @param uri the URI
	 * @return the http context
	 */
	protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
		return null;
	}

	/**
	 * Shutdown hook that closes the underlying
	 * {@link org.apache.http.conn.ClientConnectionManager ClientConnectionManager}'s
	 * connection pool, if any.
	 */
	@Override
	public void destroy() throws Exception {
		getHttpClient().getConnectionManager().shutdown();
	}

}
