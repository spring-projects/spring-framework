/*
 * Copyright 2002-2012 the original author or authors.
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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.TraceMethod;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.http.client.ClientHttpRequestFactory} implementation that uses
 * <a href="http://jakarta.apache.org/commons/httpclient">Jakarta Commons HttpClient</a> to create requests.
 *
 * <p>Allows to use a pre-configured {@link HttpClient} instance -
 * potentially with authentication, HTTP connection pooling, etc.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see org.springframework.http.client.SimpleClientHttpRequestFactory
 * @deprecated In favor of {@link HttpComponentsClientHttpRequestFactory}
 */
@Deprecated
public class CommonsClientHttpRequestFactory implements ClientHttpRequestFactory, DisposableBean {

	private static final int DEFAULT_READ_TIMEOUT_MILLISECONDS = (60 * 1000);

	private HttpClient httpClient;


	/**
	 * Create a new instance of the {@code CommonsHttpRequestFactory} with a default
	 * {@link HttpClient} that uses a default {@link MultiThreadedHttpConnectionManager}.
	 */
	public CommonsClientHttpRequestFactory() {
		this.httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
		this.setReadTimeout(DEFAULT_READ_TIMEOUT_MILLISECONDS);
	}

	/**
	 * Create a new instance of the {@code CommonsHttpRequestFactory} with the given
	 * {@link HttpClient} instance.
	 * @param httpClient the HttpClient instance to use for this factory
	 */
	public CommonsClientHttpRequestFactory(HttpClient httpClient) {
		Assert.notNull(httpClient, "httpClient must not be null");
		this.httpClient = httpClient;
	}


	/**
	 * Set the {@code HttpClient} used by this factory.
	 */
	public void setHttpClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	/**
	 * Return the {@code HttpClient} used by this factory.
	 */
	public HttpClient getHttpClient() {
		return this.httpClient;
	}

	/**
	 * Set the connection timeout for the underlying HttpClient.
	 * A timeout value of 0 specifies an infinite timeout.
	 * @param timeout the timeout value in milliseconds
	 * @see org.apache.commons.httpclient.params.HttpConnectionManagerParams#setConnectionTimeout(int)
	 */
	public void setConnectTimeout(int timeout) {
		Assert.isTrue(timeout >= 0, "Timeout must be a non-negative value");
		this.httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(timeout);
	}

	/**
	 * Set the socket read timeout for the underlying HttpClient.
	 * A timeout value of 0 specifies an infinite timeout.
	 * @param timeout the timeout value in milliseconds
	 * @see org.apache.commons.httpclient.params.HttpConnectionManagerParams#setSoTimeout(int)
	 */
	public void setReadTimeout(int timeout) {
		Assert.isTrue(timeout >= 0, "Timeout must be a non-negative value");
		getHttpClient().getHttpConnectionManager().getParams().setSoTimeout(timeout);
	}


	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		HttpMethodBase commonsHttpMethod = createCommonsHttpMethod(httpMethod, uri.toString());
		postProcessCommonsHttpMethod(commonsHttpMethod);
		return new CommonsClientHttpRequest(getHttpClient(), commonsHttpMethod);
	}

	/**
	 * Create a Commons HttpMethodBase object for the given HTTP method
	 * and URI specification.
	 * @param httpMethod the HTTP method
	 * @param uri the URI
	 * @return the Commons HttpMethodBase object
	 */
	protected HttpMethodBase createCommonsHttpMethod(HttpMethod httpMethod, String uri) {
		switch (httpMethod) {
			case GET:
				return new GetMethod(uri);
			case DELETE:
				return new DeleteMethod(uri);
			case HEAD:
				return new HeadMethod(uri);
			case OPTIONS:
				return new OptionsMethod(uri);
			case POST:
				return new PostMethod(uri);
			case PUT:
				return new PutMethod(uri);
			case TRACE:
				return new TraceMethod(uri);
			case PATCH:
				throw new IllegalArgumentException(
						"HTTP method PATCH not available before Apache HttpComponents HttpClient 4.2");
			default:
				throw new IllegalArgumentException("Invalid HTTP method: " + httpMethod);
		}
	}

	/**
	 * Template method that allows for manipulating the {@link org.apache.commons.httpclient.HttpMethodBase}
	 * before it is returned as part of a {@link CommonsClientHttpRequest}.
	 * <p>The default implementation is empty.
	 * @param httpMethod the Commons HTTP method object to process
	 */
	protected void postProcessCommonsHttpMethod(HttpMethodBase httpMethod) {
	}

	/**
	 * Shutdown hook that closes the underlying {@link HttpConnectionManager}'s
	 * connection pool, if any.
	 */
	@Override
	public void destroy() {
		HttpConnectionManager connectionManager = getHttpClient().getHttpConnectionManager();
		if (connectionManager instanceof MultiThreadedHttpConnectionManager) {
			((MultiThreadedHttpConnectionManager) connectionManager).shutdown();
		}
	}

}
