/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.web.http.client.commons;

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
import org.springframework.util.Assert;
import org.springframework.web.http.HttpMethod;
import org.springframework.web.http.client.ClientHttpRequest;
import org.springframework.web.http.client.ClientHttpRequestFactory;

/**
 * {@link org.springframework.web.http.client.ClientHttpRequestFactory} implementation that uses <a
 * href="http://jakarta.apache.org/commons/httpclient">Jakarta Commons HttpClient</a> to create requests. <p/> Allows to
 * use a pre-configured {@link HttpClient} instance, potentially with authentication, HTTP connection pooling, etc.
 *
 * @author Arjen Poutsma
 * @see org.springframework.web.http.client.SimpleClientHttpRequestFactory
 */
public class CommonsClientHttpRequestFactory implements ClientHttpRequestFactory, DisposableBean {

	private static final int DEFAULT_READ_TIMEOUT_MILLISECONDS = (60 * 1000);

	private HttpClient httpClient;

	/**
	 * Create a new instance of the <code>CommonsHttpRequestFactory</code> with a default {@link HttpClient} that uses a
	 * default {@link MultiThreadedHttpConnectionManager}.
	 */
	public CommonsClientHttpRequestFactory() {
		httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
		this.setReadTimeout(DEFAULT_READ_TIMEOUT_MILLISECONDS);
	}

	/**
	 * Create a new instance of the <code>CommonsHttpRequestFactory</code> with the given  {@link HttpClient} instance.
	 *
	 * @param httpClient the HttpClient instance to use for this sender
	 */
	public CommonsClientHttpRequestFactory(HttpClient httpClient) {
		Assert.notNull(httpClient, "httpClient must not be null");
		this.httpClient = httpClient;
	}

	/**
	 * Returns the <code>HttpClient</code> used by this message sender.
	 */
	public HttpClient getHttpClient() {
		return httpClient;
	}

	/**
	 * Set the <code>HttpClient</code> used by this message sender.
	 */
	public void setHttpClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	/**
	 * Set the socket read timeout for the underlying HttpClient. A value of 0 means <em>never</em> timeout.
	 *
	 * @param timeout the timeout value in milliseconds
	 * @see org.apache.commons.httpclient.params.HttpConnectionManagerParams#setSoTimeout(int)
	 */
	public void setReadTimeout(int timeout) {
		if (timeout < 0) {
			throw new IllegalArgumentException("timeout must be a non-negative value");
		}
		this.httpClient.getHttpConnectionManager().getParams().setSoTimeout(timeout);
	}

	public void destroy() throws Exception {
		HttpConnectionManager connectionManager = getHttpClient().getHttpConnectionManager();
		if (connectionManager instanceof MultiThreadedHttpConnectionManager) {
			((MultiThreadedHttpConnectionManager) connectionManager).shutdown();
		}
	}

	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		String uriString = uri.toString();
		HttpMethodBase httpMethodBase;
		switch (httpMethod) {
			case GET:
				httpMethodBase = new GetMethod(uriString);
				break;
			case DELETE:
				httpMethodBase = new DeleteMethod(uriString);
				break;
			case HEAD:
				httpMethodBase = new HeadMethod(uriString);
				break;
			case OPTIONS:
				httpMethodBase = new OptionsMethod(uriString);
				break;
			case POST:
				httpMethodBase = new PostMethod(uriString);
				break;
			case PUT:
				httpMethodBase = new PutMethod(uriString);
				break;
			case TRACE:
				httpMethodBase = new TraceMethod(uriString);
				break;
			default:
				throw new IllegalArgumentException("Invalid method: " + httpMethod);
		}
		process(httpMethodBase);

		return new CommonsClientHttpRequest(getHttpClient(), httpMethodBase);
	}

	/**
	 * Template method that allows for manipulating the {@link org.apache.commons.httpclient.HttpMethodBase} before it is
	 * returned as part of a {@link CommonsClientHttpRequest}. <p/> Default implementation is empty.
	 *
	 * @param httpMethod the Commons HTTP method to process
	 */
	protected void process(HttpMethodBase httpMethod) {
	}
}