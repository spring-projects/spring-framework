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

package org.springframework.remoting.httpinvoker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.Nullable;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.remoting.httpinvoker.HttpInvokerRequestExecutor} implementation that uses
 * <a href="http://hc.apache.org/httpcomponents-client-ga/httpclient/">Apache HttpComponents HttpClient</a>
 * to execute POST requests.
 *
 * <p>Allows to use a pre-configured {@link org.apache.http.client.HttpClient}
 * instance, potentially with authentication, HTTP connection pooling, etc.
 * Also designed for easy subclassing, providing specific template methods.
 *
 * <p>As of Spring 4.1, this request executor requires Apache HttpComponents 4.3 or higher.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1
 * @see org.springframework.remoting.httpinvoker.SimpleHttpInvokerRequestExecutor
 */
public class HttpComponentsHttpInvokerRequestExecutor extends AbstractHttpInvokerRequestExecutor {

	private static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 100;

	private static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 5;

	private static final int DEFAULT_READ_TIMEOUT_MILLISECONDS = (60 * 1000);


	private HttpClient httpClient;

	@Nullable
	private RequestConfig requestConfig;


	/**
	 * Create a new instance of the HttpComponentsHttpInvokerRequestExecutor with a default
	 * {@link HttpClient} that uses a default {@code org.apache.http.impl.conn.PoolingClientConnectionManager}.
	 */
	public HttpComponentsHttpInvokerRequestExecutor() {
		this(createDefaultHttpClient(), RequestConfig.custom()
				.setSocketTimeout(DEFAULT_READ_TIMEOUT_MILLISECONDS).build());
	}

	/**
	 * Create a new instance of the HttpComponentsClientHttpRequestFactory
	 * with the given {@link HttpClient} instance.
	 * @param httpClient the HttpClient instance to use for this request executor
	 */
	public HttpComponentsHttpInvokerRequestExecutor(HttpClient httpClient) {
		this(httpClient, null);
	}

	private HttpComponentsHttpInvokerRequestExecutor(HttpClient httpClient, @Nullable RequestConfig requestConfig) {
		this.httpClient = httpClient;
		this.requestConfig = requestConfig;
	}


	private static HttpClient createDefaultHttpClient() {
		Registry<ConnectionSocketFactory> schemeRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.getSocketFactory())
				.register("https", SSLConnectionSocketFactory.getSocketFactory())
				.build();

		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(schemeRegistry);
		connectionManager.setMaxTotal(DEFAULT_MAX_TOTAL_CONNECTIONS);
		connectionManager.setDefaultMaxPerRoute(DEFAULT_MAX_CONNECTIONS_PER_ROUTE);

		return HttpClientBuilder.create().setConnectionManager(connectionManager).build();
	}


	/**
	 * Set the {@link HttpClient} instance to use for this request executor.
	 */
	public void setHttpClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	/**
	 * Return the {@link HttpClient} instance that this request executor uses.
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
		this.requestConfig = cloneRequestConfig().setConnectTimeout(timeout).build();
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
		this.requestConfig = cloneRequestConfig().setConnectionRequestTimeout(connectionRequestTimeout).build();
	}

	/**
	 * Set the socket read timeout for the underlying HttpClient.
	 * A timeout value of 0 specifies an infinite timeout.
	 * <p>Additional properties can be configured by specifying a
	 * {@link RequestConfig} instance on a custom {@link HttpClient}.
	 * @param timeout the timeout value in milliseconds
	 * @see #DEFAULT_READ_TIMEOUT_MILLISECONDS
	 * @see RequestConfig#getSocketTimeout()
	 */
	public void setReadTimeout(int timeout) {
		Assert.isTrue(timeout >= 0, "Timeout must be a non-negative value");
		this.requestConfig = cloneRequestConfig().setSocketTimeout(timeout).build();
	}

	private RequestConfig.Builder cloneRequestConfig() {
		return (this.requestConfig != null ? RequestConfig.copy(this.requestConfig) : RequestConfig.custom());
	}


	/**
	 * Execute the given request through the HttpClient.
	 * <p>This method implements the basic processing workflow:
	 * The actual work happens in this class's template methods.
	 * @see #createHttpPost
	 * @see #setRequestBody
	 * @see #executeHttpPost
	 * @see #validateResponse
	 * @see #getResponseBody
	 */
	@Override
	protected RemoteInvocationResult doExecuteRequest(
			HttpInvokerClientConfiguration config, ByteArrayOutputStream baos)
			throws IOException, ClassNotFoundException {

		HttpPost postMethod = createHttpPost(config);
		setRequestBody(config, postMethod, baos);
		try {
			HttpResponse response = executeHttpPost(config, getHttpClient(), postMethod);
			validateResponse(config, response);
			InputStream responseBody = getResponseBody(config, response);
			return readRemoteInvocationResult(responseBody, config.getCodebaseUrl());
		}
		finally {
			postMethod.releaseConnection();
		}
	}

	/**
	 * Create a HttpPost for the given configuration.
	 * <p>The default implementation creates a standard HttpPost with
	 * "application/x-java-serialized-object" as "Content-Type" header.
	 * @param config the HTTP invoker configuration that specifies the
	 * target service
	 * @return the HttpPost instance
	 * @throws java.io.IOException if thrown by I/O methods
	 */
	protected HttpPost createHttpPost(HttpInvokerClientConfiguration config) throws IOException {
		HttpPost httpPost = new HttpPost(config.getServiceUrl());

		RequestConfig requestConfig = createRequestConfig(config);
		if (requestConfig != null) {
			httpPost.setConfig(requestConfig);
		}

		LocaleContext localeContext = LocaleContextHolder.getLocaleContext();
		if (localeContext != null) {
			Locale locale = localeContext.getLocale();
			if (locale != null) {
				httpPost.addHeader(HTTP_HEADER_ACCEPT_LANGUAGE, locale.toLanguageTag());
			}
		}

		if (isAcceptGzipEncoding()) {
			httpPost.addHeader(HTTP_HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
		}

		return httpPost;
	}

	/**
	 * Create a {@link RequestConfig} for the given configuration. Can return {@code null}
	 * to indicate that no custom request config should be set and the defaults of the
	 * {@link HttpClient} should be used.
	 * <p>The default implementation tries to merge the defaults of the client with the
	 * local customizations of the instance, if any.
	 * @param config the HTTP invoker configuration that specifies the
	 * target service
	 * @return the RequestConfig to use
	 */
	@Nullable
	protected RequestConfig createRequestConfig(HttpInvokerClientConfiguration config) {
		HttpClient client = getHttpClient();
		if (client instanceof Configurable) {
			RequestConfig clientRequestConfig = ((Configurable) client).getConfig();
			return mergeRequestConfig(clientRequestConfig);
		}
		return this.requestConfig;
	}

	private RequestConfig mergeRequestConfig(RequestConfig defaultRequestConfig) {
		if (this.requestConfig == null) {  // nothing to merge
			return defaultRequestConfig;
		}

		RequestConfig.Builder builder = RequestConfig.copy(defaultRequestConfig);
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
	 * Set the given serialized remote invocation as request body.
	 * <p>The default implementation simply sets the serialized invocation as the
	 * HttpPost's request body. This can be overridden, for example, to write a
	 * specific encoding and to potentially set appropriate HTTP request headers.
	 * @param config the HTTP invoker configuration that specifies the target service
	 * @param httpPost the HttpPost to set the request body on
	 * @param baos the ByteArrayOutputStream that contains the serialized
	 * RemoteInvocation object
	 * @throws java.io.IOException if thrown by I/O methods
	 */
	protected void setRequestBody(
			HttpInvokerClientConfiguration config, HttpPost httpPost, ByteArrayOutputStream baos)
			throws IOException {

		ByteArrayEntity entity = new ByteArrayEntity(baos.toByteArray());
		entity.setContentType(getContentType());
		httpPost.setEntity(entity);
	}

	/**
	 * Execute the given HttpPost instance.
	 * @param config the HTTP invoker configuration that specifies the target service
	 * @param httpClient the HttpClient to execute on
	 * @param httpPost the HttpPost to execute
	 * @return the resulting HttpResponse
	 * @throws java.io.IOException if thrown by I/O methods
	 */
	protected HttpResponse executeHttpPost(
			HttpInvokerClientConfiguration config, HttpClient httpClient, HttpPost httpPost)
			throws IOException {

		return httpClient.execute(httpPost);
	}

	/**
	 * Validate the given response as contained in the HttpPost object,
	 * throwing an exception if it does not correspond to a successful HTTP response.
	 * <p>Default implementation rejects any HTTP status code beyond 2xx, to avoid
	 * parsing the response body and trying to deserialize from a corrupted stream.
	 * @param config the HTTP invoker configuration that specifies the target service
	 * @param response the resulting HttpResponse to validate
	 * @throws java.io.IOException if validation failed
	 */
	protected void validateResponse(HttpInvokerClientConfiguration config, HttpResponse response)
			throws IOException {

		StatusLine status = response.getStatusLine();
		if (status.getStatusCode() >= 300) {
			throw new NoHttpResponseException(
					"Did not receive successful HTTP response: status code = " + status.getStatusCode() +
					", status message = [" + status.getReasonPhrase() + "]");
		}
	}

	/**
	 * Extract the response body from the given executed remote invocation request.
	 * <p>The default implementation simply fetches the HttpPost's response body stream.
	 * If the response is recognized as GZIP response, the InputStream will get wrapped
	 * in a GZIPInputStream.
	 * @param config the HTTP invoker configuration that specifies the target service
	 * @param httpResponse the resulting HttpResponse to read the response body from
	 * @return an InputStream for the response body
	 * @throws java.io.IOException if thrown by I/O methods
	 * @see #isGzipResponse
	 * @see java.util.zip.GZIPInputStream
	 */
	protected InputStream getResponseBody(HttpInvokerClientConfiguration config, HttpResponse httpResponse)
			throws IOException {

		if (isGzipResponse(httpResponse)) {
			return new GZIPInputStream(httpResponse.getEntity().getContent());
		}
		else {
			return httpResponse.getEntity().getContent();
		}
	}

	/**
	 * Determine whether the given response indicates a GZIP response.
	 * <p>The default implementation checks whether the HTTP "Content-Encoding"
	 * header contains "gzip" (in any casing).
	 * @param httpResponse the resulting HttpResponse to check
	 * @return whether the given response indicates a GZIP response
	 */
	protected boolean isGzipResponse(HttpResponse httpResponse) {
		Header encodingHeader = httpResponse.getFirstHeader(HTTP_HEADER_CONTENT_ENCODING);
		return (encodingHeader != null && encodingHeader.getValue() != null &&
				encodingHeader.getValue().toLowerCase().contains(ENCODING_GZIP));
	}

}
