/*
 * Copyright 2002-2016 the original author or authors.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
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
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

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

	private RequestConfig requestConfig;

	private boolean bufferRequestBody = true;


	/**
	 * Create a new instance of the {@code HttpComponentsClientHttpRequestFactory}
	 * with a default {@link HttpClient}.
	 */
	public HttpComponentsClientHttpRequestFactory() {
		this(HttpClients.createSystem());
	}

	/**
	 * Create a new instance of the {@code HttpComponentsClientHttpRequestFactory}
	 * with the given {@link HttpClient} instance.
	 * @param httpClient the HttpClient instance to use for this request factory
	 */
	public HttpComponentsClientHttpRequestFactory(HttpClient httpClient) {
		Assert.notNull(httpClient, "HttpClient must not be null");
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
	 * <p>Additional properties can be configured by specifying a
	 * {@link RequestConfig} instance on a custom {@link HttpClient}.
	 * @param timeout the timeout value in milliseconds
	 * @see RequestConfig#getConnectTimeout()
	 */
	public void setConnectTimeout(int timeout) {
		Assert.isTrue(timeout >= 0, "Timeout must be a non-negative value");
		this.requestConfig = requestConfigBuilder().setConnectTimeout(timeout).build();
		setLegacyConnectionTimeout(getHttpClient(), timeout);
	}

	/**
	 * Apply the specified connection timeout to deprecated {@link HttpClient}
	 * implementations.
	 * <p>As of HttpClient 4.3, default parameters have to be exposed through a
	 * {@link RequestConfig} instance instead of setting the parameters on the
	 * client. Unfortunately, this behavior is not backward-compatible and older
	 * {@link HttpClient} implementations will ignore the {@link RequestConfig}
	 * object set in the context.
	 * <p>If the specified client is an older implementation, we set the custom
	 * connection timeout through the deprecated API. Otherwise, we just return
	 * as it is set through {@link RequestConfig} with newer clients.
	 * @param client the client to configure
	 * @param timeout the custom connection timeout
	 */
	@SuppressWarnings("deprecation")
	private void setLegacyConnectionTimeout(HttpClient client, int timeout) {
		if (org.apache.http.impl.client.AbstractHttpClient.class.isInstance(client)) {
			client.getParams().setIntParameter(org.apache.http.params.CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);
		}
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
		setLegacySocketTimeout(getHttpClient(), timeout);
	}

	/**
	 * Apply the specified socket timeout to deprecated {@link HttpClient}
	 * implementations. See {@link #setLegacyConnectionTimeout}.
	 * @param client the client to configure
	 * @param timeout the custom socket timeout
	 * @see #setLegacyConnectionTimeout
	 */
	@SuppressWarnings("deprecation")
	private void setLegacySocketTimeout(HttpClient client, int timeout) {
		if (org.apache.http.impl.client.AbstractHttpClient.class.isInstance(client)) {
			client.getParams().setIntParameter(org.apache.http.params.CoreConnectionPNames.SO_TIMEOUT, timeout);
		}
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
		Assert.state(client != null, "Synchronous execution requires an HttpClient to be set");

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
	 * (may be {@code null} if the given client config is {@code null})
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
	 * Add the given headers to the given HTTP request.
	 * @param httpRequest the request to add the headers to
	 * @param headers the headers to add
	 */
	static void addHeaders(HttpUriRequest httpRequest, HttpHeaders headers) {
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			String headerName = entry.getKey();
			if (HttpHeaders.COOKIE.equalsIgnoreCase(headerName)) {  // RFC 6265
				String headerValue = StringUtils
						.collectionToDelimitedString(entry.getValue(), "; ");
				httpRequest.addHeader(headerName, headerValue);
			}
			else if (!HTTP.CONTENT_LEN.equalsIgnoreCase(headerName) &&
					!HTTP.TRANSFER_ENCODING.equalsIgnoreCase(headerName)) {
				for (String headerValue : entry.getValue()) {
					httpRequest.addHeader(headerName, headerValue);
				}
			}
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
	 * {@link org.apache.http.conn.HttpClientConnectionManager ClientConnectionManager}'s
	 * connection pool, if any.
	 */
	@Override
	public void destroy() throws Exception {
		if (this.httpClient instanceof Closeable) {
			((Closeable) this.httpClient).close();
		}
	}


	/**
	 * An alternative to {@link org.apache.http.client.methods.HttpDelete} that
	 * extends {@link org.apache.http.client.methods.HttpEntityEnclosingRequestBase}
	 * rather than {@link org.apache.http.client.methods.HttpRequestBase} and
	 * hence allows HTTP delete with a request body. For use with the RestTemplate
	 * exchange methods which allow the combination of HTTP DELETE with entity.
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

	static class HttpComponentsClientHttpRequest extends AbstractBufferingClientHttpRequest {

		final HttpClient httpClient;

		final HttpUriRequest httpRequest;

		final HttpContext httpContext;


		HttpComponentsClientHttpRequest(HttpClient httpClient, HttpUriRequest httpRequest, HttpContext httpContext) {
			this.httpClient = httpClient;
			this.httpRequest = httpRequest;
			this.httpContext = httpContext;
		}


		@Override
		public HttpMethod getMethod() {
			return HttpMethod.resolve(this.httpRequest.getMethod());
		}

		@Override
		public URI getURI() {
			return this.httpRequest.getURI();
		}


		@Override
		protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
			addHeaders(this.httpRequest, headers);

			if (this.httpRequest instanceof HttpEntityEnclosingRequest) {
				HttpEntityEnclosingRequest entityEnclosingRequest = (HttpEntityEnclosingRequest) this.httpRequest;
				HttpEntity requestEntity = new ByteArrayEntity(bufferedOutput);
				entityEnclosingRequest.setEntity(requestEntity);
			}
			HttpResponse httpResponse = this.httpClient.execute(this.httpRequest, this.httpContext);
			return new HttpComponentsClientHttpResponse(httpResponse);
		}


	}

	private static final class HttpComponentsStreamingClientHttpRequest extends HttpComponentsClientHttpRequest implements
			StreamingHttpOutputMessage {

		private Body body;


		HttpComponentsStreamingClientHttpRequest(HttpClient httpClient, HttpUriRequest httpRequest, HttpContext httpContext) {
			super(httpClient, httpRequest, httpContext);
		}

		@Override
		public void setBody(Body body) {
			assertNotExecuted();
			this.body = body;
		}

		@Override
		protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
			throw new UnsupportedOperationException("getBody not supported");
		}

		@Override
		protected ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
			addHeaders(this.httpRequest, headers);

			if (this.httpRequest instanceof HttpEntityEnclosingRequest && body != null) {
				HttpEntityEnclosingRequest entityEnclosingRequest = (HttpEntityEnclosingRequest) this.httpRequest;
				HttpEntity requestEntity = new StreamingHttpEntity(getHeaders(), body);
				entityEnclosingRequest.setEntity(requestEntity);
			}

			HttpResponse httpResponse = this.httpClient.execute(this.httpRequest, this.httpContext);
			return new HttpComponentsClientHttpResponse(httpResponse);
		}


		private static class StreamingHttpEntity implements HttpEntity {

			private final HttpHeaders headers;

			private final Body body;

			public StreamingHttpEntity(HttpHeaders headers, Body body) {
				this.headers = headers;
				this.body = body;
			}

			@Override
			public boolean isRepeatable() {
				return false;
			}

			@Override
			public boolean isChunked() {
				return false;
			}

			@Override
			public long getContentLength() {
				return this.headers.getContentLength();
			}

			@Override
			public Header getContentType() {
				MediaType contentType = this.headers.getContentType();
				return (contentType != null ? new BasicHeader("Content-Type", contentType.toString()) : null);
			}

			@Override
			public Header getContentEncoding() {
				String contentEncoding = this.headers.getFirst("Content-Encoding");
				return (contentEncoding != null ? new BasicHeader("Content-Encoding", contentEncoding) : null);

			}

			@Override
			public InputStream getContent() throws IOException, IllegalStateException {
				throw new IllegalStateException("No content available");
			}

			@Override
			public void writeTo(OutputStream outputStream) throws IOException {
				this.body.writeTo(outputStream);
			}

			@Override
			public boolean isStreaming() {
				return true;
			}

			@Override
			@Deprecated
			public void consumeContent() throws IOException {
				throw new UnsupportedOperationException();
			}
		}

	}

	static class HttpComponentsClientHttpResponse extends AbstractClientHttpResponse {

		private final HttpResponse httpResponse;

		private HttpHeaders headers;


		HttpComponentsClientHttpResponse(HttpResponse httpResponse) {
			this.httpResponse = httpResponse;
		}


		@Override
		public int getRawStatusCode() throws IOException {
			return this.httpResponse.getStatusLine().getStatusCode();
		}

		@Override
		public String getStatusText() throws IOException {
			return this.httpResponse.getStatusLine().getReasonPhrase();
		}

		@Override
		public HttpHeaders getHeaders() {
			if (this.headers == null) {
				this.headers = new HttpHeaders();
				for (Header header : this.httpResponse.getAllHeaders()) {
					this.headers.add(header.getName(), header.getValue());
				}
			}
			return this.headers;
		}

		@Override
		public InputStream getBody() throws IOException {
			HttpEntity entity = this.httpResponse.getEntity();
			return (entity != null ? entity.getContent() : StreamUtils.emptyInput());
		}

		@Override
		public void close() {
	        // Release underlying connection back to the connection manager
	        try {
	            try {
	                // Attempt to keep connection alive by consuming its remaining content
	                EntityUtils.consume(this.httpResponse.getEntity());
	            }
				finally {
					if (this.httpResponse instanceof Closeable) {
						((Closeable) this.httpResponse).close();
					}
	            }
	        }
	        catch (IOException ex) {
				// Ignore exception on close...
	        }
		}

	}
}
