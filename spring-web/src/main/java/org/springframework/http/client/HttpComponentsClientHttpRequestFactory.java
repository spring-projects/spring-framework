/*
 * Copyright 2002-2024 the original author or authors.
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
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpOptions;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpTrace;
import org.apache.hc.client5.http.config.Configurable;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.protocol.HttpContext;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.http.client.ClientHttpRequestFactory} implementation that
 * uses <a href="https://hc.apache.org/httpcomponents-client-ga/">Apache HttpComponents
 * HttpClient</a> to create requests.
 *
 * <p>Allows to use a pre-configured {@link HttpClient} instance -
 * potentially with authentication, HTTP connection pooling, etc.
 *
 * <p><b>NOTE:</b> Requires Apache HttpComponents 5.1 or higher, as of Spring 6.0.
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
	private BiFunction<HttpMethod, URI, HttpContext> httpContextFactory;

	private long connectTimeout = -1;

	private long connectionRequestTimeout = -1;


	/**
	 * Create a new instance of the {@code HttpComponentsClientHttpRequestFactory}
	 * with a default {@link HttpClient} based on system properties.
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
	 * Set the connection timeout for the underlying {@link RequestConfig}.
	 * A timeout value of 0 specifies an infinite timeout.
	 * <p>Additional properties can be configured by specifying a
	 * {@link RequestConfig} instance on a custom {@link HttpClient}.
	 * <p>This options does not affect connection timeouts for SSL
	 * handshakes or CONNECT requests; for that, it is required to
	 * use the {@link SocketConfig} on the
	 * {@link HttpClient} itself.
	 * @param connectTimeout the timeout value in milliseconds
	 * @see RequestConfig#getConnectTimeout()
	 * @see SocketConfig#getSoTimeout
	 */
	public void setConnectTimeout(int connectTimeout) {
		Assert.isTrue(connectTimeout >= 0, "Timeout must be a non-negative value");
		this.connectTimeout = connectTimeout;
	}

	/**
	 * Set the connection timeout for the underlying {@link RequestConfig}.
	 * A timeout value of 0 specifies an infinite timeout.
	 * <p>Additional properties can be configured by specifying a
	 * {@link RequestConfig} instance on a custom {@link HttpClient}.
	 * <p>This options does not affect connection timeouts for SSL
	 * handshakes or CONNECT requests; for that, it is required to
	 * use the {@link SocketConfig} on the
	 * {@link HttpClient} itself.
	 * @param connectTimeout the timeout value in milliseconds
	 * @since 6.1
	 * @see RequestConfig#getConnectTimeout()
	 * @see SocketConfig#getSoTimeout
	 */
	public void setConnectTimeout(Duration connectTimeout) {
		Assert.notNull(connectTimeout, "ConnectTimeout must not be null");
		Assert.isTrue(!connectTimeout.isNegative(), "Timeout must be a non-negative value");
		this.connectTimeout = connectTimeout.toMillis();
	}

	/**
	 * Set the timeout in milliseconds used when requesting a connection
	 * from the connection manager using the underlying {@link RequestConfig}.
	 * A timeout value of 0 specifies an infinite timeout.
	 * <p>Additional properties can be configured by specifying a
	 * {@link RequestConfig} instance on a custom {@link HttpClient}.
	 * @param connectionRequestTimeout the timeout value to request a connection in milliseconds
	 * @see RequestConfig#getConnectionRequestTimeout()
	 */
	public void setConnectionRequestTimeout(int connectionRequestTimeout) {
		Assert.isTrue(connectionRequestTimeout >= 0, "Timeout must be a non-negative value");
		this.connectionRequestTimeout = connectionRequestTimeout;
	}

	/**
	 * Set the timeout in milliseconds used when requesting a connection
	 * from the connection manager using the underlying {@link RequestConfig}.
	 * A timeout value of 0 specifies an infinite timeout.
	 * <p>Additional properties can be configured by specifying a
	 * {@link RequestConfig} instance on a custom {@link HttpClient}.
	 * @param connectionRequestTimeout the timeout value to request a connection in milliseconds
	 * @since 6.1
	 * @see RequestConfig#getConnectionRequestTimeout()
	 */
	public void setConnectionRequestTimeout(Duration connectionRequestTimeout) {
		Assert.notNull(connectionRequestTimeout, "ConnectionRequestTimeout must not be null");
		Assert.isTrue(!connectionRequestTimeout.isNegative(), "Timeout must be a non-negative value");
		this.connectionRequestTimeout = connectionRequestTimeout.toMillis();
	}

	/**
	 * Indicates whether this request factory should buffer the request body internally.
	 * <p>Default is {@code true}. When sending large amounts of data via POST or PUT, it is
	 * recommended to change this property to {@code false}, so as not to run out of memory.
	 * @since 4.0
	 * @deprecated since 6.1 requests are never buffered, as if this property is {@code false}
	 */
	@Deprecated(since = "6.1", forRemoval = true)
	public void setBufferRequestBody(boolean bufferRequestBody) {
		// no-op
	}

	/**
	 * Configure a factory to pre-create the {@link HttpContext} for each request.
	 * <p>This may be useful for example in mutual TLS authentication where a
	 * different {@code RestTemplate} for each client certificate such that
	 * all calls made through a given {@code RestTemplate} instance as associated
	 * for the same client identity. {@link HttpClientContext#setUserToken(Object)}
	 * can be used to specify a fixed user token for all requests.
	 * @param httpContextFactory the context factory to use
	 * @since 5.2.7
	 */
	public void setHttpContextFactory(BiFunction<HttpMethod, URI, HttpContext> httpContextFactory) {
		this.httpContextFactory = httpContextFactory;
	}


	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		HttpClient client = getHttpClient();

		ClassicHttpRequest httpRequest = createHttpUriRequest(httpMethod, uri);
		postProcessHttpRequest(httpRequest);
		HttpContext context = createHttpContext(httpMethod, uri);
		if (context == null) {
			context = HttpClientContext.create();
		}

		// Request configuration not set in the context
		if (context.getAttribute(HttpClientContext.REQUEST_CONFIG) == null) {
			// Use request configuration given by the user, when available
			RequestConfig config = null;
			if (httpRequest instanceof Configurable configurable) {
				config = configurable.getConfig();
			}
			if (config == null) {
				config = createRequestConfig(client);
			}
			if (config != null) {
				context.setAttribute(HttpClientContext.REQUEST_CONFIG, config);
			}
		}
		return new HttpComponentsClientHttpRequest(client, httpRequest, context);
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
		if (client instanceof Configurable configurableClient) {
			RequestConfig clientRequestConfig = configurableClient.getConfig();
			return mergeRequestConfig(clientRequestConfig);
		}
		return mergeRequestConfig(RequestConfig.DEFAULT);
	}

	/**
	 * Merge the given {@link HttpClient}-level {@link RequestConfig} with
	 * the factory-level configuration, if necessary.
	 * @param clientConfig the config held by the current
	 * @return the merged request config
	 * @since 4.2
	 */
	@SuppressWarnings("deprecation")  // setConnectTimeout
	protected RequestConfig mergeRequestConfig(RequestConfig clientConfig) {
		if (this.connectTimeout == -1 && this.connectionRequestTimeout == -1) {  // nothing to merge
			return clientConfig;
		}

		RequestConfig.Builder builder = RequestConfig.copy(clientConfig);
		if (this.connectTimeout >= 0) {
			builder.setConnectTimeout(this.connectTimeout, TimeUnit.MILLISECONDS);
		}
		if (this.connectionRequestTimeout >= 0) {
			builder.setConnectionRequestTimeout(this.connectionRequestTimeout, TimeUnit.MILLISECONDS);
		}
		return builder.build();
	}

	/**
	 * Create a Commons HttpMethodBase object for the given HTTP method and URI specification.
	 * @param httpMethod the HTTP method
	 * @param uri the URI
	 * @return the Commons HttpMethodBase object
	 */
	protected ClassicHttpRequest createHttpUriRequest(HttpMethod httpMethod, URI uri) {
		if (HttpMethod.GET.equals(httpMethod)) {
			return new HttpGet(uri);
		}
		else if (HttpMethod.HEAD.equals(httpMethod)) {
			return new HttpHead(uri);
		}
		else if (HttpMethod.POST.equals(httpMethod)) {
			return new HttpPost(uri);
		}
		else if (HttpMethod.PUT.equals(httpMethod)) {
			return new HttpPut(uri);
		}
		else if (HttpMethod.PATCH.equals(httpMethod)) {
			return new HttpPatch(uri);
		}
		else if (HttpMethod.DELETE.equals(httpMethod)) {
			return new HttpDelete(uri);
		}
		else if (HttpMethod.OPTIONS.equals(httpMethod)) {
			return new HttpOptions(uri);
		}
		else if (HttpMethod.TRACE.equals(httpMethod)) {
			return new HttpTrace(uri);
		}
		throw new IllegalArgumentException("Invalid HTTP method: " + httpMethod);
	}

	/**
	 * Template method that allows for manipulating the {@link ClassicHttpRequest}
	 * before it is returned as part of a {@link HttpComponentsClientHttpRequest}.
	 * <p>The default implementation is empty.
	 * @param request the request to process
	 */
	protected void postProcessHttpRequest(ClassicHttpRequest request) {
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
		return (this.httpContextFactory != null ? this.httpContextFactory.apply(httpMethod, uri) : null);
	}


	/**
	 * Shutdown hook that closes the underlying {@link HttpClientConnectionManager}'s
	 * connection pool, if any.
	 */
	@Override
	public void destroy() throws Exception {
		HttpClient httpClient = getHttpClient();
		if (httpClient instanceof Closeable closeable) {
			closeable.close();
		}
	}

}
