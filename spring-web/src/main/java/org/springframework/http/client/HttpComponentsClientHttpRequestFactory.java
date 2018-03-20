/*
 * Copyright 2002-2018 the original author or authors.
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

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.http.client.ClientHttpRequestFactory} implementation that
 * uses <a href="http://hc.apache.org/httpcomponents-client-ga/">Apache HttpComponents
 * HttpClient</a> to create requests.
 *
 * <p>Allows to use a pre-configured {@link HttpClient} instance -
 * potentially with authentication, HTTP connection pooling, etc.
 *
 * <p><b>NOTE:</b> Requires Apache HttpComponents 4.3 or higher, as of Spring 4.0.
 *
 * @author Oleg Kalnichevski
 * @author Arjen Poutsma
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 3.1
 */
public class HttpComponentsClientHttpRequestFactory implements ClientHttpRequestFactory, DisposableBean {

	private HttpClient httpClient;

	@Nullable
	private RequestConfig requestConfig;

	private boolean bufferRequestBody = true;


	/**
	 * Create a new instance of the {@code HttpComponentsClientHttpRequestFactory}
	 * with a default {@link HttpClient}.
	 */
	public HttpComponentsClientHttpRequestFactory() {
		this.httpClient = HttpClients.createSystem();
	}

	/**
	 * Create a new instance of the {@code HttpComponentsClientHttpRequestFactory}
	 * with the given {@link HttpClient} instance.
	 * @param httpClient the HttpClient instance to use for this request factory
	 */
	public HttpComponentsClientHttpRequestFactory(HttpClient httpClient) {
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
	 * Set the connection timeout for the underlying HttpClient.
	 * A timeout value of 0 specifies an infinite timeout.
	 * <p>Additional properties can be configured by specifying a
	 * {@link RequestConfig} instance on a custom {@link HttpClient}.
	 * @param timeout the timeout value in milliseconds
	 * @see RequestConfig#getConnectTimeout()
	 */
	public void setConnectTimeout(int timeout) {
		Assert.isTrue(timeout >= 0, "Timeout must be a non-negative value");
		this.requestConfig = requestConfigBuilder().setConnectTimeout(timeout).build();
	}

	/**
	 * Set the timeout in milliseconds used when requesting a connection from the connection
	 * manager using the underlying HttpClient.
	 * A timeout value of 0 specifies an infinite timeout.
	 * <p>Additional properties can be configured by specifying a
	 * {@link RequestConfig} instance on a custom {@link HttpClient}.
	 * @param connectionRequestTimeout the timeout value to request a connection in milliseconds
	 * @see RequestConfig#getConnectionRequestTimeout()
	 */
	public void setConnectionRequestTimeout(int connectionRequestTimeout) {
		this.requestConfig = requestConfigBuilder().setConnectionRequestTimeout(connectionRequestTimeout).build();
	}

	/**
	 * Set the socket read timeout for the underlying HttpClient.
	 * A timeout value of 0 specifies an infinite timeout.
	 * <p>Additional properties can be configured by specifying a
	 * {@link RequestConfig} instance on a custom {@link HttpClient}.
	 * @param timeout the timeout value in milliseconds
	 * @see RequestConfig#getSocketTimeout()
	 */
	public void setReadTimeout(int timeout) {
		Assert.isTrue(timeout >= 0, "Timeout must be a non-negative value");
		this.requestConfig = requestConfigBuilder().setSocketTimeout(timeout).build();
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
		HttpClient client = getHttpClient();

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
				config = createRequestConfig(client);
			}
			if (config != null) {
				context.setAttribute(HttpClientContext.REQUEST_CONFIG, config);
			}
		}

		if (this.bufferRequestBody) {
			return new HttpComponentsClientHttpRequest(client, httpRequest, context);
		}
		else {
			return new HttpComponentsStreamingClientHttpRequest(client, httpRequest, context);
		}
	}


	/**
	 * Return a builder for modifying the factory-level {@link RequestConfig}.
	 * @since 4.2
	 */
	private RequestConfig.Builder requestConfigBuilder() {
		return (this.requestConfig != null ? RequestConfig.copy(this.requestConfig) : RequestConfig.custom());
	}

	/**
	 * Create a default {@link RequestConfig} to use with the given client.
	 * Can return {@code null} to indicate that no custom request config should
	 * be set and the defaults of the {@link HttpClient} should be used.
	 * <p>The default implementation tries to merge the defaults of the client
	 * with the local customizations of this factory instance, if any.
	 * @param client the {@link HttpClient} (or {@code HttpAsyncClient}) to check
	 * @return the actual RequestConfig to use (may be {@code null})
	 * @since 4.2
	 * @see #mergeRequestConfig(RequestConfig)
	 */
	@Nullable
	protected RequestConfig createRequestConfig(Object client) {
		if (client instanceof Configurable) {
			RequestConfig clientRequestConfig = ((Configurable) client).getConfig();
			return mergeRequestConfig(clientRequestConfig);
		}
		return this.requestConfig;
	}

	/**
	 * Merge the given {@link HttpClient}-level {@link RequestConfig} with
	 * the factory-level {@link RequestConfig}, if necessary.
	 * @param clientConfig the config held by the current
	 * @return the merged request config
	 * @since 4.2
	 */
	protected RequestConfig mergeRequestConfig(RequestConfig clientConfig) {
		if (this.requestConfig == null) {  // nothing to merge
			return clientConfig;
		}

		RequestConfig.Builder builder = RequestConfig.copy(clientConfig);
		int connectTimeout = this.requestConfig.getConnectTimeout();
		if (connectTimeout >= 0) {
			builder.setConnectTimeout(connectTimeout);
		}
		int connectionRequestTimeout = this.requestConfig.getConnectionRequestTimeout();
		if (connectionRequestTimeout >= 0) {
			builder.setConnectionRequestTimeout(connectionRequestTimeout);
		}
		int socketTimeout = this.requestConfig.getSocketTimeout();
		if (socketTimeout >= 0) {
			builder.setSocketTimeout(socketTimeout);
		}
		return builder.build();
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
			case HEAD:
				return new HttpHead(uri);
			case POST:
				return new HttpPost(uri);
			case PUT:
				return new HttpPut(uri);
			case PATCH:
				return new HttpPatch(uri);
			case DELETE:
				return new HttpDelete(uri);
			case OPTIONS:
				return new HttpOptions(uri);
			case TRACE:
				return new HttpTrace(uri);
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
	@Nullable
	protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
		return null;
	}


	/**
	 * Shutdown hook that closes the underlying
	 * {@link org.apache.http.conn.HttpClientConnectionManager ClientConnectionManager}'s
	 * connection pool, if any.
	 */
	@Override
	public void destroy() throws Exception {
		HttpClient httpClient = getHttpClient();
		if (httpClient instanceof Closeable) {
			((Closeable) httpClient).close();
		}
	}


	/**
	 * An alternative to {@link org.apache.http.client.methods.HttpDelete} that
	 * extends {@link org.apache.http.client.methods.HttpEntityEnclosingRequestBase}
	 * rather than {@link org.apache.http.client.methods.HttpRequestBase} and
	 * hence allows HTTP delete with a request body. For use with the RestTemplate
	 * exchange methods which allow the combination of HTTP DELETE with an entity.
	 * @since 4.1.2
	 */
	private static class HttpDelete extends HttpEntityEnclosingRequestBase {

		public HttpDelete(URI uri) {
			super();
			setURI(uri);
		}

		@Override
		public String getMethod() {
			return "DELETE";
		}
	}

}
